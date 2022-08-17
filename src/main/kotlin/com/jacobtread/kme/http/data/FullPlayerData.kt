package com.jacobtread.kme.http.data

import com.jacobtread.kme.database.data.Player
import com.jacobtread.kme.database.data.PlayerCharacter
import com.jacobtread.kme.database.data.PlayerClass
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