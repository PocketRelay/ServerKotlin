package com.jacobtread.kme.database.repos

import com.jacobtread.kme.LOGGER
import java.sql.Connection
import java.sql.SQLException

class PlayersRepository(connection: Connection) : DatabaseRepository(connection) {

    override fun init() {
        try {
            val statement = connection.createStatement()
            statement.execute("""
                CREATE TABLE `players` (
                    `id` INT(255) AUTO_INCREMENT,
                    `email` VARCHAR(254),
                    `display_name` VARCHAR(99),
                    `credits` INT(10) unsigned,
                    `games_played` INT(10) unsigned DEFAULT 0,
                    `auth` VARCHAR(128) DEFAULT NULL,
                    `auth2` VARCHAR(128) DEFAULT NULL,
                    UNIQUE KEY `id_index` (`id`) USING BTREE,
                    UNIQUE KEY `name_index` (`display_name`) USING BTREE,
                    PRIMARY KEY (`id`)
                );
            """.trimIndent())
            LOGGER.info("Created players database table")
        } catch (e: SQLException) {
            LOGGER.fatal("Failed to create players database table", e)
        }
    }

    fun createPlayer(email: String, displayName: String, password: String): Boolean {
        val statement = connection.prepareStatement("INSERT INTO `players` (`email`, `display_name`, `auth2`) VALUES (?, ?, ?)")
        statement.setString(1, email)
        statement.setString(2, displayName)

        return true
    }

}