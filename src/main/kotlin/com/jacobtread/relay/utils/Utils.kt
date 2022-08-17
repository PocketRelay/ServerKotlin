package com.jacobtread.relay.utils

import com.jacobtread.relay.servers.RedirectorHandler
import com.jacobtread.relay.utils.logging.Logger
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.time.Instant
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLException

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

/**
 * Generates a string of random characters from a-zA-Z0-9
 * with the provided [length] using a string builder and
 * returns that value
 *
 * @param length the length of the string to generate
 * @return The generated string
 */
fun generateRandomString(length: Int): String {
    val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPSQRSTUVWXYZ0123456789"
    val builder = StringBuilder()
    repeat(length) { builder.append(chars.random()) }
    return builder.toString()
}

/**
 * Creates a new [SslContext] for Netty to create SslHandlers from
 * so that we can accept the SSLv3 traffic. Any exceptions
 *
 * @return The created [SslContext]
 */
fun createServerSslContext(): SslContext {
    try {
        val keyStorePassword = charArrayOf('1', '2', '3', '4', '5', '6')
        val keyStoreStream = RedirectorHandler::class.java.getResourceAsStream("/redirector.pfx")
        checkNotNull(keyStoreStream) { "Missing required keystore for SSLv3" }
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStoreStream.use {
            keyStore.load(keyStoreStream, keyStorePassword)
        }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, keyStorePassword)

        // Create new SSLv3 compatible context
        val context = SslContextBuilder.forServer(kmf)
            .ciphers(listOf("TLS_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_RC4_128_MD5"))
            .protocols("SSLv3", "TLSv1.2", "TLSv1.3")
            .startTls(true)
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build()
        checkNotNull(context) { "Unable to create SSL Context" }
        return context
    } catch (e: SSLException) {
        Logger.fatal("Failed to create SSLContext for redirector", e)
    } catch (e: GeneralSecurityException) {
        Logger.fatal("Failed to create SSLContext for redirector", e)
    }
}
