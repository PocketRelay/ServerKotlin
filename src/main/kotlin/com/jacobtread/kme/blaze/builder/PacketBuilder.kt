package com.jacobtread.kme.blaze.builder

import com.jacobtread.kme.blaze.PacketCommand
import com.jacobtread.kme.blaze.PacketComponent
import com.jacobtread.kme.blaze.RawPacket
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

fun Packet(
    component: PacketComponent,
    command: PacketCommand,
    qtype: Int,
    id: Int,
    error: Int = 0,
    content: TdfBuilder.() -> Unit,
): RawPacket = Packet(component.id, command.value, qtype, id, error, content)

fun Packet(
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
