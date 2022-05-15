package com.jacobtread.kme.game

import com.jacobtread.kme.utils.compareHashPassword

data class Player(
    val id: Long,
    val email: String,
    val displayName: String,
    val sessionToken: String?,
    val password: String,
) {

    data class Base(
        val credits: Long = 0,
        val c: Int = -1,
        val d: Int = 0,
        val creditsSpent: Long = 0,
        val e: Int = 0,
        val gamesPlayed: Long,
        val secondsPlayed: Long,
        val f: Int = 0,
        val inventory: String,
    ) {
        override fun toString(): String {
            val builder = StringBuilder()
            builder.append("Base=20;4;")
                .append(credits).append(';')
                .append(c).append(';')
                .append(d).append(';')
                .append(creditsSpent).append(';')
                .append(e).append(';')
                .append(gamesPlayed).append(';')
                .append(secondsPlayed).append(';')
                .append(f).append(';')
                .append(inventory)
            return builder.toString()
        }
    }

    data class PlayerClass(
        val index: Int,
        val name: String,
        val level: Int,
        val exp: Float,
        val promotions: Int,
    ) {
        override fun toString(): String {
            val builder = StringBuilder()
            builder.append("class").append(index)
                .append("=20;4;")
                .append(name).append(';')
                .append(level).append(';')
                .append(exp).append(';')
                .append(promotions)
            return builder.toString()
        }
    }

    data class PlayerCharacter(
        val index: Int,
        val kitName: String,
        val characterName: String,
        val tint1: Int,
        val tint2: Int,
        val pattern: Int,
        val patternColor: Int,
        val phong: Int,
        val emissive: Int,
        val skinTone: Int,
        val secondsPlayed: Long,
        val timeStampYear: Int,
        val timeStampMonth: Int,
        val timeStampDay: Int,
        val timeStampSeconds: Int,
        val powers: String,
        val hotkeys: String,
        val weapons: String,
        val weaponMods: String,
        val deployed: Boolean,
        val leveledUp: Boolean
    ) {
        override fun toString(): String {
            val builder = StringBuilder()
            builder.append("char").append(index)
                .append("=20;4;")
                .append(kitName).append(';')
                .append(characterName).append(';')
                .append(tint1).append(';')
                .append(tint2).append(';')
                .append(pattern).append(';')
                .append(patternColor).append(';')
                .append(phong).append(';')
                .append(emissive).append(';')
                .append(skinTone).append(';')
                .append(secondsPlayed).append(';')
                .append(timeStampYear).append(';')
                .append(timeStampMonth).append(';')
                .append(timeStampDay).append(';')
                .append(timeStampSeconds).append(';')
                .append(powers).append(';')
                .append(hotkeys).append(';')
                .append(weapons).append(';')
                .append(weaponMods).append(';')
                .append(if(deployed) "True" else "False").append(';')
                .append(if(leveledUp) "True" else "False")
            return builder.toString()
        }
    }


    var credits: Int = 0
    var creditsSpent: Int = 0
    var gamesPlayed: Int = 0
    var secondsPlayed: Int = 0
    private var inventory: String? = null

    fun setInventory(value: String) {
        inventory = value
    }

    fun getInventory(): String = inventory ?: "0".repeat(1342)

    fun isMatchingPassword(value: String): Boolean {
        return compareHashPassword(value, this.password)
    }

}