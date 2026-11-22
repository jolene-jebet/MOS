// OWNER: A — TODO
package com.example.mos.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
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

    // Profile
    suspend fun getProfile(): Result<User>
    suspend fun updateProfile(name: String, phone: String, defaultMode: TransportMode): Result<User>
    suspend fun sendPasswordReset(): Result<Unit>
    fun usesPassword(): Boolean
    suspend fun deleteAccount(): Result<Unit>
}

// Always succeeds, so the app works without a backend. Keeps one user in memory.
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

    override suspend fun getProfile(): Result<User> =
        user?.let { Result.success(it) } ?: Result.failure(Exception("Not logged in."))

    override suspend fun updateProfile(name: String, phone: String, defaultMode: TransportMode): Result<User> {
        val current = user ?: return Result.failure(Exception("Not logged in."))
        val updated = current.copy(name = name, phone = phone, defaultMode = defaultMode)
        user = updated
        return Result.success(updated)
    }

    override suspend fun sendPasswordReset(): Result<Unit> = Result.success(Unit)

    override fun usesPassword(): Boolean = true

    override suspend fun deleteAccount(): Result<Unit> {
        user = null
        return Result.success(Unit)
    }
}

// Real sign-in with Firebase Auth. Name, phone and default mode are saved in Firestore at users/{uid}.
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

    // ---------- Profile ----------

    // Reads users/{uid}. Anything missing falls back to the Firebase account.
    override suspend fun getProfile() = safeCall {
        val firebaseUser = requireUser()
        val doc = profileDoc(firebaseUser.uid).get().await()

        User(
            id = firebaseUser.uid,
            name = doc.getString("name") ?: firebaseUser.displayName.orEmpty(),
            email = doc.getString("email") ?: firebaseUser.email.orEmpty(),
            phone = doc.getString("phone").orEmpty(),
            defaultMode = doc.getString("defaultMode").toTransportMode()
        )
    }

    override suspend fun updateProfile(name: String, phone: String, defaultMode: TransportMode) = safeCall {
        val firebaseUser = requireUser()
        firebaseUser.updateProfile(userProfileChangeRequest { displayName = name }).await()

        val changes = mapOf("name" to name, "phone" to phone, "defaultMode" to defaultMode.name)
        profileDoc(firebaseUser.uid).set(changes, SetOptions.merge()).await()

        User(
            id = firebaseUser.uid,
            name = name,
            email = firebaseUser.email.orEmpty(),
            phone = phone,
            defaultMode = defaultMode
        )
    }

    override suspend fun sendPasswordReset() = safeCall {
        val email = requireUser().email ?: error("This account has no email.")
        auth.sendPasswordResetEmail(email).await()
        Unit
    }

    // True for email/password accounts, false for Google-only accounts.
    override fun usesPassword(): Boolean =
        auth.currentUser?.providerData?.any { it.providerId == "password" } == true

    // Deletes the Firestore profile, then the account. If the account can't be
    // deleted (Firebase wants a recent login), the profile is put back.
    override suspend fun deleteAccount() = safeCall {
        val firebaseUser = requireUser()
        val doc = profileDoc(firebaseUser.uid)
        val backup = doc.get().await().data

        doc.delete().await()
        try {
            firebaseUser.delete().await()
        } catch (e: Exception) {
            if (backup != null) doc.set(backup).await()
            throw e
        }
        Unit
    }

    // ---------- Helpers ----------

    private fun requireUser(): FirebaseUser = auth.currentUser ?: error("Not logged in.")

    private fun profileDoc(uid: String) = db.collection("users").document(uid)

    // Merge so an existing phone number is not wiped (e.g. on a later Google sign-in).
    private suspend fun saveProfile(user: User) {
        val profile = mutableMapOf("name" to user.name, "email" to user.email)
        if (user.phone.isNotBlank()) profile["phone"] = user.phone
        profileDoc(user.id).set(profile, SetOptions.merge()).await()
    }

    private fun FirebaseUser.toUser() = User(
        id = uid,
        name = displayName.orEmpty(),
        email = email.orEmpty()
    )

    private fun String?.toTransportMode(): TransportMode =
        TransportMode.entries.firstOrNull { it.name == this } ?: TransportMode.PRIVATE

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
        is FirebaseAuthRecentLoginRequiredException ->
            "For your security, log out and log in again, then try again."
        is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Wrong email or password."
        else -> "Something went wrong. Check your connection and try again."
    }
}
