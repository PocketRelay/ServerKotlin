package com.jacobtread.kme.utils

import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory

object Security {
    // Shared instance of private key and certificate, so they don't
    // get recreated for every ssl context
    val keyStore = loadKeystore()
}

/**
 * createContext Creates a new ssl context for use with the server
 * socket and enables SSLv3 and the required ciphers
 *
 * @return
 */
fun createContext(): SslContext {
    java.security.Security.setProperty("jdk.tls.disabledAlgorithms", "");
    return SslContextBuilder.forServer(Security.keyStore)
        .ciphers(listOf("TLS_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_RC4_128_MD5"))
        .protocols("SSLv3", "TLSv1.2", "TLSv1.3")
        .startTls(true)
        .trustManager(InsecureTrustManagerFactory.INSTANCE)
        .build() ?: throw IllegalStateException("Unable to create SSL Context")
}

private fun loadKeystore(): KeyManagerFactory {
    val password = "123456".toCharArray()
    val stream = Security::class.java.getResourceAsStream("/redirector.pfx")
        ?: throw IllegalStateException("Missing required keystore")
    val keyStore = KeyStore.getInstance("PKCS12")
    keyStore.load(stream, password)
    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    kmf.init(keyStore, password)
    return kmf
}