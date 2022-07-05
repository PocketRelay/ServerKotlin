package com.jacobtread.kme.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object PlayerGalaxyAtWarsTable : IntIdTable("player_gaw") {
    val player = reference("player_id", PlayersTable)

    val timestamp = long("timestamp")

    val a = integer("a").default(5000)
    val b = integer("b").default(5000)
    val c = integer("c").default(5000)
    val d = integer("d").default(5000)
    val e = integer("e").default(5000)

}