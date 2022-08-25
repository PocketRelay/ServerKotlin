package com.jacobtread.relay.sessions.handlers

import com.jacobtread.blaze.*
import com.jacobtread.blaze.annotations.PacketHandler
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.tdf.types.GroupTdf
import com.jacobtread.relay.blaze.Commands
import com.jacobtread.relay.blaze.Components
import com.jacobtread.relay.blaze.LoginError
import com.jacobtread.relay.database.tables.PlayersTable
import com.jacobtread.relay.sessions.Session
import com.jacobtread.relay.utils.logging.Logger

/**
 * Handles resuming a session that was present on a previous run or
 * that was logged out. This is done using the session key that was
 * provided to that session upon authenticating. The session key
 * provided by this packet is looked up in the database and if a
 * player is found with a matching one they become authenticated
 *
 * @param packet The packet requesting the session resumption
 */
@PacketHandler(Components.USER_SESSIONS, Commands.RESUME_SESSION)
fun Session.handleResumeSession(packet: Packet) {
    val sessionToken = packet.text("SKEY")
    PlayersTable.getBySessionToken(sessionToken)
        .thenApplyAsync { player ->
            if (player == null) {
                push(LoginError.INVALID_INFORMATION(packet))
            } else {
                setAuthenticatedPlayer(player)
            }
        }
        .exceptionallyAsync { Logger.warn("Failed to resume session", it) }
        .whenCompleteAsync { _, _ -> push(packet.respond()) }
}

/**
 * The packet recieved from the client contains networking information
 * including the external and internal ip addresses and ports along with
 * the natt type. All of this information is stored. This handler responds
 * with a set session packet to update the clients view of its session
 *
 * @param packet The packet containing the network update information
 */
@PacketHandler(Components.USER_SESSIONS, Commands.UPDATE_NETWORK_INFO)
fun Session.updateNetworkInfo(packet: Packet) {
    val addr = packet.optionalValue("ADDR") as GroupTdf
    val nqos = packet.group("NQOS")
    val dbps = nqos.ulong("DBPS")
    val nattType = nqos.int("NATT")
    val ubps = nqos.ulong("UBPS")
    val nlmp = packet.map<String, ULong>("NLMP")
    val pslm = nlmp.getOrDefault("ea-sjc", 0xfff0fffu)
    push(packet.respond())
    setNetworkingData(addr, dbps, nattType, ubps, pslm)
}

/**
 * Handles updating the hardware flag using the value provided by the
 * client using this packet. This handler responds with a set session
 * packet to update the clients view of its session
 *
 * @param packet The packet containing the hardware flag
 */
@PacketHandler(Components.USER_SESSIONS, Commands.UPDATE_HARDWARE_FLAGS)
fun Session.updateHardwareFlag(packet: Packet) {
    val hardwareFlag = packet.int("HWFG")
    push(packet.respond())
    setHardwareFlag(hardwareFlag)
}
