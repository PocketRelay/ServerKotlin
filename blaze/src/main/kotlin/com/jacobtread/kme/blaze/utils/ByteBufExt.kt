package com.jacobtread.kme.blaze.utils

import io.netty.buffer.ByteBuf

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
    val firstByte = readUnsignedByte().toLong()
    var shift = 6
    var result = firstByte.and(0x3F)
    if (firstByte >= 0x80) {
        var byte: Long
        do {
            byte = readUnsignedByte().toLong()
            result = result.or(byte.and(0x7F).shl(shift))
            shift += 7
        } while (byte >= 0x80)
    }
    return result

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