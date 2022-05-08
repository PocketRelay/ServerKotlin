package com.jacobtread.kme.blaze.utils

const val NULL_CHAR: Char = Char.MIN_VALUE

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

fun Int.hex() = "0x" + this.toString(16)
fun Short.hex() = "0x" + this.toString(16)
fun Long.hex() = "0x" + this.toString(16)
fun Byte.hex() = "0x" + this.toString(16)