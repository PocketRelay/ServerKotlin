package com.jacobtread.kme.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * Represents the table which stores the settings for each player.
 * This will contain the settings which are un-documented or just
 * unknown in function so they key a unique column to identify them
 */
object PlayerSettingsTable : IntIdTable("player_settings") {
    val player = reference("player_id", PlayersTable)

    val key = varchar("key", length = 32)
    val value = text("value")
}