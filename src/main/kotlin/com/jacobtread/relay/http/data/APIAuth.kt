package com.jacobtread.relay.http.data

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val token: String
)

@Serializable
data class CheckTokenRequest(
    val token: String
)