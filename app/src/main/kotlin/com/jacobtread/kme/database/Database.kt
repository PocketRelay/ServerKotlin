package com.jacobtread.kme.database

import com.jacobtread.kme.Environment
import com.jacobtread.kme.database.entities.PlayerEntity
import com.jacobtread.kme.database.tables.PlayersTable
import com.jacobtread.kme.tools.MEStringParser
import com.jacobtread.kme.tools.unixTimeSeconds
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min


/**
 * DatabaseConfig Stores configuration information about the database
 *
 * @property type Defines which database connection type to use
 * @property mysql The config for a MySQL database connection
 * @property sqlite The config for a SQLite database connection
 * @constructor Create empty DatabaseConfig
 */
@Serializable
data class DatabaseConfig(
    val type: String = "sqlite",
    val host: String = "127.0.0.1",
    val port: Int = 3306,
    val user: String = "root",
    val password: String = "password",
    val database: String = "kme",
    val file: String = "data/app.db",
)

/**
 * Create database tables Creates the nessicary database tables
 * using the SchemaUtils this is required for the program to
 * continue
 *
 */
internal fun createDatabaseTables() {
    transaction {
        SchemaUtils.create(
            PlayersTable,
            PlayerClasses,
            PlayerCharacters,
            PlayerSettings,
            PlayerGalaxyAtWars,
            Messages
        )
    }
}

//region Tables and Models

const val MIN_GAW_VALUE = 5000

/**
 * PlayerSettingsBase
 *
 * @property credits The number of spendable credits the player has
 * @property c Unknown
 * @property d Unknown
 * @property creditsSpent The number of credits the player has spent
 * @property e Unknown
 * @property gamesPlayed The number of complete games the player has played
 * @property secondsPlayed The number of seconds the player has played for
 * @property f Unknown
 * @property inventory Complex string of the player inventory contents (Not yet parsed)
 * @constructor Create empty PlayerSettingsBase
 */
@Serializable
data class PlayerSettingsBase(
    val credits: Int = 0,
    val c: Int = -1,
    val d: Int = 0,
    val creditsSpent: Int = 0,
    val e: Int = 0,
    val gamesPlayed: Int = 0,
    val secondsPlayed: Long = 0,
    val f: Int = 0,
    val inventory: String = "",
) {
    companion object {
        fun createFromValue(value: String): PlayerSettingsBase {
            val parser = MEStringParser(value, 11)
            parser.skip(2)
            return PlayerSettingsBase(
                credits = parser.int(),
                c = parser.int(-1),
                d = parser.int(),
                creditsSpent = parser.int(),
                e = parser.int(),
                gamesPlayed = parser.int(),
                secondsPlayed = parser.long(),
                f = parser.int(),
                inventory = parser.str()
            )
        }
    }

    fun mapValue(): String = StringBuilder()
        .append("20;4;")
        .append(credits).append(';')
        .append(c).append(';')
        .append(d).append(';')
        .append(creditsSpent).append(';')
        .append(e).append(';')
        .append(gamesPlayed).append(';')
        .append(secondsPlayed).append(';')
        .append(f).append(';')
        .append(inventory)
        .toString()
}

/**
 * PlayerClasses Stores the class information for each class
 * that the player owns. This is created by the game rather
 * than the server
 *
 * @constructor Create empty PlayerClasses
 */
object PlayerClasses : IntIdTable("player_classes") {
    val player = reference("player_id", PlayersTable)
    val index = integer("index")
    val name = varchar("name", length = 18)
    val level = integer("level")
    val exp = float("exp")
    val promotions = integer("promotions")
}

class PlayerClass(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerClass>(PlayerClasses) {

        /**
         * setClassFrom Sets the player's class from the provided index and value.
         * This will update existing data or create a new row in the classes table
         *
         * @param playerEntity The player this class belongs to
         * @param index The index/id of this player class
         * @param value The encoded class data
         */
        fun setClassFrom(playerEntity: PlayerEntity, index: Int, value: String) {
            PlayerClass.updateOrCreate({ (PlayerClasses.player eq playerEntity.id) and (PlayerClasses.index eq index) }) {
                this.player = playerEntity.id
                parse(index, value, this)
            }
        }

        private fun parse(index: Int, value: String, out: PlayerClass) {
            val parser = MEStringParser(value, 6)
            parser.skip(2)
            out.apply {
                this.index = index
                name = parser.str()
                level = parser.int(1)
                exp = parser.float(0f)
                promotions = parser.int(0)
            }
        }
    }

    var player by PlayerClasses.player
    var index by PlayerClasses.index
    var name by PlayerClasses.name
    var level by PlayerClasses.level
    var exp by PlayerClasses.exp
    var promotions by PlayerClasses.promotions

    fun mapKey(): String = "class$index"
    fun mapValue(): String = StringBuilder()
        .append("20;4;")
        .append(name).append(';')
        .append(level).append(';')
        .append(exp).append(';')
        .append(promotions)
        .toString()
}

object PlayerCharacters : IntIdTable("player_characters") {
    val player = reference("player_id", PlayersTable)

