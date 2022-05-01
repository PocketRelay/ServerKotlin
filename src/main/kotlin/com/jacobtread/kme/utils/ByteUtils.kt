package com.jacobtread.kme.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

const val NULL_CHAR: Char = Char.MIN_VALUE

object ByteUtils {


}

fun String.getIp(): Long {
    var result = 0L
    val (a, b, c, d) = split('.')
    result = result or (a.toInt() shl 24).toLong()
    result = result or (b.toInt() shl 16).toLong()
    result = result or (c.toInt() shl 8).toLong()
    result = result or (d.toInt()).toLong()
    return result
}

fun Long.getIp(): String = "${this shr 24}.${(this shr 16) and 0xFF}.${(this shr 8) and 0xFF}.${this and 0xFF}"

fun Int.asBEBytes(): ByteArray {
    val buf = ByteBuffer.allocate(4)
    buf.order(ByteOrder.BIG_ENDIAN)
    buf.putInt(this)
    return buf.array()
}