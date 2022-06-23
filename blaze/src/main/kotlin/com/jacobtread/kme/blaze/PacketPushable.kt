@file:Suppress("NOTHING_TO_INLINE")

package com.jacobtread.kme.blaze

/**
 * PacketPushable Represents a class that can have packets
 * sent to it. This is implemented on sessions as well as
 * the networking handler so that packets can be easily sent
 *
 * @constructor Create empty PacketPushable
 */
interface PacketPushable {

    /**
     * push a new packet out to the client
     *
     * @param packet The packet to push
     */
    fun push(packet: Packet)

    fun pushAll(vararg packets: Packet) {
        packets.forEach { push(it) }
    }
}

inline fun PacketPushable.pushResponse(packet: Packet, init: ContentInitializer) = push(packet.respond(init))
inline fun PacketPushable.pushEmptyResponse(packet: Packet) = push(packet.respond())

inline fun PacketPushable.pushUnique(
    component: Int,
    command: Int,
    init: ContentInitializer,
) = push(unique(component, command, init))

inline fun PacketPushable.pushEmptyUnique(
    component: Int,
    command: Int,
) = push(unique(component, command))

inline fun PacketPushable.pushError(
    packet: Packet, error: Int, init: ContentInitializer,
) = push(packet.error(error, init))

inline fun PacketPushable.pushEmptyError(
    packet: Packet, error: Int,
) = push(packet.error(error))

