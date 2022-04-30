package com.jacobtread.kme.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

object ByteUtils {

}

fun Int.asBEBytes(): ByteArray {
    val buf = ByteBuffer.allocate(4)
    buf.order(ByteOrder.BIG_ENDIAN)
    buf.putInt(this)
    return buf.array()
}