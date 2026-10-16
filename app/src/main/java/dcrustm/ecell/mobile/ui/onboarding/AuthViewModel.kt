package dcrustm.ecell.mobile.ui.onboarding

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.auth.AuthError
import dcrustm.ecell.mobile.domain.auth.AuthSuccess
import dcrustm.ecell.mobile.domain.model.User
import dcrustm.ecell.mobile.domain.usecase.auth.EmailLoginUseCase
import dcrustm.ecell.mobile.domain.usecase.auth.EmailSignUpUseCase
import dcrustm.ecell.mobile.domain.usecase.auth.GetGoogleIdUseCase
import dcrustm.ecell.mobile.domain.usecase.auth.GoogleLoginUseCase
import dcrustm.ecell.mobile.domain.usecase.auth.GoogleSignUpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val getGoogleIdUseCase: GetGoogleIdUseCase,
    private val emailSignUpUseCase: EmailSignUpUseCase,
    private val emailLoginUseCase: EmailLoginUseCase,
    private val googleSignUpUseCase: GoogleSignUpUseCase,
    private val googleLoginUseCase: GoogleLoginUseCase
) : ViewModel() {

    private var _isLoginSuccessful = MutableStateFlow(false)
    val isLoginSuccessful: StateFlow<Boolean> = _isLoginSuccessful

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    // Function to signup with google all in one and return the access token.
    fun signUpWithGoogle(credentialManager: CredentialManager, context: Activity) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        val result = googleSignUpUseCase(credentialManager, context)
        when(result) {
            is AuthSuccess -> {
                _authState.value = AuthState.Authenticated(result)
                _isLoginSuccessful.value = true
            }
            is AuthError -> {
                _authState.value = AuthState.Error(result.errorMessage)
                _isLoginSuccessful.value = false
            }
        }
    }

    // Function to login with google all in one and return the access token.
    fun loginWithGoogle(credentialManager: CredentialManager, context: Activity) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        val result = googleLoginUseCase(credentialManager, context)
        when(result) {
            is AuthSuccess -> {
                _authState.value = AuthState.Authenticated(result)
                _isLoginSuccessful.value = true
            }
            is AuthError -> {
                _authState.value = AuthState.Error(result.errorMessage)
                _isLoginSuccessful.value = false
            }
        }
    }

    // Email/password sign-in is not enabled on the Firebase project (Google-only for now).
    fun signUpWithEmail(user: User) = viewModelScope.launch {
        _authState.value = AuthState.Error("Email sign-up isn't available yet — please continue with Google.")
        _isLoginSuccessful.value = false
    }

    fun loginWithEmail(email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Error("Email sign-in isn't available yet — please continue with Google.")
        _isLoginSuccessful.value = false
    }

    // Function to fetch google details, left for debugging.
    fun getGoogleIdDetails(credentialManager: CredentialManager, context: Activity) = viewModelScope.launch{
        getGoogleIdUseCase(credentialManager = credentialManager,context = context)
    }

}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Authenticated(val authData: AuthSuccess) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}
