// OWNER: A — TODO
package com.example.mos.data

interface AuthRepository {
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout()
    fun currentUser(): User?
}

// Always succeeds, so login/signup navigation works without a backend.
class FakeAuthRepository : AuthRepository {
    private var user: User? = null

    override suspend fun signUp(email: String, password: String): Result<User> {
        val newUser = User(id = "fake", email = email)
        user = newUser
        return Result.success(newUser)
    }

    override suspend fun login(email: String, password: String): Result<User> {
        val loggedIn = User(id = "fake", email = email)
        user = loggedIn
        return Result.success(loggedIn)
    }

    override suspend fun logout() {
        user = null
    }

    override fun currentUser(): User? = user
}

// FirebaseAuthRepository goes here later
