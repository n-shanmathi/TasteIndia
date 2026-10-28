package com.example.tasteindia.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tasteindia.data.auth.AuthRepository
import com.example.tasteindia.data.auth.toAuthErrorMessage
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val user: FirebaseUser? = null,
    val isInitializing: Boolean = true,
    val isSubmitting: Boolean = false,
    val message: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState(user = repository.currentUser))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private var removeAuthListener: (() -> Unit)? = null

    init {
        removeAuthListener = repository.observeAuthState { user ->
            _uiState.value = _uiState.value.copy(
                user = user,
                isInitializing = false,
                isSubmitting = false
            )
        }
    }

    fun signIn(email: String, password: String) = runAuthRequest {
        repository.signIn(email.trim(), password)
    }

    fun signUp(name: String, email: String, password: String) = runAuthRequest {
        repository.signUp(name.trim(), email.trim(), password)
    }

    fun signInWithGoogle(idToken: String) = runAuthRequest {
        repository.signInWithGoogle(idToken)
    }

    fun sendPasswordReset(email: String) = runAuthRequest {
        repository.sendPasswordReset(email.trim())
    }

    fun resendVerificationEmail() = runAuthRequest {
        repository.resendVerificationEmail()
    }

    fun reloadUser() = runAuthRequest {
        repository.reloadUser()
    }

    fun signOut() {
        repository.signOut()
    }

    fun googleSignInOptions(androidContext: android.content.Context) =
        repository.googleSignInOptions(androidContext)

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, isSuccess = false)
    }

    fun setValidationMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message, isSuccess = false)
    }

    private fun runAuthRequest(request: suspend () -> Unit) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSubmitting = true,
                message = null,
                isSuccess = false
            )
            try {
                request()
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    message = "Success",
                    isSuccess = true
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    message = error.toAuthErrorMessage(),
                    isSuccess = false
                )
            }
        }
    }

    override fun onCleared() {
        removeAuthListener?.invoke()
        removeAuthListener = null
        super.onCleared()
    }

    companion object {
        fun provideFactory(repository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                        return AuthViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
