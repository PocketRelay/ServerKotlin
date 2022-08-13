package com.jacobtread.kme.sessions.handlers

import com.jacobtread.blaze.annotations.PacketHandler
import com.jacobtread.blaze.group
import com.jacobtread.blaze.notify
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.respond
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.sessions.Session

/**
 * Needs further investigation for proper documentation.
 *
 * @param packet
 */
@PacketHandler(Components.ASSOCIATION_LISTS, Commands.GET_LISTS)
fun Session.handleAssociationListGetLists(packet: Packet) {
    push(packet.respond {
        list("LMAP", listOf(
            group {
                +group("INFO") {
                    tripple("BOID", 0x19, 0x1, 0x74b09c4)
                    number("FLGS", 4)
                    +group("LID") {
                        text("LNM", "friendList")
                        number("TYPE", 1)
                    }
                    number("LMS", 0xC8)
                    number("PRID", 0)
                }
                number("OFRC", 0)
                number("TOCT", 0)
            }
        ))
    })
}

/**
 * Handles the submission of an offline game report.
 *
 * Needs further investigation for proper documentation.
 *
 * @param packet The game reporting packet
 */
@PacketHandler(Components.GAME_REPORTING, Commands.SUBMIT_OFFLINE_GAME_REPORT)
fun Session.handleSubmitOfflineReport(packet: Packet) {
    push(packet.respond())
    push(notify(Components.GAME_REPORTING, Commands.NOTIFY_GAME_REPORT_SUBMITTED) {
        varList("DATA")
        number("EROR", 0)
        number("FNL", 0)
        number("GHID", 0)
        number("GRID", 0) // Game Report ID
    })
}