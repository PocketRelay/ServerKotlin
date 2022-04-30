package com.jacobtread.kme.blaze

import io.netty.buffer.ByteBuf

class Packet {
}

fun readRawPacket(buf: ByteBuf): RawPacket {
    return RawPacket(0,0,0,0,0, 0, byteArrayOf())
}

data class RawPacket(
    val length: Short,
    val component: Short,
    val command: Short,
    val error: Short,
    val qtype: Short,
    val id: Short,
    val content: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawPacket

        if (length != other.length) return false
        if (component != other.component) return false
        if (command != other.command) return false
        if (error != other.error) return false
        if (qtype != other.qtype) return false
        if (id != other.id) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = length.toInt()
        result = 31 * result + component
        result = 31 * result + command
        result = 31 * result + error
        result = 31 * result + qtype
        result = 31 * result + id
        result = 31 * result + content.contentHashCode()
        return result
    }
}