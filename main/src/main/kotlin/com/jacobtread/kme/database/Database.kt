package com.jacobtread.kme.database

import com.jacobtread.kme.Config
import com.jacobtread.kme.utils.MEStringParser
import com.jacobtread.kme.utils.unixTimeSeconds
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists
import kotlin.math.max
import org.jetbrains.exposed.sql.Database as ExposedDatabase

/**
 * DatabaseType Enum containing the different supported Database
 * connection types. Currently, only MySQL and SQLite may add more
 * such as PostgresSQL when releasing for production
 *
 * @constructor Create empty DatabaseType
 */
@Serializable
enum class DatabaseType {
    @SerialName("mysql")
    MYSQL,

    @SerialName("sqlite")
    SQLITE;
}

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
    @Comment("The type of database to use MySQL or SQLite")
    val type: DatabaseType = DatabaseType.SQLITE,
    @Comment("Settings for connecting to MySQL database")
    val mysql: MySQLConfig = MySQLConfig(),
    @Comment("Settings used for connecting to SQLite database")
    val sqlite: SQLiteConfig = SQLiteConfig(),
)

/**
 * MySQLConfig The config to use when targeting a MySQL database
 *
 * @property host The host address of the MySQL server
 * @property port The port of the MySQL server
 * @property user The user account for the MySQL server
 * @property password The password for the MySQL server account
 * @property database The database on the MySQL server to use
 * @constructor Create empty MySQLConfig
 */
@Serializable
data class MySQLConfig(
    val host: String = "127.0.0.1",
    val port: String = "3306",
    val user: String = "root",
    val password: String = "password",
    val database: String = "kme",
)

/**
 * SQLiteConfig The config to use when targeting a SQLite
 * database takes only a file path
 *
 * @property file The file to use as the SQLite database
 * @constructor Create empty SQLiteConfig
 */
@Serializable
data class SQLiteConfig(
    val file: String = "data/app.db",
)

/**
 * startDatabase "Starts" the database by connecting to the database
 * that was specified in the configuration file. Then creates the
 * necessary tables if they don't already exist
 *
 * @param config The database configuration
 */
fun startDatabase(config: DatabaseConfig) {
    when (config.type) {
        DatabaseType.MYSQL -> {
            val mysql = config.mysql
            ExposedDatabase.connect(
                url = "jdbc:mysql://${mysql.host}:${mysql.port}/${mysql.database}",
                user = mysql.user,
                password = mysql.password
            )
        }
        DatabaseType.SQLITE -> {
            val file = config.sqlite.file
            val parentDir = Paths.get(file).absolute().parent
            if (parentDir.notExists()) parentDir.createDirectories()
            ExposedDatabase.connect("jdbc:sqlite:$file")
        }
    }

    transaction {
        SchemaUtils.create(
            Players,
            PlayerClasses,
            PlayerCharacters,
            PlayerSettings,
            PlayerGalaxyAtWars
        )
    }
}

//region Helpers

/**
 * findOne Shorthand for finding a singular entry or null limits the number
 * of rows to find to 1 and returns the first row or null
 *
 * @param ID The ID type
 * @param T The entity type
 * @param op The SQL operation builder
 * @receiver
 * @return The found row or null
 */
private fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.findOne(op: SqlExpressionBuilder.() -> Op<Boolean>): T? = transaction {
    find(op).limit(1).firstOrNull()
}

private fun <ID : Comparable<ID>, T : Entity<ID>>
        EntityClass<ID, T>.updateOrCreate(op: SqlExpressionBuilder.() -> Op<Boolean>, apply: T.() -> Unit) = transaction {
    val value = find(op).limit(1).firstOrNull()
    value?.apply() ?: new(apply)
}

//endregion

//region Tables and Models

/**
 * Players The table which stores all the players credentials,
 * session information and basic account data
 *
 * @constructor Create empty Players
 */
object Players : IntIdTable("players") {
    val email = varchar("email", length = 254)
    val displayName = varchar("display_name", length = 99)
    val sessionToken = varchar("session_token", length = 128)
        .nullable()
        .default(null)
    val password = varchar("password", length = 128)
    val settingsBase = text("settings_base")
        .nullable()
        .default(null)
}

const val MIN_GAW_VALUE = 5000

/**
 * Player Represents a player stored in the database each player
 * has a related collection of classes, characters, and settings
 *
 * @constructor
 *
 * @param id The unique identifier for this player
 */
