package com.jacobtread.kme.utils

import com.jacobtread.kme.utils.logging.Logger
import java.net.InetAddress
import java.net.UnknownHostException

private val IPV4_REGEX = Regex("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)(\\.(?!\$)|\$)){4}\$")

fun lookupServerAddress(value: String): ServerAddress {
    if (value.matches(IPV4_REGEX)) {
        return ServerAddress(value, value)
    } else {
        if (value == "localhost") return ServerAddress("127.0.0.1", value)
        try {
            val address = InetAddress.getByName(value)
            val ip = address.hostAddress
            return ServerAddress(ip, value)
        } catch (e: UnknownHostException) {
            Logger.fatal("Unable to lookup server address \"$value\"", e)
        }
    }
}

data class ServerAddress(val ip: String, val host: String) {
    val address = IPAddress.asLong(ip)
}