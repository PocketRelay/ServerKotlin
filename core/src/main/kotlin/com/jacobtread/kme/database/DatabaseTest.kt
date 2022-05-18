package com.jacobtread.kme.database

import com.jacobtread.kme.utils.MEStringParser
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseTest {

    interface TdfMappable {
        fun mapKey(): String
        fun mapValue(): String
    }

    object Players : IntIdTable("players") {
        val email = varchar("email", length = 254)
        val displayName = varchar("display_name", length = 99)
        val sessionToken = varchar("session_token", length = 128)
            .nullable()
        val password = varchar("password", length = 128)
    }

    class Player(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Player>(Players)

        val email by Players.email
        val displayName by Players.displayName
        val sessionToken by Players.sessionToken
        val password by Players.password

        val classes by PlayerClass referrersOn PlayerClasses.owner
        val characters by PlayerCharacter referrersOn PlayerCharacters.owner
        val settings by PlayerSetting referrersOn PlayerSettings.owner

        fun makeSettingsMap(): LinkedHashMap<String, String> {
            val out = HashMap<String, String>()
            for (playerClass in classes) {
                out[playerClass.mapKey()] = playerClass.mapValue()
            }
            for (character in characters) {
                out[character.mapKey()] = character.mapValue()
            }
            for (setting in settings) {
                out[setting.mapKey()] = setting.mapValue()
            }
            return out
        }

        
    }

    object PlayerClasses : IntIdTable("player_classes") {

        val CLASS_NAMES = arrayOf(
            "Adept",
            "Soldier",
            "Engineer",
            "Sentinel",
            "Infiltrator",
            "Vanguard"
        )

        val index = integer("index")
        val owner = reference("owner_id", Players)
        val name = varchar("name", length = 18)
        val level = integer("level")
        val exp = float("exp")
        val promotions = integer("exp")
    }

    class PlayerClass(id: EntityID<Int>) : IntEntity(id), TdfMappable {
        companion object : IntEntityClass<PlayerClass>(PlayerClasses) {

            fun setFromValue(player: Player, index: Int, value: String) {
                transaction {
                    val existing = PlayerClass.find {
                        (PlayerClasses.owner eq player.id)
                            .and(PlayerClasses.index eq index)
                    }.firstOrNull()
                    if (existing != null) {
                        parseAndApply(index, value, existing)
                    } else {
                        PlayerClass.new { parseAndApply(index, value, this) }
                    }
                }
            }

            private fun parseAndApply(index: Int, value: String, player: PlayerClass) {
                val parser = MEStringParser(value, 6)
                parser.skip(2)
                player.apply {
                    this.index = index
                    name = parser.str()
                    level = parser.int(1)
                    exp = parser.float(0f)
                    promotions = parser.int(0)
                }
            }
        }

        var index by PlayerClasses.index
        var name by PlayerClasses.name
        var level by PlayerClasses.level
        var exp by PlayerClasses.exp
        var promotions by PlayerClasses.promotions

        override fun mapKey(): String = "class$index"
        override fun mapValue(): String = StringBuilder()
            .append("20;4;")
            .append(name).append(';')
            .append(level).append(';')
            .append(exp).append(';')
            .append(promotions)
            .toString()
    }

    object PlayerCharacters : IntIdTable("player_characters") {
        val index = integer("index")
        val owner = reference("owner_id", Players)
        val kitName = varchar("kit_name", length = 128)
        val name = varchar("name", length = 128)
        val tint1 = integer("tint_1")
        val tint2 = integer("tint_2")
        val pattern = integer("pattern")
        val patternColor = integer("patternColor")
        val phong = integer("phong")
        val emissive = integer("emissive")
        val skinTone = integer("skin_tone")
        val secondsPlayed = long("seconds_played")

        val timeStampYear = integer("ts_year")
        val timeStampMonth = integer("ts_month")
        val timeStampDay = integer("ts_day")
        val timeStampSeconds = integer("ts_seconds")

        val powers = text("powers")
        val hotkeys = text("hotkeys")
        val weapons = text("weapons")
        val weaponMods = text("weapon_mods")
        val deployed = bool("deployed")
        val leveledUp = bool("leveled_up")
    }

    class PlayerCharacter(id: EntityID<Int>) : IntEntity(id), TdfMappable {
        companion object : IntEntityClass<PlayerCharacter>(PlayerCharacters) {
            fun setFromValue(player: Player, index: Int, value: String) {
                transaction {
                    val existing = PlayerCharacter.find {
                        (PlayerCharacters.owner eq player.id)
                            .and(PlayerCharacters.index eq index)
                    }.firstOrNull()
                    if (existing != null) {
                        parseAndApply(index, value, existing)
                    } else {
                        PlayerCharacter.new { parseAndApply(index, value, this) }
                    }
                }
            }

            private fun parseAndApply(index: Int, value: String, player: PlayerCharacter) {
                val parser = MEStringParser(value, 22)
                parser.skip(2)
                player.apply {
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

        var index by PlayerCharacters.index
        var kitName by PlayerCharacters.kitName
        var name by PlayerCharacters.name
        var tint1 by PlayerCharacters.tint1
        var tint2 by PlayerCharacters.tint2
        var pattern by PlayerCharacters.pattern
        var patternColor by PlayerCharacters.patternColor
        var phong by PlayerCharacters.phong
        var emissive by PlayerCharacters.emissive
        var skinTone by PlayerCharacters.skinTone
        var secondsPlayed by PlayerCharacters.secondsPlayed
        var timeStampYear by PlayerCharacters.timeStampYear
        var timeStampMonth by PlayerCharacters.timeStampMonth
        var timeStampDay by PlayerCharacters.timeStampDay
        var timeStampSeconds by PlayerCharacters.timeStampSeconds
        var powers by PlayerCharacters.powers
        var hotkeys by PlayerCharacters.hotkeys
        var weapons by PlayerCharacters.weapons
        var weaponMods by PlayerCharacters.weaponMods
        var deployed by PlayerCharacters.deployed
        var leveledUp by PlayerCharacters.leveledUp

        override fun mapKey(): String = "char$index"

        override fun mapValue(): String = StringBuilder()
            .append("20;4;")
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

    object PlayerSettings : IntIdTable("player_settings") {
        val owner = reference("owner_id", Players)
        val key = varchar("key", length = 32)
        val value = text("value")
    }

    class PlayerSetting(id: EntityID<Int>) : IntEntity(id), TdfMappable {
        companion object : IntEntityClass<PlayerSetting>(PlayerSettings)

        var key by PlayerSettings.key
        var value by PlayerSettings.value


        override fun mapKey(): String = key
        override fun mapValue(): String = value
    }


    fun connect() {
        Database.connect(
            url = "jdbc:sqlite:data/app.db1",
            driver = "org.sqlite.JDBC",
        )
        transaction {
            SchemaUtils.create(Players)

            Players.insert {

            }

        }
    }

}