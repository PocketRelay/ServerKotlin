package com.jacobtread.kme.utils

import java.time.Instant

/**
 * Uses the java Instant api to return the current
 * unix time in seconds.
 *
 * @return The current unix time in seconds
 */
fun unixTimeSeconds(): Long = Instant.now().epochSecond

/**
 * Calculates the time in seconds between the current
 * unix time in seconds and the provided unix time in
 * seconds.
 *
 * @see unixTimeSeconds for retrieving unix time in seconds
 * @param time The time to calculate the difference between
 * @return The difference in seconds between the current and provided times
 */
fun unixTimeSecondsSince(time: Long): Long = unixTimeSeconds() - time

/**
 * Caclulates the time in days between the current time
 * in unix seconds and the provided time in unix seconds
 *
 * @param time The time to calculate the difference between
 * @return The difference in days between in the current and provided times
 */
fun unixTimeDaysSince(time: Long): Long = (unixTimeSecondsSince(time) / 86400 /* 1 day = 86400 seconds */)

/**
 * Converts an IPv4 string (e.g. 192.168.0.1) into the encoded
 * format represented by a single unsigned long
 *
 * @param value The IPv4 address
 * @return The encoded value
 */
fun getIPv4Encoded(value: String): ULong {
    val ipv4Regex = Regex("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)(\\.(?!\$)|\$)){4}\$")
    if (value.matches(ipv4Regex)) { // Check if the address is an IPv4 Address
        val ipParts = value.split('.', limit = 4) // Split the address into 4 parts
        require(ipParts.size == 4) { "Invalid IPv4 Address" } // Ensure that the address is 4 parts
        // Encoding the address as an unsigned long value
        return (ipParts[0].toULong() shl 24)
            .or(ipParts[1].toULong() shl 16)
            .or(ipParts[2].toULong() shl 8)
            .or(ipParts[3].toULong())

    }
    return 0u
}