    val index = integer("index")
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


class PlayerCharacter(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerCharacter>(PlayerCharacters) {
        /**
         * setCharacterFrom Sets the player's character from the provided index
         * and value. This will update existing data or create a new row in the
         * characters table
         *
         * @param playerEntity The player this character belongs to
         * @param index The index/id of this character
         * @param value The encoded value of this character data
         */
        fun setCharacterFrom(playerEntity: PlayerEntity, index: Int, value: String) {
            PlayerCharacter.updateOrCreate({ (PlayerCharacters.player eq playerEntity.id) and (PlayerCharacters.index eq index) }) {
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
        private fun parse(index: Int, value: String, out: PlayerCharacter) {
            val parser = MEStringParser(value, 22)
            parser.skip(2)
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

    var player by PlayerCharacters.player
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

    fun mapKey(): String = "char$index"
    fun mapValue(): String = StringBuilder()
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

object PlayerGalaxyAtWars : IntIdTable("player_gaw") {
    val player = reference("player_id", PlayersTable)

    val timestamp = long("timestamp")

    val a = integer("a").default(5000)
    val b = integer("b").default(5000)
    val c = integer("c").default(5000)
    val d = integer("d").default(5000)
    val e = integer("e").default(5000)

}

class PlayerGalaxyAtWar(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerGalaxyAtWar>(PlayerGalaxyAtWars) {
        fun create(playerEntity: PlayerEntity): PlayerGalaxyAtWar {
            return transaction {
                PlayerGalaxyAtWar.new {
                    player = playerEntity.id
                    timestamp = unixTimeSeconds()
                }
            }
        }
    }

    var player by PlayerGalaxyAtWars.player
    var timestamp by PlayerGalaxyAtWars.timestamp

    var a by PlayerGalaxyAtWars.a
    var b by PlayerGalaxyAtWars.b
    var c by PlayerGalaxyAtWars.c
    var d by PlayerGalaxyAtWars.d
    var e by PlayerGalaxyAtWars.e

    fun increase(ai: Int, bi: Int, ci: Int, di: Int, ei: Int) {
        transaction {
            val maxValue = 10099
            a = min(maxValue, a + ai)
            b = min(maxValue, b + bi)
            c = min(maxValue, c + ci)
            d = min(maxValue, d + di)
            e = min(maxValue, e + ei)
        }
    }

    fun applyDecay() {
        if (Environment.gawReadinessDecay > 0f) {
            transaction {
                val minValue = 5000
                val time = unixTimeSeconds()
                val timeDifference = time - timestamp
                val days = timeDifference / 86400f
                val decayValue = (Environment.gawReadinessDecay * days * 100).toInt()
                a = max(minValue, a - decayValue)
                b = max(minValue, b - decayValue)
                c = max(minValue, c - decayValue)
                d = max(minValue, d - decayValue)
                e = max(minValue, e - decayValue)
            }
        }
    }

    fun average(): Int = (a + b + c + d + e) / 5
}

/**
 * PlayerSettings
 *
 * @constructor Create empty PlayerSettings
 */
object PlayerSettings : IntIdTable("player_settings") {
    val player = reference("player_id", PlayersTable)

    val key = varchar("key", length = 32)
    val value = text("value")
}

class PlayerSetting(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerSetting>(PlayerSettings) {
        /**
         * setSettingUnknown Stores setting key values pairs for settings
         * that are not parsed. Will update existing values if there are
         * any otherwise will create new row
         *
         * @param playerEntity The player to set the setting for
         * @param key The setting key
         * @param value The setting value
         */
        fun setSetting(playerEntity: PlayerEntity, key: String, value: String) {
            val playerId = playerEntity.id
            PlayerSetting.updateOrCreate({ (PlayerSettings.player eq playerId) and (PlayerSettings.key eq key) }) {
                this.player = playerId
                this.key = key
                this.value = value
            }
        }
    }

    var player by PlayerSettings.player
    var key by PlayerSettings.key
    var value by PlayerSettings.value
}

object Messages : IntIdTable("messages") {

    // The different message types
    const val MENU_TABBED_TYPE: Byte = 0
    const val MENU_SCROLLING_TYPE: Byte = 1
    const val MULTIPLAYER_PROMOTION: Byte = 8

    val endDate = long("end_date")
    val image = varchar("image", 120)
    val message = text("message")
    val title = varchar("title", 255)
    val priority = short("priority")
    val type = byte("type")
}

class Message(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Message>(Messages) {
        val DATE_FORMAT = DateTimeFormatter.ofPattern("MM:dd:yyyy")

        fun createMessageMap(): LinkedHashMap<String, String> {
            return transaction {
                val out = LinkedHashMap<String, String>()
                val messages = Message.all()
                val locales = arrayOf("de", "es", "fr", "it", "ja", "pl", "ru")
                messages.forEachIndexed { i, message ->
                    val index = i + 1
                    out["MSG_${index}_endDate"] = Message.DATE_FORMAT.format(LocalDate.ofEpochDay(message.endDate))
                    out["MSG_${index}_image"] = message.image
                    out["MSG_${index}_message"] = message.message
                    locales.forEach { locale ->
                        out["MSG_${index}_message_$locale"] = message.message
                    }
                    out["MSG_${index}_priority"] = message.priority.toString()
                    out["MSG_${index}_title"] = message.title
                    locales.forEach { locale ->
                        out["MSG_${index}_title_$locale"] = message.title
                    }
                    out["MSG_${index}_trackingId"] = message.id.value.toString()
                    out["MSG_${index}_type"] = message.type.toString()
                }
                out
            }
        }

        fun create(
            title: String,
            message: String,
            image: String = "Promo_n7.dds",
            priority: Short = 0,
            type: Byte = Messages.MENU_SCROLLING_TYPE,
            endDate: LocalDate = LocalDate.now().plusDays(15),
        ) {
            val timestamp = endDate.toEpochDay()
            transaction {
                Message.new {
                    this.endDate = timestamp
                    this.image = image
                    this.title = title
                    this.message = message
                    this.priority = priority
                    this.type = type
                }
            }
        }
    }

    var endDate by Messages.endDate
    var image by Messages.image
    var message by Messages.message
    var title by Messages.title
    var priority by Messages.priority
    var type by Messages.type
}


//endregion
