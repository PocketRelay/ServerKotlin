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

    /**
     * Challenge reward banner
     *
     * 100: MEMstr.dds
     *
     */
    val challengeReward = integer("csreward")
        .default(0)
    val faceCodes = text("faces_codes")
        .nullable()
        .default(null)

    val newItem = text("new_item")
        .nullable()
        .default(null)

    val completion = text("completion")
        .nullable()
        .default(null)

    val progress = text("progress")
        .nullable()
        .default(null)

    val challengeCompletion = text("cs_completion")
        .nullable()
        .default(null)

    val cstimestamps1 = text("cs_timestamps_1")
        .nullable()
        .default(null)

    val cstimestamps2 = text("cs_timestamps_2")
        .nullable()
        .default(null)

    val cstimestamps3 = text("cs_timestamps_3")
        .nullable()
        .default(null)
}

