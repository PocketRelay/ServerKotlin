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
     * General:
     *
     * - 100: Mass Effect   MEMstr.dds
     * - 104: Squad Elite
     * - 105: Spectre Master
     * - 106: Solo Mastery
     *
     * - 125: N7 Mastery
     * - 126: Map Mastery
     * - 127: Biotic Mastery
     * - 128: Tech Mastery
     *
     * Aliens:
     * - 112: Resurgence Mastery
     * - 113: Rebellion Mastery
     * - 114: Earth Mastery
     * - 115: Retaliation Mastery
     * - 121: Blood Pack Mastery
     * - 122: Commando Mastery
     * - 123: Machine Mastery
     * - 124: Outsider Mastery
     * - 206: Reckoning Mastery
     *
     * Weapons:
     *
     * - 107: Shotgun Mastery
     * - 108: Assault Rifle Mastery
     * - 109: Pistol Mastery
     * - 110: SMG Mastery
     * - 111: Sniper Rifle Mastery
     * - 116: Combat Mastery
     * - 117: Ceberus Mastery
     * - 118: Reaper Mastery
     * - 119: Geth Mastery
     * - 120: Collector Mastery
     */
    val challengeReward = integer("csreward")
        .default(0)
    val faceCodes = text("faces_codes")
        .default("20;")

    val newItem = text("new_item")
        .default("20;4;")

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

