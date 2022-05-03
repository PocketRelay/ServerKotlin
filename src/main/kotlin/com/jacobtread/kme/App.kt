package com.jacobtread.kme

import com.jacobtread.kme.database.Database
import com.jacobtread.kme.logging.Level
import com.jacobtread.kme.logging.Logger
import com.jacobtread.kme.servers.*
import net.mamoe.yamlkt.Yaml
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

val LOGGER = Logger.get()

fun main() {
    Thread.currentThread().name = "Main"

    val rootPath = Paths.get(".")

    LOGGER.info("Starting ME3 Server")
    val configFile = rootPath.resolve("config.yml")
    val config: Config

    if (configFile.exists()) {
        val contents = configFile.readText()
        config = Yaml.decodeFromString(Config.serializer(), contents)
        LOGGER.info("Loaded Configuration from: $configFile")
    } else {
        LOGGER.info("No configuration found. Using default")
        config = Config()
        try {
            LOGGER.debug("Saving newly created config to: $configFile")
            configFile.writeText(Yaml.encodeToString(config))
        } catch (e: Exception) {
            LOGGER.error("Failed to write newly created config file", e)
        }
    }
    Logger.setLogLevel(Level.fromName(config.logLevel))

    startRedirector(config)
    startTickerServer(config)
    startTelemetryServer(config)
    startHttpServer(config)

//    val database = Database.connect(config)
//    startMainServer(config, database)

    val input = System.`in`
    val inputReader = input.bufferedReader()
    while (true) {
        val input = inputReader.readLine()
        LOGGER.info("Unknown command: $input")
    }
}

