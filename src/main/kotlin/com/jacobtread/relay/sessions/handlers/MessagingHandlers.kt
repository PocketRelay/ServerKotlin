package com.jacobtread.relay.sessions.handlers

import com.jacobtread.blaze.annotations.PacketHandler
import com.jacobtread.blaze.group
import com.jacobtread.blaze.notify
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.respond
import com.jacobtread.relay.Environment
import com.jacobtread.relay.data.Constants
import com.jacobtread.relay.data.blaze.Commands
import com.jacobtread.relay.data.blaze.Components
import com.jacobtread.relay.sessions.Session
import com.jacobtread.relay.utils.unixTimeSeconds

/**
 * Handles sending messages to the client when the client
 * requests them. Currently, this only includes the main menu
 * message. But once further investigation is complete I hope
 * to have this include all messaging types.
 *
 * TODO: Investigate sending of other message types
 *
 * @param packet The packet requesting messages
 */
@PacketHandler(Components.MESSAGING, Commands.FETCH_MESSAGES)
fun Session.handleFetchMessages(packet: Packet) {
    val playerEntity = player

    if (playerEntity == null) { // If not authenticate display no messages
        push(packet.respond { number("MCNT", 0) })
        return
    }

    val menuMessage = Environment.menuMessage
        .replace("{v}", Constants.RELAY_VERSION)
        .replace("{n}", playerEntity.displayName)
        .replace("{ip}", getAddressString()) + 0xA.toChar()

    pushAll(
        packet.respond { number("MCNT", 1) }, // Number of messages

        notify(Components.MESSAGING, Commands.SEND_MESSAGE) {
            number("FLAG", 0x1)
            number("MGID", 0x1)
            text("NAME", menuMessage)
            +group("PYLD") {
                map("ATTR", mapOf("B0000" to "160"))
                // Flag types
                // 0x1 = Default message
                // 0x2 = Unlocked acomplishment

                number("FLAG", 0x1)
                number("STAT", 0x0)
                number("TAG", 0x0)
                tripple(
                    "TARG",
                    Components.USER_SESSIONS,
                    Commands.SET_SESSION,
                    playerEntity.playerId
                )
                number("TYPE", 0x0)
            }
            tripple(
                "SRCE",
                Components.USER_SESSIONS,
                Commands.SET_SESSION,
                playerEntity.playerId
            )
            number("TIME", unixTimeSeconds())
        }
    )
}