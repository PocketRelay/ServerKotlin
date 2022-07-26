package com.jacobtread.kme.database.old.entities

import com.jacobtread.kme.database.old.tables.PlayerCharactersTable
import com.jacobtread.kme.database.updateOrCreate
import com.jacobtread.kme.tools.MEStringParser
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class PlayerCharacterEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerCharacterEntity>(PlayerCharactersTable) {
        /**
         * setCharacterFrom Sets the player's character from the provided index
         * and value. This will update existing data or create a new row in the
         * characters table
         *
         * @param playerEntity The player this character belongs to
         * @param index The index/id of this character
         * @param value The encoded value of this character data
         */
        fun updateOrCreate(playerEntity: PlayerEntity, index: Int, value: String) {
            PlayerCharacterEntity.updateOrCreate({ (PlayerCharactersTable.player eq playerEntity.id) and (PlayerCharactersTable.index eq index) }) {
                this.player = playerEntity.id
                parse(index, value, this)
            }
        }

        /**
         * parse Parses the player character and applies the parsed
         * values to the provided PlayerCharacter object
         *
         * @param index The index of the character
         * @param value The encoded character value
         * @param out The object to update
         */
        private fun parse(index: Int, value: String, out: PlayerCharacterEntity) {
            val parser = MEStringParser(value, 22)
            out.apply {
                this.index = index
                kitName = parser.str()
                name = parser.str()
                tint1 = parser.int()
                tint2 = parser.int()
                pattern = parser.int()
                patternColor = parser.int()
                phong = parser.int()
                emissive = parser.int()
                skinTone = parser.int()
                secondsPlayed = parser.long()
                timeStampYear = parser.int()
                timeStampMonth = parser.int()
                timeStampDay = parser.int()
                timeStampSeconds = parser.int()
                powers = parser.str()
                hotkeys = parser.str()
                weapons = parser.str()
                weaponMods = parser.str()
                deployed = parser.bool()
                leveledUp = parser.bool()
            }
        }
    }

    var player by PlayerCharactersTable.player
    var index by PlayerCharactersTable.index
    var kitName by PlayerCharactersTable.kitName
    var name by PlayerCharactersTable.name
    var tint1 by PlayerCharactersTable.tint1
    var tint2 by PlayerCharactersTable.tint2
    var pattern by PlayerCharactersTable.pattern
    var patternColor by PlayerCharactersTable.patternColor
    var phong by PlayerCharactersTable.phong
    var emissive by PlayerCharactersTable.emissive
    var skinTone by PlayerCharactersTable.skinTone
    var secondsPlayed by PlayerCharactersTable.secondsPlayed
    var timeStampYear by PlayerCharactersTable.timeStampYear
    var timeStampMonth by PlayerCharactersTable.timeStampMonth
    var timeStampDay by PlayerCharactersTable.timeStampDay
    var timeStampSeconds by PlayerCharactersTable.timeStampSeconds
    var powers by PlayerCharactersTable.powers
    var hotkeys by PlayerCharactersTable.hotkeys
    var weapons by PlayerCharactersTable.weapons
    var weaponMods by PlayerCharactersTable.weaponMods
    var deployed by PlayerCharactersTable.deployed
    var leveledUp by PlayerCharactersTable.leveledUp

    val key: String get() = "char$index"
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
        .append(timeStampYear).append(';')
        .append(timeStampMonth).append(';')
        .append(timeStampDay).append(';')
        .append(timeStampSeconds).append(';')
        .append(powers).append(';')
        .append(hotkeys).append(';')
        .append(weapons).append(';')
        .append(weaponMods).append(';')
        .append(if (deployed) "True" else "False").append(';')
        .append(if (leveledUp) "True" else "False")
        .toString()
}