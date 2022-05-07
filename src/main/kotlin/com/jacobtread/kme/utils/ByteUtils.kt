package com.jacobtread.kme.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

const val NULL_CHAR: Char = Char.MIN_VALUE

object ByteUtils {


}

fun String.getIp(): Long {
    var result = 0L
    val parts = split('.', limit = 4)
    result = result or (parts[0].toInt() shl 24).toLong()
    result = result or (parts[1].toInt() shl 16).toLong()
    result = result or (parts[2].toInt() shl 8).toLong()
    result = result or (parts[3].toInt()).toLong()
    return result
}

fun Long.getIp(): String = "${this shr 24}.${(this shr 16) and 0xFF}.${(this shr 8) and 0xFF}.${this and 0xFF}"

fun Int.asBEBytes(): ByteArray {
    val buf = ByteBuffer.allocate(4)
    buf.order(ByteOrder.BIG_ENDIAN)
    buf.putInt(this)
    return buf.array()
}

fun Int.hex(): String {
    return "0x" + this.toString(16)
}

fun Long.hex(): String {
    return "0x" + this.toString(16)
}

fun Byte.hex(): String {
    return "0x" + this.toString(16)
}

fun Short.hex(): String {
    return "0x" + this.toString(16)
}