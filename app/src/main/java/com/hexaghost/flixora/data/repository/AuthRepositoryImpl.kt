package com.hexaghost.flixora.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.hexaghost.flixora.data.api.User
import com.hexaghost.flixora.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        // Restore session from Firebase persistent storage
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName ?: "User",
                image = firebaseUser.photoUrl?.toString()
            )
            _currentUser.value = user
            _isLoggedIn.value = true
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Sign up failed"))

            // Set display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                name = name,
                image = null
            )
            _currentUser.value = user
            _isLoggedIn.value = true
            Result.success(user)
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("Password is too weak. Use at least 6 characters."))
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("An account with this email already exists."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Invalid email address format."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Sign up failed"))
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Sign in failed"))

            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: email,
                name = firebaseUser.displayName ?: "User",
                image = firebaseUser.photoUrl?.toString()
            )
            _currentUser.value = user
            _isLoggedIn.value = true
            Result.success(user)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Incorrect password. Please try again."))
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("No account found with this email."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Sign in failed"))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            _currentUser.value = null
            _isLoggedIn.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Sign out failed"))
        }
    }

    override suspend fun checkSession(): Result<User> {
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // Force token refresh to validate session is still alive
                firebaseUser.reload().await()
                val user = User(
                    id = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    name = firebaseUser.displayName ?: "User",
                    image = firebaseUser.photoUrl?.toString()
                )
                _currentUser.value = user
                _isLoggedIn.value = true
                Result.success(user)
            } else {
                _currentUser.value = null
                _isLoggedIn.value = false
                Result.failure(Exception("No active session"))
            }
        } catch (e: Exception) {
            _currentUser.value = null
            _isLoggedIn.value = false
            Result.failure(Exception(e.message ?: "Session check failed"))
        }
    }
}
