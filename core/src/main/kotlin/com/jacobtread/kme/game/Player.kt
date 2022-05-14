package com.jacobtread.kme.game

import com.jacobtread.kme.utils.compareHashPassword

data class Player(
    val id: Long,
    val email: String,
    val displayName: String,
    val credits: Int,
    val games_player: Int,
    val sessionToken: String?,
    val password: String,
) {

    fun isMatchingPassword(value: String): Boolean {
        return compareHashPassword(value, this.password)
    }

}