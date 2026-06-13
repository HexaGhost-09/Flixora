package com.hexaghost.flixora.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class SignUpRequest(
    val email: String,
    val password: String,
    val name: String
)

data class SignInRequest(
    val email: String,
    val password: String,
    val rememberMe: Boolean = true
)

data class User(
    val id: String,
    val email: String,
    val name: String,
    val image: String? = null
)

data class Session(
    val id: String,
    val userId: String,
    val expiresAt: String
)

data class SessionResponse(
    val session: Session,
    val user: User
)

data class AuthResponse(
    val token: String? = null,
    val user: User? = null
)

interface AuthApiService {
    @POST("signup/email")
    suspend fun signUp(@Body request: SignUpRequest): Response<AuthResponse>

    @POST("signin/email")
    suspend fun signIn(@Body request: SignInRequest): Response<AuthResponse>

    @GET("get-session")
    suspend fun getSession(): Response<SessionResponse>
}
