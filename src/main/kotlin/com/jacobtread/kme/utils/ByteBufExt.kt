package com.jacobtread.kme.utils

import com.jacobtread.kme.blaze.RawPacket
import io.netty.buffer.ByteBuf


fun ByteBuf.readPacket(): RawPacket {
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
    return RawPacket(component, command, error, qtype, id, content)
}

fun ByteBuf.writeVarInt(value: Long) {
    if (value < 0x40) {
        writeByte((value and 0xFF).toInt())
    } else {
        var curByte = (value and 0x3F).toInt() or 0x80
        writeByte(curByte)
        var curShift = value shr 6
        while (curShift >= 0x80) {
            curByte = ((curShift and 0x7F) or 0x80).toInt()
            curShift = curShift shr 7
            writeByte(curByte)
        }
        writeByte(curShift.toInt())
    }
}

fun ByteBuf.readVarInt(): Long {
    var value = 0L
    var position = 0
    var currentByte: Int
    while (true) {
        currentByte = readUnsignedByte().toInt()
        value = value or ((currentByte and 0x7F) shl position).toLong()
        if (currentByte and 0x80 == 0) break
        position += 7
        if (position >= 32) throw RuntimeException("VarInt is too big")
    }
    return value
}

fun ByteBuf.readString(): String {
    val length = readVarInt()
    val bytes = ByteArray(length.toInt() - 1)
    readBytes(bytes)
    readUnsignedByte()
    return String(bytes, Charsets.UTF_8)
}

fun ByteBuf.writeString(value: String) {
    val v = if (value.endsWith(Char.MIN_VALUE)) value else (value + '\u0000')
    writeVarInt(v.length.toLong())
    writeBytes(v.toByteArray(Charsets.UTF_8))
}