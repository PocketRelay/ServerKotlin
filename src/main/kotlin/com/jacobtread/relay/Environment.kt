package com.jacobtread.relay

import com.jacobtread.blaze.logging.PacketLogger
import com.jacobtread.relay.data.Constants
import com.jacobtread.relay.data.blaze.DebugLoggingHandler
import com.jacobtread.relay.data.retriever.OriginDetailsRetriever
import com.jacobtread.relay.data.retriever.Retriever
import com.jacobtread.relay.database.adapter.DatabaseAdapter
import com.jacobtread.relay.database.adapter.sql.MySQLDatabaseAdapter
import com.jacobtread.relay.database.adapter.sql.SQLiteDatabaseAdapter
import com.jacobtread.relay.exceptions.DatabaseException
import com.jacobtread.relay.utils.logging.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.Security
import java.util.*

/**
 * Environment This object stores the names of different system
 * environment variables that can control the server as well as
 * providing a function for applying said controls
 *
 * @constructor Create empty Environment
 */
object Environment {

    val database: DatabaseAdapter

    val externalAddress: String

    val redirectorPort: Int
    val mainPort: Int
    val httpPort: Int

    val menuMessage: String

    val mitmEnabled: Boolean

    val gawReadinessDecay: Float
    val gawEnabledPromotions: Boolean

    val apiEnabled: Boolean
    val apiUsername: String
    val apiPassword: String

    /**
     * Initializes the environment values from the system
     * environment variables as well as the config file if
     * KME_ENVIRONMENT_CONFIG then only the environment
     * variables and default configuration values will be
     * used.
     */
    init {
        Logger.info("Starting Pocket Relay server (version: ${Constants.RELAY_VERSION})")

        val env = Settings(
            System.getenv(),
            Properties()
        )

        // Choose whether to load the config or use the default
        if (!env.booleanValue("RELAY_ENVIRONMENT_CONFIG", null, false)) {
            loadProperties(env.properties) // Load the existing config file
        }

        val unpooledNetty = env.booleanValue("RELAY_NETTY_UNPOOLED", "netty.unpooled", false)
        if (unpooledNetty) {
            Logger.warn("Netty pooling disabled.")
            System.setProperty("io.netty.allocator.type", "unpooled")
        }
        // Clears the disabled algorithms. This is necessary for SSLv3
        Security.setProperty("jdk.tls.disabledAlgorithms", "")

        // Initialize the logger with its configuration
        Logger.init(
            env.stringValue("RELAY_LOGGER_LEVEL", "logging.level", "info"),
            env.booleanValue("RELAY_LOGGER_SAVE", "logging.save", true)
        )

        val logPackets = env.booleanValue("RELAY_LOGGER_PACKETS", "logging.packets", false)

        if (logPackets && Logger.debugEnabled) { // Load command and component names for debugging
            PacketLogger.init(DebugLoggingHandler())
        }

        // External address string
        externalAddress = env.stringValue(
            "RELAY_EXTERNAL_ADDRESS",
            "externalAddress",
            "kme.jacobtread.local"
        )

        // Server ports
        redirectorPort = env.intValue("RELAY_REDIRECTOR_PORT", "ports.redirector", 42127)
        mainPort = env.intValue("RELAY_MAIN_PORT", "ports.main", 14219)
        httpPort = env.intValue("RELAY_HTTP_PORT", "ports.http", 80)


        // Main menu message string
        menuMessage = env.stringValue(
            "RELAY_MENU_MESSAGE",
            "menuMessage",
            "<font color='#B2B2B2'>Pocket Relay v{v}</font> - <font color='#FFFF66'>Logged as: {n}</font>"
        )


        // Man in the middle configuration
        mitmEnabled = env.booleanValue("RELAY_MITM_ENABLED", "mitm", false)


        // Galaxy at war configuration
        gawReadinessDecay = env.floatValue("RELAY_GAW_READINESS_DECAY", "gaw.readinessDailyDecay", 0f)
        gawEnabledPromotions = env.booleanValue("RELAY_GAW_ENABLE_PROMOTIONS", "gaw.enablePromotions", true)

        // Panel information
        apiEnabled = env.booleanValue("RELAY_API_ENABLED", "api.enabled", false)
        apiUsername = env.stringValue("RELAY_API_USERNAME", "api.username", "admin")
        apiPassword = env.stringValue("RELAY_API_PASSWORD", "api.password", "admin")

        // Database configuration
        val databaseType = env.stringValue("RELAY_DATABASE_TYPE", "database.type", "sqlite")
            .lowercase()
        database = when (databaseType) {
            "mysql" -> {
                val host = env.stringValue("RELAY_MYSQL_HOST", "database.host", "127.0.0.1")
                val port = env.intValue("RELAY_MYSQL_PORT", "database.port", 3306)
                val user = env.stringValue("RELAY_MYSQL_USER", "database.user", "root")
                val password = env.stringValue("RELAY_MYSQL_PASSWORD", "database.password", "password")
                val database = env.stringValue("RELAY_MYSQL_DATABASE", "database.db", "relay")
                MySQLDatabaseAdapter(host, port, user, password, database)
            }

            "sqlite" -> {
                val file = env.stringValue("RELAY_SQLITE_FILE", "database.file", "data/app.db")
                SQLiteDatabaseAdapter(file)
            }

            else -> Logger.fatal("Unknwon database type: $databaseType (expected mysql, or sqlite)")
        }
        try {
            database.setup()
        } catch (e: DatabaseException) {
            Logger.fatal("Failed to setup database", e)
        }

        val retrieverEnabled = env.booleanValue("RELAY_RETRIEVE_OFFICIAL", "retriever.enabled", true)
        if (retrieverEnabled || mitmEnabled) {
            Retriever.isEnabled = true
            OriginDetailsRetriever.isDataFetchingEnabled = env.booleanValue("RELAY_RETRIEVE_ORIGIN_DATA", "retriever.originPlayerData.enabled", true)
        }
    }

