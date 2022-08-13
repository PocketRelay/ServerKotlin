package com.jacobtread.kme.sessions.handlers

import com.jacobtread.blaze.annotations.PacketHandler
import com.jacobtread.blaze.group
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.respond
import com.jacobtread.blaze.tdf.types.GroupTdf
import com.jacobtread.blaze.text
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.sessions.Session

/**
 * getLocaleName Translates the provided locale name
 * to the user readable name
 *
 * @param code The shorthand code for the locale name
 * @return The human-readable locale name
 */
private fun getLocaleName(code: String): String = when (code.lowercase()) {
    "global" -> "Global"
    "de" -> "Germany"
    "en" -> "English"
    "es" -> "Spain"
    "fr" -> "France"
    "it" -> "Italy"
    "ja" -> "Japan"
    "pl" -> "Poland"
    "ru" -> "Russia"
    else -> code
}

/**
 * Handle leaderboard group
 *
 * TODO: NOT IMPLEMENTED PROPERLY
 *
 * @param packet
 */
@PacketHandler(Components.STATS, Commands.GET_LEADERBOARD_GROUP)
fun Session.handleLeaderboardGroup(packet: Packet) {
    val name: String = packet.text("NAME")
    val isN7 = name.startsWith("N7Rating")
    if (isN7 || name.startsWith("ChallengePoints")) {
        val locale: String = name.substring(if (isN7) 8 else 15)
        val localeName = getLocaleName(locale)
        val desc: String
        val sname: String
        val sdsc: String
        val gname: String
        if (isN7) {
            desc = "N7 Rating - $localeName"
            sname = "n7rating"
            sdsc = "N7 Rating"
            gname = "ME3LeaderboardGroup"
        } else {
            desc = "Challenge Points - $localeName"
            sname = "ChallengePoints"
            sdsc = "Challenge Points"
            gname = "ME3ChallengePoints"
        }
        push(packet.respond {
            number("ACSD", 0x0)
            text("BNAM", name)
            text("DESC", desc)
            pair("ETYP", 0x7802, 0x1)
            map("KSUM", mapOf(
                "accountcountry" to group {
                    map("KSVL", mapOf(0x0 to 0x0))
                }
            ))
            number("LBSZ", 0x7270e0)
            list("LIST", listOf(
                group {
                    text("CATG", "MassEffectStats")
                    text("DFLT", "0")
                    number("DRVD", 0x0)
                    text("FRMT", "%d")
                    text("KIND", "")
                    text("LDSC", sdsc)
                    text("META", "W=200, HMC=tableColHeader3, REMC=tableRowEntry3")
                    text("NAME", sname)
                    text("SDSC", sdsc)
                    number("TYPE", 0x0)
                }
            ))
            text("META", "RF=@W=150, HMC=tableColHeader1, REMC=tableRowEntry1@ UF=@W=670, HMC=tableColHeader2, REMC=tableRowEntry2@")
            text("NAME", gname)
            text("SNAM", sname)
        })
    } else {
        push(packet.respond())
    }
}

/**
 * Handles a filtered leaderboard
 *
 * TODO: Currently not implemented
 *
 * @param packet
 */
@PacketHandler(Components.STATS, Commands.GET_FILTERED_LEADERBOARD)
fun Session.handleFilteredLeaderboard(packet: Packet) {
    push(packet.respond {
        list("LDLS", emptyList<GroupTdf>())
    })
}

/**
 * Handles retrieving the number of entities on the leaderboard
 *
 * TODO: Currently not implemented
 *
 * @param packet
 */
@PacketHandler(Components.STATS, Commands.GET_LEADERBOARD_ENTITY_COUNT)
fun Session.handleLeaderboardEntityCount(packet: Packet) {
    val entityCount = 1 // The number of leaderboard entities
    push(packet.respond { number("CNT", entityCount) })
}

/**
 * Handles retrieving the contents of the centered leaderboard
 *
 * TODO: Currently not implemented
 *
 * @param packet
 */
@PacketHandler(Components.STATS, Commands.GET_CENTERED_LEADERBOARD)
fun Session.handleCenteredLeadboard(packet: Packet) {
    // TODO: Currenlty not implemented
    push(packet.respond {
        list("LDLS", emptyList<GroupTdf>())
    })
}