package com.hexaghost.flixora.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexaghost.flixora.data.api.User
import com.hexaghost.flixora.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class AuthUiEvent {
    object SignUpSuccess : AuthUiEvent()
    object SignInSuccess : AuthUiEvent()
    class Error(val message: String) : AuthUiEvent()
}

data class AuthUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AuthUiEvent>()
    val eventFlow: SharedFlow<AuthUiEvent> = _eventFlow.asSharedFlow()

    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // Automatically check if there is an active session
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.checkSession()
                .onSuccess { user ->
                    _uiState.update { it.copy(user = user, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(user = null, isLoading = false) }
                }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signUp(email, password, name)
                .onSuccess { user ->
                    _uiState.update { it.copy(user = user, isLoading = false) }
                    _eventFlow.emit(AuthUiEvent.SignUpSuccess)
                }
                .onFailure { e ->
                    val errorMsg = e.message ?: "Sign Up failed"
                    _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                    _eventFlow.emit(AuthUiEvent.Error(errorMsg))
                }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signIn(email, password)
                .onSuccess { user ->
                    _uiState.update { it.copy(user = user, isLoading = false) }
                    _eventFlow.emit(AuthUiEvent.SignInSuccess)
                }
                .onFailure { e ->
                    val errorMsg = e.message ?: "Sign In failed"
                    _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                    _eventFlow.emit(AuthUiEvent.Error(errorMsg))
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signOut()
                .onSuccess {
                    _uiState.update { it.copy(user = null, isLoading = false) }
                }
                .onFailure { e ->
                    val errorMsg = e.message ?: "Sign Out failed"
                    _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                    _eventFlow.emit(AuthUiEvent.Error(errorMsg))
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
