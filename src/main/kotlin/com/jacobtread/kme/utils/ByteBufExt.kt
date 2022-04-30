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

fun ByteBuf.writeVarInt(value: Long) {
    var v = value
    while (true) {
        if (v and -128 == 0L) {
            writeByte(v.toInt())
            return
        }
        writeByte(((v and 0x7F) or 0x80).toInt())
        v = value ushr 7
    }
}

fun ByteBuf.readVarInt(): Long {
    var value = 0L
    var position = 0
    var currentByte: Long
    while (true) {
        currentByte = readByte().toLong()
        value = value or ((currentByte and 0x7F) shl position)
        if (currentByte and 0x80 == 0L) break
        position += 7
        if (position >= 32) throw RuntimeException("VarInt is too big")
    }
    return value
}

fun ByteBuf.readString(): String {
    val length = readVarInt()
    val bytes = ByteArray(length.toInt())
    readBytes(bytes)
    readByte()
    return String(bytes, Charsets.UTF_8)
}

fun ByteBuf.writeString(value: String) {
    val v = if (value.endsWith(Char.MIN_VALUE)) value else (value + '\u0000')
    writeVarInt(v.length.toLong())
    writeBytes(value.toByteArray(Charsets.UTF_8))
}