class Player(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Player>(Players)

    val playerId: Int get() = id.value

    var email by Players.email
    var displayName by Players.displayName
    var password by Players.password

    var sessionToken by Players.sessionToken
    private var settingsBase by Players.settingsBase

    private val classes by PlayerClass referrersOn PlayerClasses.player
    private val characters by PlayerCharacter referrersOn PlayerCharacters.player
    private val settings by PlayerSetting referrersOn PlayerSettings.player

    fun getOrCreateGAW(config: Config.GalaxyAtWarConfig): PlayerGalaxyAtWar {
        val existing = PlayerGalaxyAtWar.findOne { (PlayerGalaxyAtWars.player eq this@Player.id) }
        if (existing != null) {
            if (config.readinessDailyDecay > 0f) {
                val time = unixTimeSeconds()
                val timeDiff = time - existing.timestamp
                val days = timeDiff.toFloat() / 86400
                val decayValue = (config.readinessDailyDecay * days * 100).toInt()
                transaction {
                    existing.a = max(MIN_GAW_VALUE, existing.a - decayValue)
                    existing.b = max(MIN_GAW_VALUE, existing.b - decayValue)
                    existing.c = max(MIN_GAW_VALUE, existing.c - decayValue)
                    existing.d = max(MIN_GAW_VALUE, existing.d - decayValue)
                    existing.e = max(MIN_GAW_VALUE, existing.e - decayValue)
                }
            }
            return existing
        }
        val seconds = unixTimeSeconds()
        return transaction {
            PlayerGalaxyAtWar.new {
                player = this@Player.id
                timestamp = seconds
            }
        }
    }

    /**
     * setSetting Updates a user setting. Settings that are parsed such
     * as classes, characters, and the base setting are handled separately
     * and other settings get their own rows in the setting table
     *
     * @param key The key of the setting
     * @param value The value of the setting
     */
    fun setSetting(key: String, value: String) {
        if (key.startsWith("class")) { // Class Setting
            val index = key.substring(5).toInt()
            PlayerClass.setClassFrom(this, index, value)
        } else if (key.startsWith("char")) { // Character Setting
            val index = key.substring(4).toInt()
            PlayerCharacter.setCharacterFrom(this, index, value)
        } else if (key == "Base") { // Base Setting
            transaction { settingsBase = value }
        } else { // Other Setting
            PlayerSetting.setSetting(this, key, value)
        }
    }


    /**
     * getSettingsBase Parses the base settings field and returns
     * it as a PlayerSettingsBase object if parsing fails then a
     * default PlayerSettingsBase is returned instead
     *
     * @return The player settings base
     */
    fun getSettingsBase(): PlayerSettingsBase {
        val base = settingsBase
        return if (base != null) PlayerSettingsBase.createFromValue(base) else PlayerSettingsBase()
    }

    /**
     * createSettingsMap Stores all the settings from this in a LinkedHashMap
     * so that they can be sent to the client
     *
     * @return
     */
    fun createSettingsMap(): LinkedHashMap<String, String> {
        val out = LinkedHashMap<String, String>()
        transaction {
            for (playerClass in classes) {
                out[playerClass.mapKey()] = playerClass.mapValue()
            }
            for (character in characters) {
                out[character.mapKey()] = character.mapValue()
            }
            for (setting in settings) {
                out[setting.key] = setting.value
            }
            settingsBase?.let { out["Base"] = it }
        }
        return out
    }

    /**
     * getN7Rating Produces a rating value based on the total
     * level and number of promotions this player has.
     *
     * @return The calculated N7 rating
     */
    fun getN7Rating(): Int {
        return transaction {
            var level = 0
            var promotions = 0
            for (playerClass in classes) {
                level += playerClass.level
                promotions += playerClass.promotions
            }
            level + promotions * 30
        }
    }

    fun getTotalPromotions(): Int {
        return transaction { classes.sumOf { it.promotions } }
    }
}

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
class PlayerSettingsBase(
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

    fun mapKey(): String = "Base"
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
    val player = reference("player_id", Players)
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
         * @param player The player this class belongs to
         * @param index The index/id of this player class
         * @param value The encoded class data
         */
        fun setClassFrom(player: Player, index: Int, value: String) {
            PlayerClass.updateOrCreate({ (PlayerClasses.player eq player.id) and (PlayerClasses.index eq index) }) {
                this.player = player.id
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
    val player = reference("player_id", Players)

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
         * @param player The player this character belongs to
         * @param index The index/id of this character
         * @param value The encoded value of this character data
         */
        fun setCharacterFrom(player: Player, index: Int, value: String) {
            PlayerCharacter.updateOrCreate({ (PlayerCharacters.player eq player.id) and (PlayerCharacters.index eq index) }) {
                this.player = player.id
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
    val player = reference("player_id", Players)

    val timestamp = long("timestamp")

    val a = integer("a").default(MIN_GAW_VALUE)
    val b = integer("b").default(MIN_GAW_VALUE)
    val c = integer("c").default(MIN_GAW_VALUE)
    val d = integer("d").default(MIN_GAW_VALUE)
    val e = integer("e").default(MIN_GAW_VALUE)
}

class PlayerGalaxyAtWar(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerGalaxyAtWar>(PlayerGalaxyAtWars)

    var player by PlayerGalaxyAtWars.player
    var timestamp by PlayerGalaxyAtWars.timestamp

    var a by PlayerGalaxyAtWars.a
    var b by PlayerGalaxyAtWars.b
    var c by PlayerGalaxyAtWars.c
    var d by PlayerGalaxyAtWars.d
    var e by PlayerGalaxyAtWars.e
}

/**
 * PlayerSettings
 *
 * @constructor Create empty PlayerSettings
 */
object PlayerSettings : IntIdTable("player_settings") {
    val player = reference("player_id", Players)

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
         * @param player The player to set the setting for
         * @param key The setting key
         * @param value The setting value
         */
        fun setSetting(player: Player, key: String, value: String) {
            val playerId = player.id
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

//endregion
