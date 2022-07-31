package com.jacobtread.kme.database.adapter.sql

import com.jacobtread.kme.database.RuntimeDriver
import com.jacobtread.kme.utils.logging.Logger
import java.sql.SQLException

class SQLiteDatabaseAdapter(file: String) : SQLDatabaseAdapter(RuntimeDriver.createSQLiteConnection(file)) {

    /**
     * The setup function for the SQLite database is only
     * slightly different to MySQL the main differences are
     * the datatypes in the table creation and the query being
     * one large query.
     *
     * The MySQL driver/MySQl doesn't support the large bulk
     */
    override fun setup() {
        try {
            val statement = connection.createStatement()
            statement.executeUpdate(
                """
                -- Players Table
                CREATE TABLE IF NOT EXISTS `players`
                (
                    `id`              INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    `email`           TEXT    NOT NULL,
                    `display_name`    TEXT    NOT NULL,
                    `session_token`   TEXT    DEFAULT NULL,
                    `origin`          INTEGER NOT NULL,
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
            )
            statement.close()
        } catch (e: SQLException) {
            Logger.fatal("Failed to create database tables", e)
        }
    }
}
