// OWNER: A — TODO
package com.example.mos.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mos.data.AuthRepository
import com.example.mos.data.TransportMode
import com.example.mos.data.User
import com.example.mos.screens.auth.toKenyanPhone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// The profile only offers these three. Explore still has all four modes.
val profileModes = listOf(TransportMode.WALK, TransportMode.BODA, TransportMode.PRIVATE)

// Any other saved mode (e.g. Matatu) shows as Car.
fun TransportMode.forProfile(): TransportMode = if (this in profileModes) this else TransportMode.PRIVATE

// ---------- Profile ----------

data class ProfileUiState(
    val user: User? = null,
    val usesPassword: Boolean = false,
    val loading: Boolean = false,
    val resetEmailSent: Boolean = false,
    val error: String? = null
)

class ProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            authRepository.getProfile()
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            user = user.copy(defaultMode = user.defaultMode.forProfile()),
                            usesPassword = authRepository.usesPassword(),
                            loading = false
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.message) } }
        }
    }

    // On success the screen shows a "Check your email" pop-up.
    fun sendPasswordReset() {
        viewModelScope.launch {
            authRepository.sendPasswordReset()
                .onSuccess { _uiState.update { it.copy(resetEmailSent = true, error = null) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun dismissResetDialog() = _uiState.update { it.copy(resetEmailSent = false) }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            authRepository.deleteAccount()
                .onSuccess { onDeleted() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }
}

// ---------- Edit profile ----------

data class EditProfileUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",          // digits after +254, e.g. "712345678"
    val defaultMode: TransportMode = TransportMode.PRIVATE,
    val saving: Boolean = false,
    val error: String? = null
)

class EditProfileViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    // Fills the form with the saved details.
    fun load() {
        viewModelScope.launch {
            authRepository.getProfile().onSuccess { user ->
                _uiState.update {
                    it.copy(
                        name = user.name,
                        email = user.email,
                        phone = user.phone.removePrefix("+254"),
                        defaultMode = user.defaultMode.forProfile()
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }
    fun onPhoneChange(phone: String) = _uiState.update { it.copy(phone = phone) }
    fun onModeChange(mode: TransportMode) = _uiState.update { it.copy(defaultMode = mode) }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val phone = toKenyanPhone(state.phone)
        val error = when {
            state.name.isBlank() -> "Please enter your name."
            phone == null -> "Enter a valid phone number, e.g. 712 345 678."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }

        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            authRepository.updateProfile(state.name.trim(), phone!!, state.defaultMode)
                .onSuccess {
                    _uiState.update { it.copy(saving = false) }
                    onSaved()
                }
                .onFailure { e -> _uiState.update { it.copy(saving = false, error = e.message) } }
        }
    }
}
