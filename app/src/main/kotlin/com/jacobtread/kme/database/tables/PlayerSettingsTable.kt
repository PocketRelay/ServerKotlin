package com.jacobtread.kme.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * PlayerSettings
 *
 * @constructor Create empty PlayerSettings
 */
object PlayerSettingsTable : IntIdTable("player_settings") {
    val player = reference("player_id", PlayersTable)

    val key = varchar("key", length = 32)
    val value = text("value")
}