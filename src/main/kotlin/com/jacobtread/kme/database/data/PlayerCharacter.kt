package com.jacobtread.kme.database.data

import com.jacobtread.kme.utils.MEStringParser

data class PlayerCharacter(
    val index: Int,
    val kitName: String,
    val name: String,
    val tint1: Int,
    val tint2: Int,
    val pattern: Int,
    val patternColor: Int,
    val phong: Int,
    val emissive: Int,
    val skinTone: Int,
    val secondsPlayed: Long,

    val timestampYear: Int,
    val timestampMonth: Int,
    val timestampDay: Int,
    val timestampSeconds: Int,

    val powers: String,
    val hotkeys: String,
    val weapons: String,
    val weaponMods: String,
    val deployed: Boolean,
    val leveledUp: Boolean,
) {
    fun getKey(): String = "char$index"
    fun toEncoded(): String = StringBuilder("20;4;")
        .append(kitName).append(';')
        .append(name).append(';')
        .append(tint1).append(';')
        .append(tint2).append(';')
        .append(pattern).append(';')
        .append(patternColor).append(';')
        .append(phong).append(';')
        .append(emissive).append(';')
        .append(skinTone).append(';')
        .append(secondsPlayed).append(';')
        .append(timestampYear).append(';')
        .append(timestampMonth).append(';')
        .append(timestampDay).append(';')
        .append(timestampSeconds).append(';')
        .append(powers).append(';')
        .append(hotkeys).append(';')
        .append(weapons).append(';')
        .append(weaponMods).append(';')
        .append(if (deployed) "True" else "False").append(';')
        .append(if (leveledUp) "True" else "False")
        .toString()

    companion object {
        fun createFromKeyValue(key: String, value: String): PlayerCharacter {
            val index = key.substring(4).toInt()
            val parser = MEStringParser(value, 22)
            return PlayerCharacter(
                index = index,
                kitName = parser.str(),
                name = parser.str(),
                tint1 = parser.int(),
                tint2 = parser.int(),
                pattern = parser.int(),
                patternColor = parser.int(),
                phong = parser.int(),
                emissive = parser.int(),
                skinTone = parser.int(),
                secondsPlayed = parser.long(),
                timestampYear = parser.int(),
                timestampMonth = parser.int(),
                timestampDay = parser.int(),
                timestampSeconds = parser.int(),
                powers = parser.str(),
                hotkeys = parser.str(),
                weapons = parser.str(),
                weaponMods = parser.str(),
                deployed = parser.bool(),
                leveledUp = parser.bool(),
            )
        }
    }
}