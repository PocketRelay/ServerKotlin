package com.jacobtread.kme.utils

import com.jacobtread.kme.blaze.Packet
import io.netty.buffer.ByteBuf

fun ByteBuf.readPacket(): Packet {
    val length = readUnsignedShort();
    val component = readUnsignedShort()
    val command = readUnsignedShort()
    val error = readUnsignedShort()
    val qtype = readUnsignedShort()
    val id = readUnsignedShort()
    val extLength = if ((qtype and 0x10) != 0) readUnsignedShort() else 0
    val contentLength = length + (extLength shl 16)
    val content = ByteArray(contentLength)
    readBytes(content)
    return Packet(component, command, error, qtype, id, content)
}
