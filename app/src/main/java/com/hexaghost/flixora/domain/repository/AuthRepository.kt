package com.hexaghost.flixora.domain.repository

import com.hexaghost.flixora.data.api.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    val isLoggedIn: StateFlow<Boolean>

    suspend fun signUp(email: String, password: String, name: String): Result<User>
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun checkSession(): Result<User>
}
