package com.jacobtread.kme.blaze.builder

import com.jacobtread.kme.blaze.PacketCommand
import com.jacobtread.kme.blaze.PacketComponent
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

fun Packet(
    component: PacketComponent,
    command: PacketCommand,
    qtype: Int,
    id: Int,
    error: Int = 0,
    content: TdfBuilder.() -> Unit,
): ByteBuf = Packet(component.id, command.value, qtype, id, error, content)

fun Packet(
    component: Int,
    command: Int,
    qtype: Int,
    id: Int,
    error: Int = 0,
    populate: TdfBuilder.() -> Unit,
): ByteBuf {
    val contentBuffer = Unpooled.buffer()
    val contentBuilder = TdfBuilder()
    contentBuilder.populate()
    contentBuilder.write(contentBuffer)
    val buff = Unpooled.buffer();
    val length = contentBuffer.readableBytes()
    buff.writeByte((length and 0xFFFF) shr 8)
    buff.writeByte((length and 0xFF))
    buff.writeByte(component shr 8)
    buff.writeByte(component and 0xFF)
    buff.writeByte(command shr 8)
    buff.writeByte(command and 0xFF)
    buff.writeByte(error shr 8)
    buff.writeByte(error and 0xFF)
    buff.writeByte(qtype shr 8)
    if (length > 0xFFFF) {
        buff.writeByte(0x10)
    } else {
        buff.writeByte(0x00)
    }
    buff.writeByte(id shr 8)
    buff.writeByte(id and 0xFF)
    if (length > 0xFFFF) {
        buff.writeByte(((length.toLong() and 0xFF000000) shr 24).toInt())
        buff.writeByte((length and 0x00FF0000) shr 16)
    }
    buff.writeBytes(contentBuffer)
    return buff
}
