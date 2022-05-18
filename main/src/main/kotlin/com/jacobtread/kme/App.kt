@file:JvmName("App")

package com.jacobtread.kme

import com.jacobtread.kme.database.startDatabase
import com.jacobtread.kme.servers.startDiscardServer
import com.jacobtread.kme.servers.startHttpServer
import com.jacobtread.kme.servers.startMainServer
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.nameThread

// The version of KME
const val KME_VERSION = "1.0.0"

// Load the config as a global variable
val CONFIG = loadConfigFile()

fun main() {
    nameThread("Main")

    Logger.init(CONFIG.logging)
    Logger.info("Starting ME3 Server")

    startDatabase(CONFIG.database)
    startHttpServer()
    startMainServer()

    // Telemetry & Ticker servers just discard any contents they receive
    startDiscardServer("Telemetry", CONFIG.telemetry)
    startDiscardServer("Ticker", CONFIG.ticker)

    // Infinite loop to read commands from System in
    while (true) {
        val input = readLine() ?: continue
        Logger.info("Unknown command: $input")
    }
}
