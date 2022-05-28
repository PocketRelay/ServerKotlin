@file:Suppress("NOTHING_TO_INLINE")

package com.jacobtread.kme.blaze

import com.jacobtread.kme.blaze.Packet.Companion.ERROR_TYPE
import com.jacobtread.kme.blaze.Packet.Companion.NO_ERROR
import com.jacobtread.kme.blaze.Packet.Companion.RESPONSE_TYPE
import com.jacobtread.kme.blaze.Packet.Companion.UNIQUE_TYPE
import io.netty.buffer.Unpooled

/**
 * Alias for a function that initializes the contents of a TdfBuilder
 * @see TdfBuilder
 */
typealias ContentInitializer = TdfBuilder.() -> Unit

/**
 * respond Creates a packet responding to the packet that this was called on.
 * The packet will be of the RESPONSE type and will have its contents initialized
 * within the provided function. INLINED
 *
 * @param init The initializer for the content of this packet
 * @return The created packet
 */
inline fun Packet.respond(init: ContentInitializer): Packet = initializePacket(component, command, NO_ERROR, RESPONSE_TYPE, id, init)

/**
 * respond Creates a packet responding to the packet that this was called on.
 * The packet will be of the RESPONSE type this packet will have empty content
 *
 * @return The created packet
 */
inline fun Packet.respond(): Packet = initializeEmptyPacket(component, command, NO_ERROR, RESPONSE_TYPE, id)

/**
 * unique Creates a packet that is not responding to any other packets
 * using the provided command and component, initializes the contents
 * using the provided content initializer
 *
 * @param component The component of the packet
 * @param command The command of the packet
 * @param init The content initializer
 * @return The created packet
 */
fun unique(
    component: Int,
    command: Int,
    init: ContentInitializer,
): Packet = initializePacket(component, command, NO_ERROR, UNIQUE_TYPE, 0x0, init)

/**
 * unique Creates a packet that is not responding to any other packets
 * using the provided command and component
 *
 * @param component The component of the packet
 * @param command The command of the packet
 * @return The created packet
 */
fun unique(component: Int, command: Int): Packet = initializeEmptyPacket(component, command, NO_ERROR, UNIQUE_TYPE, 0x0)


/**
 * error Creates a packet responding to the packet that this was called on. This packet
 * contains an error as well. The packet will be of the ERROR type and will have its
 * contents initialized within the provided function. INLINED
 *
 * @param init The initializer for the content of this packet
 * @return The created packet
 */
inline fun Packet.error(error: Int, init: ContentInitializer): Packet = initializePacket(component, command, error, ERROR_TYPE, id, init)

/**
 * error Creates a packet responding to the packet that this was called on. This packet
 * contains an error as well. The packet will be of the ERROR type and will have no content
 *
 * @return The created packet
 */
inline fun Packet.error(error: Int): Packet = initializeEmptyPacket(component, command, error, ERROR_TYPE, id)


/**
 * initializePacket Initializes the contents of and then creates a new packet
 * with the provided packet details. Creates the packet contents with the init
 * function that was provided.
 *
 * @param component The packet component
 * @param command The packet command
 * @param error The packet error
 * @param type The packet type
 * @param id The packet id
 * @param init The content initializer
 * @return The created packet
 */
inline fun initializePacket(
    component: Int,
    command: Int,
    error: Int,
    type: Int,
    id: Int,
    init: ContentInitializer,
): Packet {
    val builder = TdfBuilder()
    builder.init()
    return Packet(component, command, error, type, id, builder.createBuffer())
}

/**
 * initializeEmptyPacket Creates a packet with the provided details
 * but with empty contents.
 *
 * @param component The packet component
 * @param command The packet command
 * @param error The packet error
 * @param type The packet type
 * @param id The packet id
 * @return
 */
inline fun initializeEmptyPacket(
    component: Int,
    command: Int,
    error: Int,
    type: Int,
    id: Int,
): Packet = Packet(component, command, error, type, id, Unpooled.EMPTY_BUFFER)