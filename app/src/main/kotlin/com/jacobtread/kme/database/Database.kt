package com.jacobtread.kme.database

import com.jacobtread.kme.database.tables.*
import com.jacobtread.kme.tools.MEStringParser
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


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
            PlayerClassesTable,
            PlayerCharactersTable,
            PlayerSettingsTable,
            PlayerGalaxyAtWarsTable,
            MessagesTable
        )
    }
}

//region Tables and Models

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

    fun toEncodedValue(): String = StringBuilder()
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


//endregion
