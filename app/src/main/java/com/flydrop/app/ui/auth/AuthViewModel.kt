package com.flydrop.app.ui.auth

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flydrop.app.data.auth.AuthRepository
import com.flydrop.app.data.auth.AuthUnavailableReason
import com.flydrop.app.data.auth.SignInResult
import com.flydrop.app.data.model.FlyUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the app is in the sign-in lifecycle. */
@Immutable
sealed interface AuthStatus {
    /** Before the first Firebase callback arrives; the app shows a splash. */
    data object Resolving : AuthStatus

    data object SignedOut : AuthStatus

    data class SignedIn(val user: FlyUser) : AuthStatus
}

@Immutable
data class AuthUiState(
    val status: AuthStatus = AuthStatus.Resolving,
    val signingIn: Boolean = false,
    val errorMessage: String? = null,
    val unavailableReason: AuthUnavailableReason? = null,
    /**
     * Lets the three reference screens be opened without Firebase, so the UI
     * stays reviewable on a machine with no Firebase project. Only offered
     * while sign-in is genuinely unavailable.
     */
    val bypassedSetup: Boolean = false,
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application)

    private val _uiState = MutableStateFlow(
        AuthUiState(unavailableReason = repository.unavailableReason),
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        if (repository.unavailableReason != null) {
            // No Firebase to wait on, so resolve immediately.
            _uiState.update { it.copy(status = AuthStatus.SignedOut) }
        } else {
            viewModelScope.launch {
                repository.authState().collect { user ->
                    _uiState.update { state ->
                        state.copy(
                            status = if (user != null) {
                                AuthStatus.SignedIn(user)
                            } else {
                                AuthStatus.SignedOut
                            },
                            signingIn = false,
                        )
                    }
                }
            }
        }
    }

    fun signInWithGoogle(activity: Activity) {
        if (_uiState.value.signingIn) return
        _uiState.update { it.copy(signingIn = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.signInWithGoogle(activity)) {
                is SignInResult.Success ->
                    // The auth-state listener drives the status change; this
                    // just clears the spinner if the listener is slow.
                    _uiState.update { it.copy(signingIn = false) }

                SignInResult.Cancelled ->
                    _uiState.update { it.copy(signingIn = false) }

                is SignInResult.Failure ->
                    _uiState.update {
                        it.copy(signingIn = false, errorMessage = result.message)
                    }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _uiState.update {
                it.copy(status = AuthStatus.SignedOut, bypassedSetup = false, errorMessage = null)
            }
        }
    }

    /** Opens the app without an account while Firebase is not yet configured. */
    fun continueWithoutFirebase() {
        _uiState.update { it.copy(bypassedSetup = true, errorMessage = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
