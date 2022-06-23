package com.jacobtread.kme.utils

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress

object IPAddress {

    private fun convertToBytes(address: SocketAddress): ByteArray {
        require(address is InetSocketAddress) { "Address is not InetSocketAddress" }
        return address.address.address
    }

    fun asLong(address: SocketAddress): ULong {
        val bytes = convertToBytes(address)
        return (bytes[0].toULong() shl 24)
            .or(bytes[1].toULong() shl 16)
            .or(bytes[2].toULong() shl 8)
            .or(bytes[3].toULong())
    }

    fun asLong(value: String): Long {
        val parts = value.split('.', limit = 4)
        require(parts.size == 4) { "Invalid IPv4 Address" }
        return (parts[0].toLong() shl 24)
            .or(parts[1].toLong() shl 16)
            .or(parts[2].toLong() shl 8)
            .or(parts[3].toLong())
    }

    fun fromULong(value: ULong): InetAddress {
        val bytes = byteArrayOf(
            value.toByte(),
            (value shr 8).toByte(),
            (value shr 16).toByte(),
            (value shr 24).toByte(),
        )
        return InetAddress.getByAddress(bytes)
    }

    fun fromULongStr(value: ULong): String {
        return ((value shr 24) and 255u).toString() + "." + ((value shr 16) and 255u) + "." + ((value shr 8) and 255u) + "." + (value and 255u)
    }
}