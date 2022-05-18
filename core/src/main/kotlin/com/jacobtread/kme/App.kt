@file:JvmName("App")

package com.jacobtread.kme

import com.jacobtread.kme.database.startDatabase
import com.jacobtread.kme.logging.Level
import com.jacobtread.kme.logging.Logger
import com.jacobtread.kme.servers.startDiscardServer
import com.jacobtread.kme.servers.startHttpServer
import com.jacobtread.kme.servers.startMainServer
import com.jacobtread.kme.servers.startRedirector
import java.security.Security


val CONFIG = loadConfigFile()

fun main() {
    Security.setProperty("jdk.tls.disabledAlgorithms", "")
    Thread.currentThread().name = "Main"
    Logger.logLevel = Level.fromName(CONFIG.logLevel)

    Logger.info("Starting ME3 Server")

    startRedirector(CONFIG.ports.redirector, CONFIG.ports.main)
    startDatabase(CONFIG.database)
    startHttpServer()
    startMainServer()

    // Telemetry & Ticker servers just discard any contents they receive
    startDiscardServer("Telemetry", CONFIG.ports.telemetry)
    startDiscardServer("Ticker", CONFIG.ports.ticker)

    while (true) {
        val input = readLine() ?: continue
        Logger.info("Unknown command: $input")
    }
}
