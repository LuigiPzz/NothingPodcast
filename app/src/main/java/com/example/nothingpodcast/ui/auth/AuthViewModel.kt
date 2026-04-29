package com.example.nothingpodcast.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nothingpodcast.data.auth.AuthRepository
import com.example.nothingpodcast.data.auth.AuthState
import com.example.nothingpodcast.data.local.datastore.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading:      Boolean = false,
    val errorMessage:   String? = null,
    val isRegisterMode: Boolean = false,
    /** When true, show the "new password" field for local reset */
    val isResetMode:    Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    /** Global auth state consumed by MainActivity */
    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    val hasSeenOnboarding: StateFlow<Boolean> = preferencesDataStore.hasSeenOnboarding
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true) 

    fun setHasSeenOnboarding(seen: Boolean) {
        viewModelScope.launch { preferencesDataStore.setHasSeenOnboarding(seen) }
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // ── Sign in ────────────────────────────────────────────────────────────

    fun signInWithEmail(email: String, password: String) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.signInWithEmail(email, password)
                .onFailure { e -> _uiState.update { it.copy(errorMessage = friendlyError(e.message)) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ── Create account ─────────────────────────────────────────────────────

    fun createAccount(email: String, password: String) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.createAccount(email, password)
                .onFailure { e -> _uiState.update { it.copy(errorMessage = friendlyError(e.message)) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ── Local password reset (no email sent — sets new pwd directly) ───────

    fun resetPassword(email: String, newPassword: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter your email first") }
            return
        }
        if (newPassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "New password must be at least 6 characters") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.resetPassword(email, newPassword)
                .onSuccess { _uiState.update { it.copy(errorMessage = "Password updated ✓", isResetMode = false) } }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ── UI helpers ─────────────────────────────────────────────────────────

    fun toggleRegisterMode() =
        _uiState.update { it.copy(isRegisterMode = !it.isRegisterMode, isResetMode = false, errorMessage = null) }

    fun toggleResetMode() =
        _uiState.update { it.copy(isResetMode = !it.isResetMode, errorMessage = null) }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
    fun signOut() { viewModelScope.launch { authRepository.signOut() } }

    // ── Validation ─────────────────────────────────────────────────────────

    private fun validate(email: String, password: String): Boolean = when {
        email.isBlank() -> {
            _uiState.update { it.copy(errorMessage = "Email is required") }; false
        }
        !email.contains("@") -> {
            _uiState.update { it.copy(errorMessage = "Enter a valid email address") }; false
        }
        password.length < 6 -> {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }; false
        }
        else -> true
    }

    private fun friendlyError(msg: String?): String = when {
        msg == null                           -> "An error occurred"
        "incorrect password" in msg.lowercase() -> "Incorrect password"
        "no account" in msg.lowercase()       -> "No account found for this email"
        "already registered" in msg.lowercase() -> "This email is already registered"
        else                                  -> msg
    }
}
