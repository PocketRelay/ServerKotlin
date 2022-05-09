package com.jacobtread.kme.blaze.utils

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress


object IPAddress {

    private fun convertToBytes(address: SocketAddress): ByteArray {
        require(address is InetSocketAddress) { "Address is not InetSocketAddress" }
        return address.address.address
    }

    fun asLong(address: SocketAddress): Long {
        val bytes = convertToBytes(address)
        bytes.reverse()
        return BigEndian.bytesToUInt32(bytes)
    }

    fun asLong(value: String): Long {
        val parts = value.split('.', limit = 4)
        require(parts.size == 4) { "Invalid IPv4 Address" }
        return (parts[0].toLong() shl 24)
            .or(parts[1].toLong() shl 16)
            .or(parts[2].toLong() shl 8)
            .or(parts[3].toLong())
    }

    fun fromLong(value: Long): InetAddress {
        val bytes = BigEndian.uint32ToBytes(value)
        bytes.reverse()
        return InetAddress.getByAddress(bytes)
    }


}