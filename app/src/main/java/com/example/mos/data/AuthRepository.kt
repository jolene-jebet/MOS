// OWNER: A — TODO
package com.example.mos.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

interface AuthRepository {
    suspend fun signUp(name: String, email: String, phone: String, password: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun logout()
    fun currentUser(): User?
}

// Always succeeds, so login/signup navigation works without a backend.
class FakeAuthRepository : AuthRepository {
    private var user: User? = null

    override suspend fun signUp(name: String, email: String, phone: String, password: String): Result<User> {
        val newUser = User(id = "fake", name = name, email = email, phone = phone)
        user = newUser
        return Result.success(newUser)
    }

    override suspend fun login(email: String, password: String): Result<User> {
        val loggedIn = User(id = "fake", email = email)
        user = loggedIn
        return Result.success(loggedIn)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        val loggedIn = User(id = "fake", email = "google@fake.com")
        user = loggedIn
        return Result.success(loggedIn)
    }

    override suspend fun logout() {
        user = null
    }

    override fun currentUser(): User? = user
}

// Real sign-in with Firebase Auth. Name and phone are saved in Firestore at users/{uid}.
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override suspend fun signUp(name: String, email: String, phone: String, password: String) = safeCall {
        val firebaseUser = auth.createUserWithEmailAndPassword(email, password).await().user!!
        firebaseUser.updateProfile(userProfileChangeRequest { displayName = name }).await()

        val user = User(id = firebaseUser.uid, name = name, email = email, phone = phone)
        saveProfile(user)
        user
    }

    override suspend fun login(email: String, password: String) = safeCall {
        auth.signInWithEmailAndPassword(email, password).await().user!!.toUser()
    }

    override suspend fun signInWithGoogle(idToken: String) = safeCall {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.signInWithCredential(credential).await().user!!.toUser()
        saveProfile(user)
        user
    }

    override suspend fun logout() {
        auth.signOut()
    }

    override fun currentUser(): User? = auth.currentUser?.toUser()

    // Merge so an existing phone number is not wiped (e.g. on a later Google sign-in).
    private suspend fun saveProfile(user: User) {
        val profile = mutableMapOf("name" to user.name, "email" to user.email)
        if (user.phone.isNotBlank()) profile["phone"] = user.phone
        db.collection("users").document(user.id).set(profile, SetOptions.merge()).await()
    }

    private fun FirebaseUser.toUser() = User(
        id = uid,
        name = displayName.orEmpty(),
        email = email.orEmpty()
    )

    // Runs a Firebase call and turns any error into a short, readable message.
    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(friendlyMessage(e), e))
        }

    private fun friendlyMessage(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Wrong email or password."
        else -> "Something went wrong. Check your connection and try again."
    }
}
