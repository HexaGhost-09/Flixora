package com.hexaghost.flixora.data.api

// User model used throughout the app for auth state
data class User(
    val id: String,
    val email: String,
    val name: String,
    val image: String? = null
)
