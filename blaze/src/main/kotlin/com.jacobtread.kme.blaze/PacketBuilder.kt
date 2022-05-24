@file:Suppress("NOTHING_TO_INLINE")

package com.jacobtread.kme.blaze


import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel

inline fun Channel.send(packet: Packet, flush: Boolean = true) {
    write(packet)
    if (flush) flush()
}

inline fun Channel.respond(
    responding: Packet,
    error: Int = Packet.NO_ERROR,
    flush: Boolean = true,
    populate: TdfBuilder.() -> Unit = {},
) = send(createPacket(responding.component, responding.command, Packet.RESPONSE_TYPE, responding.id, error, populate), flush)

inline fun Channel.respond(
    responding: Packet,
    content: ByteBuf,
    flush: Boolean = true,
    error: Int = Packet.NO_ERROR,
) = send(Packet(responding.component, responding.command, error, Packet.RESPONSE_TYPE, responding.id, content), flush)

inline fun Channel.error(
    responding: Packet,
    error: Int,
    flush: Boolean = true,
    populate: TdfBuilder.() -> Unit = {},
) = send(createPacket(responding.component, responding.command, Packet.ERROR_TYPE, responding.id, error, populate), flush)

inline fun error(
    responding: Packet,
    error: Int,
    populate: TdfBuilder.() -> Unit = {},
): Packet = createPacket(responding.component, responding.command, Packet.ERROR_TYPE, responding.id, error, populate)

inline fun respond(
    responding: Packet,
    error: Int = Packet.NO_ERROR,
    populate: TdfBuilder.() -> Unit = {},
): Packet = createPacket(
    responding.component,
    responding.command,
    Packet.RESPONSE_TYPE,
    responding.id,
    error,
    populate
)


inline fun respond(
    responding: Packet,
    content: ByteBuf,
    error: Int = Packet.NO_ERROR,
): Packet = Packet(responding.component, responding.command, error, Packet.ERROR_TYPE, responding.id, content)


inline fun Channel.unique(
    component: Int,
    command: Int,
    id: Int = 0x0,
    error: Int = Packet.NO_ERROR,
    flush: Boolean = true,
    populate: TdfBuilder.() -> Unit = {},
) = send(createPacket(component, command, Packet.UNIQUE_TYPE, id, error, populate), flush)

inline fun unique(
    component: Int,
    command: Int,
    id: Int = 0x0,
    error: Int = Packet.NO_ERROR,
    populate: TdfBuilder.() -> Unit = {},
): Packet = createPacket(component, command, Packet.UNIQUE_TYPE, id, error, populate)

inline fun Channel.packet(
    component: Int,
    command: Int,
    qtype: Int,
    id: Int = 0x0,
    error: Int = Packet.NO_ERROR,
    flush: Boolean = true,
    populate: TdfBuilder.() -> Unit = {},
) = send(createPacket(component, command, qtype, id, error, populate), flush)

inline fun createPacket(
    component: Int,
    command: Int,
    qtype: Int,
    id: Int = 0x0,
    error: Int = Packet.NO_ERROR,
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

