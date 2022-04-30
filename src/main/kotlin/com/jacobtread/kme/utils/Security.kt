package com.jacobtread.kme.utils

import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

const val PRIVATE_KEY = """
-----BEGIN PRIVATE KEY-----
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
FfGX4OJNAiwkLe/PfFMwCTCLKXhCrA==
-----END PRIVATE KEY-----
"""


/**
 * CERTIFICATE X.509 Certificate used by the SSL context
 */
const val CERTIFICATE = """
-----BEGIN CERTIFICATE-----
MIIFZDCCA0ygAwIBAgITLmeHk7tcLNncfLf6Nqhn4sBtjTANBgkqhkiG9w0BAQsF
ADBCMQswCQYDVQQGEwJVUzEOMAwGA1UEAwwFR29tZXMxIzAhBgkqhkiG9w0BCQEW
FGphY29idHJlYWRAZ21haWwuY29tMB4XDTIyMDMwNzIzNTgyMloXDTIyMDQwNjIz
NTgyMlowQjELMAkGA1UEBhMCVVMxDjAMBgNVBAMMBUdvbWVzMSMwIQYJKoZIhvcN
AQkBFhRqYWNvYnRyZWFkQGdtYWlsLmNvbTCCAiIwDQYJKoZIhvcNAQEBBQADggIP
ADCCAgoCggIBANbKg6ru8ZkJckkLEXmHPcKJaCfwsp8qTUOzgqnCJ+u19ppxyesU
UwIK0FTl0AWdU7w1hGSktEqjIar06zXw9Q83ANjR3J6C1v/iEypFnd+qzRiitmop
NT2FMmhORaBPisNVe4BtXnSozgI6864t0pKpIrM+QtByC8UlbXVRnliOMoNnTyuP
IGxa0uqmKS5kXsdqnXmT102n2SCb7rR9NlQ9Wa9pOaxqgToVz917fydXjYkvhW2T
lc78Rf0BTwKjp8v3+qAaoKmM+PCbAz43bTY0OjTiH2b6pmFBLRiUpmCZ616G6Xs4
UdLfcKMM9zoCx/BfLbOJl2sPuyxL3PwlNx011pu4YxsTgFa1Xi63vQeDFXb6wNN+
F1tZmRUdrGm7N+f27ABA5cjczkRVlQhofwfHcJGx9hlv4m9Mh4a2fGSOcm+CCnwo
y6nZC3FxxnK/1AHPDGPQVk7UJZUBJbeiYetoaBNIgZz0OSRs2rUKzh9NAbElLO1Y
x1luDd0RGKI0vJMW4Co/bA4gFJQRx5xjt40DyCd7JT4KdmqVVDMU1rn3tl4hKJs8
IwReT2PrekVEQ+9GHzgP7++i4yIeBKXHyZQsQ02fGttCxGXdlmeecSGrpdqz4c9a
oGIfVy67i/Lr80T42tUF8zHo/iWM3KA8v9YNd7rjEEhW7uL/kzFNdhufAgMBAAGj
UzBRMB0GA1UdDgQWBBTTyB4dbfWSxz2coIDnto8423/1KjAfBgNVHSMEGDAWgBTT
yB4dbfWSxz2coIDnto8423/1KjAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEB
CwUAA4ICAQCS5Bx/Ngbj+LznBY/gFjLDuCN7A00UIIyUcLoramsQyYRuLsqBrimo
uRth8q1+OFMv3x+h5Lx1+xe30I64VGampbyFn1JmsHhBGFtE9lHn8oVAoekssIG5
UScR08slNsVVgiuPT10nEIRLyV445vKd6AMA+wfAn7jsP1wGpmDtsCF5dAw4+bre
epxy+nSD4wRIIq4MImaVSUPiLZ8shMGQvZOWkXR0N1utIgygst2kyBqMHq7NcOXC
1XzrhHZlCcL5bAd0J44XskMseeCjvXhoc0sp/F+tnuMNLUZQWukEyw/ZG6cO/AyL
ptOnfHEwwXq1nLWXG9yhb+DBRwsoby1WqVDt2YgMDMSmGZ55zPhhZvEodU/zoE8f
WhWrzD7QQQ++vjQokOzrU39fJdo1LDMgHY23ytVQBdil8+QwhIcD5RoaoAixPPlJ
KVNcPlO84JQr8R96jcKGVfPR4ZhayjzJDqJ3W5Pq52xISyWeBFen6+z8ezib3NKv
4X55yTOqO2UizDR5wLZhvFNml95dXLLx4LRe0OI/11286YrWTCArqVvbsaHpQyfx
dCGz2e4y0MdABtw/VdXIouMN0yp8Zzh9Ohk0Wb7l+PtZZ8paPLfzfiWF6HrlulDW
Sq1GwA5r8YlliQyBMBsb+QuPQa4qjrN0yU4elkejhY4xlfoZFH5hXw==
-----END CERTIFICATE-----
"""

object Security {
    // Shared instance of private key and certificate, so they don't
    // get recreated for every ssl context
    val privateKey = loadPrivateKey()
    val certificate = loadCertificate()
}

/**
 * createContext Creates a new ssl context for use with the server
 * socket and enables SSLv3 and the required ciphers
 *
 * @return
 */
fun createContext(): SslContext {
    java.security.Security.setProperty("jdk.tls.disabledAlgorithms", "");
    return SslContextBuilder.forServer(Security.privateKey, Security.certificate)
        .ciphers(listOf("TLS_RSA_WITH_RC4_128_MD5", "TLS_RSA_WITH_RC4_128_SHA"))
        .protocols("SSLv3")
        .build() ?: throw IllegalStateException("Unable to create SSL Context")
}

/**
 * loadPrivateKey Loads the server private key from the PRIVATE_KEY constant
 * for use within the SSLContext
 *
 * @return The private key
 */
private fun loadPrivateKey(): PrivateKey {
    val contents = PRIVATE_KEY
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("\n", "")
        .replace("-----END PRIVATE KEY-----", "")
        .trim()
    val encoded = Base64.getDecoder().decode(contents)
    val keyFactory = KeyFactory.getInstance("RSA")
    val keySpec = PKCS8EncodedKeySpec(encoded)
    return keyFactory.generatePrivate(keySpec)
}

/**
 * loadCertificate Loads the server certificate from the CERTIFICATE constant
 * for use within the SSLContext
 *
 * @return The X.509 certificate
 */
private fun loadCertificate(): X509Certificate {
    val factory = CertificateFactory.getInstance("X.509")
        ?: throw IllegalStateException("Unable to create certificate factory")
    val cert: Certificate? = CERTIFICATE.byteInputStream().use {
        return@use factory.generateCertificate(it)
    }
    if (cert !is X509Certificate) throw IllegalStateException("Expected certificate to be X.509")
    return cert
}