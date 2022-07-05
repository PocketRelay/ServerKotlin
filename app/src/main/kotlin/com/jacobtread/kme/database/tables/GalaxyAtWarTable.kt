package com.jacobtread.kme.database.tables

import com.jacobtread.kme.tools.unixTimeSeconds
import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * Represents thet able containing the Galaxy At War statistics
 * for each player.
 *
 * @constructor Create empty Galaxy at war table
 */
object GalaxyAtWarTable : IntIdTable("player_gaw") {

    /**
     * References the player that the galaxy at war entity
     * is a reference to
     */
    val player = reference("player_id", PlayersTable)

    /**
     * Represents the unix time in seconds that the galaxy at war
     * statistic was last modified. This is used to cacluale the
     * amount that it should have decayed by since
     */
    val lastModified = long("last_modified")
        .clientDefault { unixTimeSeconds() }

    val groupA = integer("group_a")
        .default(5000)
    val groupB = integer("group_b")
        .default(5000)
    val groupC = integer("group_c")
        .default(5000)
    val groupD = integer("group_d")
        .default(5000)
    val groupE = integer("group_e")
        .default(5000)
}