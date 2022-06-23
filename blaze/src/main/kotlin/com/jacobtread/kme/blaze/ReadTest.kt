package com.jacobtread.kme.blaze

import com.jacobtread.kme.utils.IPAddress
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

fun main() {
    val bytes = intArrayOf(
        192, 194, 252, 180, 0
    )
    val b = bytes.map { it.toByte() }.toByteArray()

    val wrapper = Unpooled.wrappedBuffer(b)
    val varInt = wrapper.readVarIntPrint()
    println(varInt)
    val ipbytes = IPAddress.asLong("192.168.1.74");
    println(IPAddress.fromULongStr(3232235851u))
//    val v = wrapper.readUnsignedInt()
//
//    println(v)
//    val tag = (v and 0xFFFFFF00)
//    println(v and 0xFF)
//
//    println(Tdf.createLabel(tag))
}

@OptIn(ExperimentalUnsignedTypes::class)
fun ByteBuf.readVarIntPrint(): Long {
    val bytes = ArrayList<UByte>()
    var byte: UByte
    while (true) {
        byte = readUnsignedByte().toUByte()
        println(byte)
        bytes.add(byte)
        if (byte < 0x80u) {
            break
        }
    }

    println("Consumed ${bytes.size} bytes")

    val buf = bytes.toUByteArray()
    var curshift = 6
    var result: ULong = (buf[0] and 63u).toULong()
    for (i in 1 until buf.size) {
        val curbyte = buf[i]
        val l = (curbyte and 127u).toULong() shl curshift
        result = result or  l
        curshift += 7
    }



//    val firstByte = readUnsignedByte().toLong()
//    if (firstByte < 0x80) return firstByte and 0x3F
//    var shift = 6
//    var result = firstByte and 0x3F
//    var byte: Long
//    var i =1
//    do {
//        byte = readUnsignedByte().toLong()
//        result = result or ((byte and 0x7F) shl shift)
//        shift += 7
//        i++
//    } while (byte >= 0x80)
//
//    println("Stopped reading at $i")
    return result.toLong()
}
