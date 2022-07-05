package com.jacobtread.kme.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * Players table This table contains the data for each player account.
 * This table is referenced by other data relating to this player
 *
 * @constructor Create empty Players table
 */
object PlayersTable : IntIdTable("players") {
    val email = varchar("email", length = 254)
    val displayName = varchar("display_name", length = 99)
    val sessionToken = varchar("session_token", length = 128)
        .nullable()
        .default(null)
    val password = varchar("password", length = 128)

    /**
     * Settings base Encoded base settings object for the player
     * this is in the format
     */
    val settingsBase = text("settings_base")
        .nullable()
        .default(null)
}

