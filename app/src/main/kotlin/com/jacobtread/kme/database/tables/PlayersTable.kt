package com.jacobtread.kme.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * Represents the table which contains the data for each player
 * account. This table is references by other tables in order
 * to store data relating to each player.
 */
object PlayersTable : IntIdTable("players") {

    val email = varchar("email", length = 254)
    val displayName = varchar("display_name", length = 99)
    val sessionToken = varchar("session_token", length = 128)
        .nullable()
        .default(null)
    val password = varchar("password", length = 128)

    val credits = integer("credits").default(0)
    val creditsSpent = integer("credits_spent").default(0)
    val gamesPlayed = integer("games_played").default(0)
    val secondsPlayed = long("seconds_played").default(0L)
    val inventory = text("inventory").default("")
}

