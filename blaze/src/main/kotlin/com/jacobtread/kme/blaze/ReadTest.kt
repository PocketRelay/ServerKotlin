package com.jacobtread.kme.blaze

import com.jacobtread.kme.blaze.tdf.Tdf
import com.jacobtread.kme.blaze.utils.readVarInt
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

@OptIn(ExperimentalUnsignedTypes::class)
fun main() {
    val value: ULong = 0x5a4f2b378b715c6u
    val bytes = intArrayOf(
        139, 57,
    )
    val b = bytes.map { it.toByte() }.toByteArray()
    val wrapper = Unpooled.wrappedBuffer(b)
    val varInt = wrapper.readVarIntPrint()
    println(varInt)
//    val v = wrapper.readUnsignedInt()
//
//    println(v)
//    val tag = (v and 0xFFFFFF00)
//    println(v and 0xFF)
//
//    println(Tdf.createLabel(tag))
}

fun ByteBuf.readVarIntPrint(): Long {
    val firstByte = readUnsignedByte().toLong()
    if (firstByte < 0x80) return firstByte and 0x3F
    var shift = 6
    var result = firstByte and 0x3F
    var byte: Long
    var i =1
    do {
        byte = readUnsignedByte().toLong()
        result = result or ((byte and 0x7F) shl shift)
        shift += 7
        i++
    } while (byte >= 0x80)

    println("Stopped reading at $i")
    return result
}
