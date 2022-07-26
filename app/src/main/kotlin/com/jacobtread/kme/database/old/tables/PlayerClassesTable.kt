package com.jacobtread.kme.database.old.tables

import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * PlayerClasses Stores the class information for each class
 * that the player owns. This is created by the game rather
 * than the server
 *
 * @constructor Create empty PlayerClasses
 */
object PlayerClassesTable : IntIdTable("player_classes") {
    val player = reference("player_id", PlayersTable)
    val index = integer("index")
    val name = varchar("name", length = 18)
    val level = integer("level")
    val exp = float("exp")
    val promotions = integer("promotions")
}