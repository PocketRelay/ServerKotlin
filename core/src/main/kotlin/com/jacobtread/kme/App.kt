@file:JvmName("App")

package com.jacobtread.kme

import com.jacobtread.kme.database.startDatabase
import com.jacobtread.kme.logging.Level
import com.jacobtread.kme.logging.Logger
import com.jacobtread.kme.servers.startDiscardServer
import com.jacobtread.kme.servers.startHttpServer
import com.jacobtread.kme.servers.startMainServer
import com.jacobtread.kme.servers.startRedirector
import com.jacobtread.kme.utils.nameThread
import java.security.Security

// The version of KME
const val KME_VERSION = "1.0.0"
// Load the config as a global variable
val CONFIG = loadConfigFile()

fun main() {
    // Clears the disabled algorithms necessary for SSLv3
    Security.setProperty("jdk.tls.disabledAlgorithms", "")
    nameThread("Main")

    Logger.setLevelFrom(CONFIG.logLevel)
    Logger.info("Starting ME3 Server")

    startRedirector(CONFIG.ports.redirector, CONFIG.ports.main)
    startDatabase(CONFIG.database)
    startHttpServer()
    startMainServer()

    // Telemetry & Ticker servers just discard any contents they receive
    startDiscardServer("Telemetry", CONFIG.ports.telemetry)
    startDiscardServer("Ticker", CONFIG.ports.ticker)

    // Infinite loop to read commands from System in
    while (true) {
        val input = readLine() ?: continue
        Logger.info("Unknown command: $input")
    }
}
