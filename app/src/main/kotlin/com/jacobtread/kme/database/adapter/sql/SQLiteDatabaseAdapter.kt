package com.jacobtread.kme.database.adapter.sql

import com.jacobtread.kme.database.RuntimeDriver
import com.jacobtread.kme.utils.logging.Logger
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

class SQLiteDatabaseAdapter(file: String) : SQLDatabaseAdapter(createConnection(file)) {

    companion object {
        fun createConnection(file: String): Connection {
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
                return DriverManager.getConnection("jdbc:sqlite:$file")
            } catch (e: SQLException) {
                Logger.fatal("Unable to connect to SQLite database", e)
            }
        }
    }

    override fun getTableSql(): String = """
        -- Players Table
        CREATE TABLE IF NOT EXISTS `players`
        (
            `id`              INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            `email`           TEXT    NOT NULL,
            `display_name`    TEXT    NOT NULL,
            `session_token`   TEXT    DEFAULT NULL,
            `password`        TEXT    NOT NULL,
            `credits`         INT     DEFAULT 0,
            `credits_spent`   INTEGER DEFAULT 0,
            `games_played`    INTEGER DEFAULT 0,
            `seconds_played`  INTEGER DEFAULT 0,
            `inventory`       TEXT    DEFAULT '',
            `csreward`        INTEGER DEFAULT 0,
            `face_codes`      TEXT    DEFAULT '20;',
            `new_item`        TEXT    DEFAULT '20;4;',
            `completion`      TEXT    DEFAULT NULL,
            `progress`        TEXT    DEFAULT NULL,
            `cs_completion`   TEXT    DEFAULT NULL,
            `cs_timestamps_1` TEXT    DEFAULT NULL,
            `cs_timestamps_2` TEXT    DEFAULT NULL,
            `cs_timestamps_3` TEXT    DEFAULT NULL
        );

        -- Player Classes Table
        CREATE TABLE IF NOT EXISTS `player_classes`
        (
            `id`         INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            `player_id`  INTEGER NOT NULL,
            `index`      INTEGER NOT NULL,
            `name`       TEXT    NOT NULL,
            `level`      INTEGER NOT NULL,
            `exp`        REAL    NOT NULL,
            `promotions` INTEGER NOT NULL,

            FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
        );

        -- Player Characters Table
        CREATE TABLE IF NOT EXISTS `player_characters`
        (
            `id`                INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            `player_id`         INTEGER NOT NULL,
            `index`             INTEGER NOT NULL,
            `kit_name`          TEXT    NOT NULL,
            `name`              TEXT    NOT NULL,
            `tint1`             INTEGER NOT NULL,
            `tint2`             INTEGER NOT NULL,
            `pattern`           INTEGER NOT NULL,
            `pattern_color`     INTEGER NOT NULL,
            `phong`             INTEGER NOT NULL,
            `emissive`          INTEGER NOT NULL,
            `skin_tone`         INTEGER NOT NULL,
            `seconds_played`    INTEGER NOT NULL,

            `timestamp_year`    INTEGER NOT NULL,
            `timestamp_month`   INTEGER NOT NULL,
            `timestamp_day`     INTEGER NOT NULL,
            `timestamp_seconds` INTEGER NOT NULL,

            `powers`            TEXT    NOT NULL,
            `hotkeys`           TEXT    NOT NULL,
            `weapons`           TEXT    NOT NULL,
            `weapon_mods`       TEXT    NOT NULL,

            `deployed`          INTEGER NOT NULL,
            `leveled_up`        INTEGER NOT NULL,

            FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
        );

        -- Galaxy At War Table
        CREATE TABLE IF NOT EXISTS `player_gaw`
        (
            `id`            INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            `player_id`     INTEGER NOT NULL,
            `last_modified` INTEGER NOT NULL,
            `group_a`       INTEGER NOT NULL,
            `group_b`       INTEGER NOT NULL,
            `group_c`       INTEGER NOT NULL,
            `group_d`       INTEGER NOT NULL,
            `group_e`       INTEGER NOT NULL,

            FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
        );
    """.trimIndent()
}