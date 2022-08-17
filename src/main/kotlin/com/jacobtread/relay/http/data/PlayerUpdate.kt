package com.jacobtread.relay.http.data

import kotlinx.serialization.Serializable

@Serializable
data class PlayerUpdate(
    val displayName: String,
    val credits: Int,
    val inventory: String,
    val csReward: Int
)