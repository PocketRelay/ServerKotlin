package com.jacobtread.kme

import net.mamoe.yamlkt.Yaml
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText


fun main() {
    val configFile = Paths.get("config.yml")
    val config: Config = if (configFile.exists()) {
        val contents = configFile.readText()
        Yaml.decodeFromString(Config.serializer(), contents)
    } else {
        val value = Config()
        try {
            configFile.writeText(Yaml.encodeToString(value))
        } catch (e: Exception) {
            System.err.println("Failed to write config file: ${e.message ?: e.javaClass.simpleName}")
        }
        value
    }
    RedirectorServer.create(config)
}

