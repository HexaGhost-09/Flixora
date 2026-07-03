package com.hexaghost.flixora.data.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TraktManager @Inject constructor() {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class DeviceCodeResponse(
        @SerializedName("device_code") val deviceCode: String,
        @SerializedName("user_code") val userCode: String,
        @SerializedName("verification_url") val verificationUrl: String,
        @SerializedName("expires_in") val expiresIn: Int,
        @SerializedName("interval") val interval: Int
    )

    data class TokenResponse(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("token_type") val tokenType: String,
        @SerializedName("expires_in") val expiresIn: Long,
        @SerializedName("refresh_token") val refreshToken: String,
        @SerializedName("scope") val scope: String,
        @SerializedName("created_at") val createdAt: Long
    )

    data class UserProfile(
        @SerializedName("username") val username: String,
        @SerializedName("name") val name: String?
    )

    suspend fun generateDeviceCode(clientId: String): DeviceCodeResponse? = withContext(Dispatchers.IO) {
        val json = """{"client_id":"$clientId"}"""
        val request = Request.Builder()
            .url("https://api.trakt.tv/oauth/device/code")
            .post(json.toRequestBody(jsonMediaType))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    gson.fromJson(body, DeviceCodeResponse::class.java)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun pollDeviceToken(clientId: String, clientSecret: String, deviceCode: String): TokenResponse? = withContext(Dispatchers.IO) {
        val json = """
            {
              "code": "$deviceCode",
              "client_id": "$clientId",
              "client_secret": "$clientSecret"
            }
        """.trimIndent()
        val request = Request.Builder()
            .url("https://api.trakt.tv/oauth/device/token")
            .post(json.toRequestBody(jsonMediaType))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    gson.fromJson(body, TokenResponse::class.java)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchUsername(token: String, clientId: String): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.trakt.tv/users/me")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("trakt-api-version", "2")
            .addHeader("trakt-api-key", clientId)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val profile = gson.fromJson(body, UserProfile::class.java)
                    profile.username
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun scrobble(
        action: String, // "start", "pause", "stop"
        token: String,
        clientId: String,
        tmdbId: Int,
        mediaType: String,
        progress: Float
    ): Boolean = withContext(Dispatchers.IO) {
        val isTv = mediaType.equals("tv", ignoreCase = true)
        val payload = if (isTv) {
            """
            {
              "show": {
                "ids": {
                  "tmdb": $tmdbId
                }
              },
              "episode": {
                "season": 1,
                "number": 1
              },
              "progress": $progress
            }
            """.trimIndent()
        } else {
            """
            {
              "movie": {
                "ids": {
                  "tmdb": $tmdbId
                }
              },
              "progress": $progress
            }
            """.trimIndent()
        }

        val request = Request.Builder()
            .url("https://api.trakt.tv/scrobble/$action")
            .post(payload.toRequestBody(jsonMediaType))
            .addHeader("Authorization", "Bearer $token")
            .addHeader("trakt-api-version", "2")
            .addHeader("trakt-api-key", clientId)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
