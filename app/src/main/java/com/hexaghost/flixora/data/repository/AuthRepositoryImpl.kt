package com.hexaghost.flixora.data.repository

import android.content.Context
import com.hexaghost.flixora.data.api.AuthApiService
import com.hexaghost.flixora.data.api.SignInRequest
import com.hexaghost.flixora.data.api.SignUpRequest
import com.hexaghost.flixora.data.api.User
import com.hexaghost.flixora.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NeonAuthInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {
    private val prefs = context.getSharedPreferences("flixora_auth_prefs", Context.MODE_PRIVATE)

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // Read all stored cookies
        val cookies = prefs.getStringSet("cookies", emptySet()) ?: emptySet()
        for (cookie in cookies) {
            requestBuilder.addHeader("Cookie", cookie)
        }

        val response = chain.proceed(requestBuilder.build())

        // Save Set-Cookie headers
        val headers = response.headers("Set-Cookie")
        if (headers.isNotEmpty()) {
            val newCookies = HashSet(cookies)
            newCookies.addAll(headers)
            prefs.edit().putStringSet("cookies", newCookies).apply()
        }

        return response
    }
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val prefs = context.getSharedPreferences("flixora_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        // Load cached user details if available
        val userId = prefs.getString("user_id", null)
        val userEmail = prefs.getString("user_email", null)
        val userName = prefs.getString("user_name", null)
        val userImage = prefs.getString("user_image", null)

        if (userId != null && userEmail != null && userName != null) {
            val user = User(userId, userEmail, userName, userImage)
            _currentUser.value = user
            _isLoggedIn.value = true
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): Result<User> {
        return try {
            val response = apiService.signUp(SignUpRequest(email, password, name))
            if (response.isSuccessful) {
                // Better Auth sign-up endpoint automatically logs user in and sets session cookie in headers.
                // We'll verify session or construct user from response.
                val user = response.body()?.user ?: User("id_temp_${System.currentTimeMillis()}", email, name)
                saveUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Sign Up failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val response = apiService.signIn(SignInRequest(email, password))
            if (response.isSuccessful) {
                val user = response.body()?.user ?: User("id_temp_${System.currentTimeMillis()}", email, "Guest")
                saveUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Sign In failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        clearUser()
        return Result.success(Unit)
    }

    override suspend fun checkSession(): Result<User> {
        return try {
            val response = apiService.getSession()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!.user
                saveUser(user)
                Result.success(user)
            } else {
                clearUser()
                Result.failure(Exception("Session expired"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveUser(user: User) {
        _currentUser.value = user
        _isLoggedIn.value = true
        prefs.edit().apply {
            putString("user_id", user.id)
            putString("user_email", user.email)
            putString("user_name", user.name)
            putString("user_image", user.image)
            apply()
        }
    }

    private fun clearUser() {
        _currentUser.value = null
        _isLoggedIn.value = false
        prefs.edit().apply {
            remove("user_id")
            remove("user_email")
            remove("user_name")
            remove("user_image")
            remove("cookies")
            apply()
        }
    }
}
