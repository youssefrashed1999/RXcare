package com.example.rxcare.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rxcare.domain.model.User
import com.example.rxcare.domain.model.UserRole
import com.example.rxcare.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Expose authentication state as a Flow<Boolean>
    fun isLoggedIn() = currentUser.map { user -> user != null }

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _currentUser.value = user
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun signUp(name: String, email: String, password: String, role: UserRole) {
        if (validateInput(name, email, password)) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            viewModelScope.launch {
                val result = authRepository.register(email, password, name)
                result.fold(
                    onSuccess = { user ->
                        _currentUser.value = user
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            signUpSuccess = true
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                )
            }
        }
    }

    fun createPharmacist(name: String, email: String, password: String) {
        if (validateInput(name, email, password)) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            viewModelScope.launch {
                val result = authRepository.registerPharmacist(email, password, name)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            createPharmacistSuccess = true
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                )
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (validateEmail(email) && password.isNotBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            viewModelScope.launch {
                val result = authRepository.login(email, password)
                result.fold(
                    onSuccess = { user ->
                        _currentUser.value = user
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            signInSuccess = true
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.logout()
            _currentUser.value = null
            _uiState.value = AuthUiState()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessStates() {
        _uiState.value = _uiState.value.copy(
            signUpSuccess = false,
            signInSuccess = false,
            createPharmacistSuccess = false
        )
    }

    private fun validateInput(name: String, email: String, password: String): Boolean {
        return when {
            name.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Name cannot be empty")
                false
            }
            !validateEmail(email) -> {
                _uiState.value = _uiState.value.copy(error = "Invalid email format")
                false
            }
            password.length < 6 -> {
                _uiState.value = _uiState.value.copy(error = "Password must be at least 6 characters")
                false
            }
            else -> true
        }
    }

    private fun validateEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

data class AuthUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val signUpSuccess: Boolean = false,
    val signInSuccess: Boolean = false,
    val createPharmacistSuccess: Boolean = false
)
