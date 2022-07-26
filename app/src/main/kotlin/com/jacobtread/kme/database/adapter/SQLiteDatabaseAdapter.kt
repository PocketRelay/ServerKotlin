package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.database.RuntimeDriver
import com.jacobtread.kme.database.data.GalaxyAtWarData
import com.jacobtread.kme.database.data.Player
import com.jacobtread.kme.database.data.PlayerCharacter
import com.jacobtread.kme.database.data.PlayerClass
import com.jacobtread.kme.utils.logging.Logger
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

class SQLiteDatabaseAdapter(file: String) : DatabaseAdapter {

    private val connection: Connection

    init {
        val version = "3.36.0.3"
        RuntimeDriver.createRuntimeDriver(
            "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/$version/sqlite-jdbc-$version.jar",
            "org.sqlite.JDBC",
            "sqlite.jar"
        )

        val path = Paths.get(file).absolute()
        val parentDir = path.parent
        if (parentDir.notExists()) parentDir.createDirectories()
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:$file")
        } catch (e: SQLException) {
            Logger.fatal("Unable to connect to SQLite database", e)
        }
    }

    private fun createDatabaseTables() {
        val tableSql = """
            | -- Players Table
            | CREATE TABLE IF NOT EXISTS `players`
            | (
            |     `id`              INTEGER
            |         CONSTRAINT players_pk
            |             PRIMARY KEY AUTOINCREMENT,
            |     `display_name`    TEXT NOT NULL,
            |     `session_token`   TEXT    DEFAULT NULL,
            |     `password`        TEXT,
            |     `credits`         INTEGER DEFAULT 0,
            |     `credits_sepnt`   INTEGER DEFAULT 0,
            |     `games_played`    INTEGER DEFAULT 0,
            |     `seconds_played`  INTEGER DEFAULT 0,
            |     `inventory`       TEXT,
            |     `csreward`        INTEGER DEFAULT 0,
            |     `face_codes`      TEXT    DEFAULT '20;',
            |     `new_item`        TEXT    DEFAULT '20;4;',
            |     `completion`      TEXT    DEFAULT NULL,
            |     `progress`        TEXT    DEFAULT NULL,
            |     `cs_completion`   TEXT    DEFAULT NULL,
            |     `cs_timestamps_1` TEXT    DEFAULT NULL,
            |     `cs_timestamps_2` TEXT    DEFAULT NULL,
            |     `cs_timestamps_3` TEXT    DEFAULT NULL
            | );
            | 
            | -- Player Classes Table
            | CREATE TABLE IF NOT EXISTS `player_classes`
            | (
            |     `id`         INTEGER
            |         CONSTRAINT player_classes_pk
            |             PRIMARY KEY AUTOINCREMENT,
            |     `player_id`  INTEGER
            |         constraint player_classes_players_id_fk
            |             references players (`id`) NOT NULL,
            |     `index`      INTEGER              NOT NULL,
            |     `name`       TEXT                 NOT NULL,
            |     `level`      INTEGER              NOT NULL,
            |     `exp`        REAL                 NOT NULL,
            |     `promotions` INTEGER              NOT NULL
            | );
            | 
            | -- Player Characters Table
            | CREATE TABLE IF NOT EXISTS `player_characters`
            | (
            |     `id`                INTEGER
            |         CONSTRAINT player_characters_pk
            |             PRIMARY KEY AUTOINCREMENT,
            |     `player_id`         INTEGER
            |         CONSTRAINT player_characters_players_id_fk
            |             REFERENCES players (`id`) NOT NULL,
            |     `index`             INTEGER       NOT NULL,
            |     `kit_name`          TEXT          NOT NULL,
            |     `name`              TEXT          NOT NULL,
            |     `tint1`             INTEGER       NOT NULL,
            |     `tint2`             INTEGER       NOT NULL,
            |     `pattern`           INTEGER       NOT NULL,
            |     `patternColor`      INTEGER       NOT NULL,
            |     `phong`             INTEGER       NOT NULL,
            |     `emissive`          INTEGER       NOT NULL,
            |     `skin_tone`         INTEGER       NOT NULL,
            |     `seconds_played`    INTEGER       NOT NULL,
            | 
            |     `timestamp_year`    INTEGER       NOT NULL,
            |     `timestamp_month`   INTEGER       NOT NULL,
            |     `timestamp_day`     INTEGER       NOT NULL,
            |     `timestamp_seconds` INTEGER       NOT NULL,
            | 
            |     `powers`            TEXT          NOT NULL,
            |     `hotkeys`           TEXT          NOT NULL,
            |     `weapons`           TEXT          NOT NULL,
            |     `weapon_mods`       TEXT          NOT NULL,
            | 
            |     `deployed`          INTEGER       NOT NULL,
            |     `leveled_up`        INTEGER       NOT NULL
            | );
            | 
            | -- Galaxy At War Table
            | CREATE TABLE IF NOT EXISTS `player_gaw`
            | (
            |     `id`            INTEGER
            |         CONSTRAINT player_classes_pk
            |             PRIMARY KEY AUTOINCREMENT,
            |     `player_id`     INTEGER
            |         constraint player_classes_players_id_fk
            |             references players (`id`) NOT NULL,
            |     `last_modified` INTEGER           NOT NULL,
            |     `group_a`       INTEGER           NOT NULL,
            |     `group_b`       INTEGER           NOT NULL,
            |     `group_c`       INTEGER           NOT NULL,
            |     `group_d`       INTEGER           NOT NULL,
            |     `group_e`       INTEGER           NOT NULL
            | );
        """.trimMargin()
        try {
            val statement = connection.createStatement()
            statement.executeUpdate(tableSql)
        } catch (e: SQLException) {
            Logger.fatal("Failed to create database tables", e)
        }
    }

    override fun setup() {
        createDatabaseTables()
    }

    override fun isPlayerEmailTaken(email: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getPlayerById(id: Int): Player? {
        TODO("Not yet implemented")
    }

    override fun getPlayerByEmail(email: String): Player? {
        TODO("Not yet implemented")
    }

    override fun getPlayerBySessionToken(sessionToken: String): Player? {
        TODO("Not yet implemented")
    }

    override fun getPlayerClasses(player: Player): MutableList<PlayerClass> {
        TODO("Not yet implemented")
    }

    override fun getPlayerCharacters(player: Player): MutableList<PlayerCharacter> {
        TODO("Not yet implemented")
    }

    override fun getGalaxyAtWarData(player: Player): GalaxyAtWarData? {
        TODO("Not yet implemented")
    }

    override fun createPlayer(email: String, hashedPassword: String): Player {
        TODO("Not yet implemented")
    }

    override fun updatePlayerFully(player: Player) {
        TODO("Not yet implemented")
    }

    override fun setPlayerClass(player: Player, playerClass: PlayerClass) {
        TODO("Not yet implemented")
    }

    override fun setPlayerCharacter(player: Player, playerCharacter: PlayerCharacter) {
        TODO("Not yet implemented")
    }

    override fun setPlayerSessionToken(player: Player, sessionToken: String) {
        TODO("Not yet implemented")
    }

    override fun setUpdatedPlayerData(player: Player, key: String) {
        TODO("Not yet implemented")
    }

    override fun setGalaxyAtWarData(player: Player, galaxyAtWarData: GalaxyAtWarData) {
        TODO("Not yet implemented")
    }


}