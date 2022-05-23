package com.jacobtread.kme.blaze

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel

const val INCOMING = 0x0000 // This packet is coming from the client
const val RESPONSE = 0x1000 // This packet is a response to an existing packet
const val UNIQUE = 0x2000 // This packet is not responding to a packet
const val ERROR = 0x3000 // This packet contains an error

const val NO_ERROR = 0

@Suppress("NOTHING_TO_INLINE")
inline fun Channel.send(packet: Packet, flush: Boolean = true) {
    write(packet)
    if (flush) flush()
}

inline fun Channel.respond(
    responding: Packet,
    error: Int = NO_ERROR,
    flush: Boolean = true,
    populate: TdfBuilder.() -> Unit = {},
) = send(createPacket(responding.component, responding.command, RESPONSE, responding.id, error, populate), flush)

@Suppress("NOTHING_TO_INLINE")
inline fun Channel.respond(
    responding: Packet,
    content: ByteBuf,
    flush: Boolean = true,
    error: Int = NO_ERROR,
) = send(Packet(responding.component, responding.command, error, RESPONSE, responding.id, content), flush)

inline fun Channel.error(
    responding: Packet,
    error: Int,
    flush: Boolean = true,
    populate: TdfBuilder.() -> Unit = {},
) = send(createPacket(responding.component, responding.command, ERROR, responding.id, error, populate), flush)

inline fun error(
    responding: Packet,
    error: Int,
    populate: TdfBuilder.() -> Unit = {},
) = createPacket(responding.component, responding.command, ERROR, responding.id, error, populate)

inline fun respond(
    responding: Packet,
    error: Int = NO_ERROR,
    populate: TdfBuilder.() -> Unit = {},
): Packet = createPacket(
    responding.component,
    responding.command,
    RESPONSE,
    responding.id,
    error,
    populate
)


@Suppress("NOTHING_TO_INLINE")
inline fun respond(
    responding: Packet,
    content: ByteBuf,
    error: Int = NO_ERROR,
) = Packet(responding.component, responding.command, error, RESPONSE, responding.id, content)


inline fun Channel.unique(
    component: Int,
    command: Int,
    id: Int = 0x0,
    error: Int = NO_ERROR,
    flush: Boolean = true,
    populate: TdfBuilder.() -> Unit = {},
) = send(createPacket(component, command, UNIQUE, id, error, populate), flush)

inline fun unique(
    component: Int,
    command: Int,
    id: Int = 0x0,
    error: Int = NO_ERROR,
    populate: TdfBuilder.() -> Unit = {},
): Packet = createPacket(component, command, UNIQUE, id, error, populate)

inline fun Channel.packet(
    component: Int,
    command: Int,
    qtype: Int,
    id: Int = 0x0,
    error: Int = NO_ERROR,
    flush: Boolean = true,
    populate: TdfBuilder.() -> Unit = {},
) = send(createPacket(component, command, qtype, id, error, populate), flush)

inline fun createPacket(
    component: Int,
    command: Int,
    qtype: Int,
    id: Int = 0x0,
    error: Int = NO_ERROR,
    populate: TdfBuilder.() -> Unit = {},
): Packet {
    val contentBuilder = TdfBuilder()
    contentBuilder.populate()
    return Packet(
        component,
        command,
        error,
        qtype,
        id,
        contentBuilder.createBuffer()
    )
}

inline fun lazyPacketBody(crossinline populate: TdfBuilder.() -> Unit): Lazy<ByteBuf> {
    return lazy {
        val builder = TdfBuilder()
        builder.populate()
        Unpooled.unreleasableBuffer(builder.createBuffer())
    }
}

