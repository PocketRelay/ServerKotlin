package com.jacobtread.kme.utils

import java.net.InetAddress

private val IPV4_REGEX = Regex("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)(\\.(?!\$)|\$)){4}\$")

fun lookupServerAddress(value: String): ServerAddress {
    if (value.matches(IPV4_REGEX)) {
        return ServerAddress(value, value)
    } else {
        if (value == "localhost") return ServerAddress("127.0.0.1", value)
        val address = InetAddress.getByName(value)
        val ip = address.hostAddress
        return ServerAddress(ip, value)
    }
}

data class ServerAddress(val ip: String, val host: String) {
    val address = IPAddress.asLong(ip)
}