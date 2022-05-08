package com.jacobtread.kme.blaze

import io.netty.buffer.Unpooled

fun packet(
    component: PacketComponent,
    command: PacketCommand,
    qtype: Int,
    id: Int,
    error: Int = 0,
    content: TdfBuilder.() -> Unit,
): RawPacket = packet(component.id, command.value, qtype, id, error, content)

fun packet(
    component: Int,
    command: Int,
    qtype: Int,
    id: Int,
    error: Int = 0,
    populate: TdfBuilder.() -> Unit,
): RawPacket {
    val contentBuffer = Unpooled.buffer()
    val contentBuilder = TdfBuilder()
    contentBuilder.populate()
    contentBuilder.write(contentBuffer)
    val length = contentBuffer.readableBytes()
    val content = ByteArray(length)
    contentBuffer.readBytes(content)
    return RawPacket(
        component,
        command,
        error,
        qtype,
        id,
        content
    )
}
