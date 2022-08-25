package com.jacobtread.relay.database.models

import com.jacobtread.relay.utils.MEStringParser
import kotlinx.serialization.Serializable

@Serializable
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

    // Name UKNOWN Level, 1 = 4 top, 2
    /**
     * Format is as followed:
     * Each value is split by a comma
     *
     * 1: Name
     * 2: ID
     * 3: Level 0 - 6
     * 4: Rank 4 Top (0 = Not Unlocked, 1 = Unlocked)
     * 5: Rank 4 Botom (0 = Not Unlocked, 1 = Unlocked)
     * 6: Rank 5 Top (0 = Not Unlocked, 2 = Unlocked)
     * 7: Rank 5 Bottom (0 = Not Unlocked, 2 = Unlocked)
     * 8: Rank 6 Top (0 = Not Unlocked, 3 = Unlocked)
     * 9: Rank 5: Bottom (0 = Not Unlocked, 3 = Unlocked)
     * 10: Unknown
     * 11: Where this belongs to this character (True/False)
     *
     *
     */
    var powers: String,
    val hotkeys: String,
    val weapons: String,
    val weaponMods: String,
    val deployed: Boolean,
    val leveledUp: Boolean,
) {

    fun getParsedPowers(): List<PowerData> {
        val powerData = ArrayList<PowerData>()
        val powersSplit = powers.split(',')
        for (section in powersSplit) {
            val parts = section.split(' ', limit = 11)
            if (parts.size < 11) continue
            val name = parts[0]
            val ua = parts[1].toIntOrNull()
            val level = parts[2].toFloatOrNull()
            if (ua == null || level == null) {
                continue
            }
            val rankAT = parts[3].toInt()
            val rankAB = parts[4].toInt()
            val rankBT = parts[5].toInt()
            val rankBB = parts[6].toInt()
            val rankCT = parts[7].toInt()
            val rankCB = parts[8].toInt()
            val ub = parts[9].toInt()
            val specific = parts[10]
                .lowercase()
                .toBoolean()

            powerData.add(
                PowerData(
                    name,
                    ua,
                    level,
                    if (rankAT == 1) 1 else if (rankAB == 1) 2 else 0,
                    if (rankBT == 2) 1 else if (rankBB == 2) 2 else 0,
                    if (rankCT == 3) 1 else if (rankCB == 3) 2 else 0,
                    ub,
                    specific
                )
            )
        }
        return powerData
    }

    fun setPowersFromParsed(values: List<PowerData>) {
        powers = values.joinToString(",") { it.toEncoded() }
    }

    data class PowerData(
        val name: String,
        val id: Int,
        var level: Float,
        var rank4: Int,
        var rank5: Int,
        var rank6: Int,
        var ub: Int,
        val specific: Boolean,
    ) {
        fun toEncoded(): String = StringBuilder(name)
            .append(' ')
            .append(id)
            .append(' ')
            .append(level)
            .append(' ')
            .append(if (rank4 == 1 || rank4 == 3) 1 else 0)
            .append(' ')
            .append(if (rank4 == 2 || rank4 == 3) 1 else 0)
            .append(' ')
            .append(if (rank5 == 1 || rank5 == 3) 2 else 0)
            .append(' ')
            .append(if (rank5 == 2 || rank5 == 3) 2 else 0)
            .append(' ')
            .append(if (rank6 == 1 || rank6 == 3) 3 else 0)
            .append(' ')
            .append(if (rank6 == 2 || rank6 == 3) 3 else 0)
            .append(' ')
            .append(ub)
            .append(' ')
            .append(if (specific) "True" else "False")
            .toString()
    }


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