    private class Settings(val env: Map<String, String>, val properties: Properties) {

        fun stringValue(envKey: String, propertiesKey: String?, default: String): String {
            val envValue = env[envKey]
            if (envValue != null) return envValue
            if (propertiesKey == null) return default
            return when (val propertyValue = properties[propertiesKey]) {
                is String -> propertyValue
                else -> default
            }
        }

        fun booleanValue(envKey: String, propertiesKey: String?, default: Boolean): Boolean {
            val envValue = env[envKey]
            if (envValue != null) {
                val booleanValue = envValue.toBooleanStrictOrNull()
                if (booleanValue != null) return booleanValue
            }
            if (propertiesKey == null) return default
            return when (val propertyValue = properties[propertiesKey]) {
                is String -> propertyValue.toBooleanStrictOrNull() ?: default
                is Boolean -> propertyValue
                else -> default
            }
        }


        fun intValue(envKey: String, propertiesKey: String?, default: Int): Int {
            val envValue = env[envKey]
            if (envValue != null) {
                val intValue = envValue.toIntOrNull()
                if (intValue != null) return intValue
            }
            if (propertiesKey == null) return default
            return when (val propertyValue = properties[propertiesKey]) {
                is String -> propertyValue.toIntOrNull() ?: default
                is Int -> propertyValue
                else -> default
            }
        }

        fun floatValue(envKey: String, propertiesKey: String?, default: Float): Float {
            val envValue = env[envKey]
            if (envValue != null) {
                val floatValue = envValue.toFloatOrNull()
                if (floatValue != null) return floatValue
            }
            if (propertiesKey == null) return default
            return when (val propertyValue = properties[propertiesKey]) {
                is String -> propertyValue.toFloatOrNull() ?: default
                is Float -> propertyValue
                else -> default
            }
        }
    }

    private fun loadProperties(properties: Properties) {
        val configFile = Paths.get("app.properties")
        if (Files.notExists(configFile)) {
            Logger.info("No configuration found. Using default")
            val defaultPropertiesStream = Environment::class.java.getResourceAsStream("/app.default.properties")
            if (defaultPropertiesStream != null) {
                // Write the internal config file to disk
                Files.copy(defaultPropertiesStream, configFile, StandardCopyOption.REPLACE_EXISTING)
                try {
                    defaultPropertiesStream.close()
                } catch (e: IOException) {
                    Logger.warn("Failed to close stream", e)
                }
            } else {
                Logger.error("App default properties is missing is the jar file corrupt?")
                Logger.error("Unable to load default properties")
            }
        } else {
            val inputStream = Files.newInputStream(configFile)
            properties.load(inputStream)
            try {
                inputStream.close()
            } catch (e: IOException) {
                Logger.warn("Failed to close stream", e)
            }
        }
    }
}
