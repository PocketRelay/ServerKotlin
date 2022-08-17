package com.jacobtread.relay.http.data

import com.jacobtread.relay.database.data.Player
import com.jacobtread.relay.database.data.PlayerClass
import kotlinx.serialization.Serializable

@Serializable
data class FullPlayerData(
    val player: Player,
    val classes: List<PlayerClass>,
    val characters: List<CharacterSerializable>,
)

@Serializable
data class CharacterSerializable(
    val index: Int,
    val kitName: String,
    val name: String,
    val deployed: Boolean,
)