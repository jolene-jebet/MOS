// OWNER: A — TODO
package com.example.mos.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mos.data.AuthRepository
import com.example.mos.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        val error = when {
            email.isBlank() || password.isBlank() -> "Enter your email and password."
            !isValidEmail(email) -> "Enter a valid email address."
            else -> null
        }
        if (error != null) return showError(error)

        run(onSuccess) { authRepository.login(email.trim(), password) }
    }

    fun signUp(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        val error = when {
            name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank() ->
                "Please fill in all the fields."
            !isValidEmail(email) -> "Enter a valid email address."
            toKenyanPhone(phone) == null -> "Enter a valid phone number, e.g. 712 345 678."
            password.length < 6 -> "Password must be at least 6 characters."
            password != confirmPassword -> "Passwords do not match."
            else -> null
        }
        if (error != null) return showError(error)

        run(onSuccess) {
            authRepository.signUp(name.trim(), email.trim(), toKenyanPhone(phone)!!, password)
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        run(onSuccess) { authRepository.signInWithGoogle(idToken) }
    }

    fun showError(message: String) {
        _uiState.value = AuthUiState(error = message)
    }

    // Shows a spinner, calls the repository, then either navigates on or shows the error.
    private fun run(onSuccess: () -> Unit, call: suspend () -> Result<User>) {
        if (_uiState.value.loading) return
        _uiState.value = AuthUiState(loading = true)

        viewModelScope.launch {
            call()
                .onSuccess {
                    _uiState.value = AuthUiState()
                    onSuccess()
                }
                .onFailure { showError(it.message ?: "Something went wrong.") }
        }
    }
}

private fun isValidEmail(email: String): Boolean =
    Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())

// Accepts "712345678", "0712345678" or "+254712345678" and returns "+254712345678".
internal fun toKenyanPhone(input: String): String? {
    val digits = input.filter { it.isDigit() }.removePrefix("254").removePrefix("0")
    return if (digits.length == 9) "+254$digits" else null
}
