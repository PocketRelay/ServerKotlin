package com.jacobtread.kme.blaze.utils

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

fun ByteBuf.writeVarInt(value: Long) = writeVarInt(value.toULong())
fun ByteBuf.writeVarInt(value: UInt) = writeVarInt(value.toULong())
fun ByteBuf.writeVarInt(value: Int) = writeVarInt(value.toULong())

fun ByteBuf.writeVarInt(value: ULong) {
    if (value < 64u) {
        writeByte((value and 255u).toInt())
    } else {
        var curByte = (value and 63u).toUByte() or 0x80u
        writeByte(curByte.toInt())
        var curShift = value shr 6
        while (curShift >= 128u) {
            curByte = ((curShift and 127u) or 128u).toUByte()
            curShift = curShift shr 7
            writeByte(curByte.toInt())
        }
        writeByte(curShift.toInt())
    }
}

fun ByteBuf.readVarInt(): ULong {
    val firstByte = readUnsignedByte().toUByte()
    var result: ULong = (firstByte and 63u).toULong()
    if (firstByte < 128u) return result
    var shift = 6
    var byte: UByte
    do {
        byte = readUnsignedByte().toUByte()
        result = result or ((byte and 127u).toULong() shl shift)
        shift += 7
    } while (byte >= 128u)
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
    val bytes = v.toByteArray(Charsets.UTF_8)
    writeVarInt(bytes.size)
    writeBytes(bytes)
}

fun ByteBuf.asString(): String {
    val start = readerIndex()
    val valueClone = copy(start, readableBytes())
    val output = StringBuilder()
    valueClone.forEachByte {
        output.append(it.toUByte().toString())
        output.append(", ")
        true
    }
    return output.toString()
}

fun ByteBuf.asByteArray(): ByteArray {
    val bytes = ByteArray(readableBytes())
    readBytes(bytes)
    return bytes
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.copiedBuffer(): ByteBuf = Unpooled.copiedBuffer(this, Charsets.UTF_8)