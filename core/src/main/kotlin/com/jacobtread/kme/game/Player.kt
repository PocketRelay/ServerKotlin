package com.jacobtread.kme.game

import com.jacobtread.kme.utils.compareHashPassword

data class Player(
    val id: Int,
    val email: String,
    val display_name: String,
    val credits: Int,
    val games_player: Int,
    val auth: String,
    val password: String,
) {

    fun isMatchingPassword(value: String): Boolean {
        return compareHashPassword(value, this.password)
    }

}