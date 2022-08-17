package com.jacobtread.relay.http.data

import com.jacobtread.relay.database.data.Player
import kotlinx.serialization.Serializable

@Serializable
data class GameSerializable(
    val id: ULong,
    val gameState: Int,
    val gameSetting: Int,
    val attributes: Map<String, String>,
    val players: List<Player>,
)