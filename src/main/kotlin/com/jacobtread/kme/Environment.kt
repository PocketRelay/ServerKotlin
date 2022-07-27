package com.jacobtread.kme

import com.jacobtread.blaze.PacketLogger
import com.jacobtread.blaze.debug.BlazeLoggingOutput
import com.jacobtread.kme.data.Commands
import com.jacobtread.kme.data.Components
import com.jacobtread.kme.data.Constants
import com.jacobtread.kme.database.adapter.DatabaseAdapter
import com.jacobtread.kme.database.adapter.sql.MySQLDatabaseAdapter
import com.jacobtread.kme.database.adapter.sql.SQLiteDatabaseAdapter
import com.jacobtread.kme.exceptions.DatabaseException
import com.jacobtread.kme.logging.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.Security
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.notExists

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
    val discardPort: Int
    val httpPort: Int

    val menuMessage: String

    val mitmEnabled: Boolean

    val gawReadinessDecay: Float
    val gawEnabledPromotions: Boolean

    /**
     * Initializes the environment values from the system
     * environment variables as well as the config file if
     * KME_ENVIRONMENT_CONFIG then only the environment
     * variables and default configuration values will be
     * used.
     */
    init {
        Logger.info("Starting KME3 server (version: ${Constants.KME_VERSION})")

        val env = Settings(
            System.getenv(),
            Properties()
        )

        // Choose whether to load the config or use the default
        if (!env.booleanValue("KME_ENVIRONMENT_CONFIG", null, false)) {
            loadProperties(env.properties) // Load the existing config file
        }

        val unpooledNetty = env.booleanValue("KME_NETTY_UNPOOLED", "netty.unpooled", false)
        if (unpooledNetty) {
            Logger.warn("Netty pooling disabled.")
            System.setProperty("io.netty.allocator.type", "unpooled")
        }
        // Clears the disabled algorithms. This is necessary for SSLv3
        Security.setProperty("jdk.tls.disabledAlgorithms", "")

        // Initialize the logger with its configuration
        Logger.init(
            env.stringValue("KME_LOGGER_LEVEL", "logging.level", "info"),
            env.booleanValue("KME_LOGGER_SAVE", "logging.save", true)
        )

        val logPackets = env.booleanValue("KME_LOGGER_PACKETS", "logging.packets", false)

        if (logPackets && Logger.debugEnabled) { // Load command and component names for debugging
            PacketLogger.init(Components, Commands, createBlazeLoggingOutput())
        }

        // External address string
        externalAddress = env.stringValue("KME_EXTERNAL_ADDRESS", "externalAddress", "383933-gosprapp396.ea.com")

        // Server ports
        redirectorPort = env.intValue("KME_REDIRECTOR_PORT", "ports.redirector", 42127)
        mainPort = env.intValue("KME_MAIN_PORT", "ports.main", 14219)
        discardPort = env.intValue("KME_DISCARD_PORT", "ports.discard", 9988)
        httpPort = env.intValue("KME_HTTP_PORT", "ports.http", 80)


        // Main menu message string
        menuMessage = env.stringValue("KME_MENU_MESSAGE", "menuMessage", "<font color='#B2B2B2'>KME3</font> - <font color='#FFFF66'>Logged as: {n}</font>")


        // Man in the middle configuration
        mitmEnabled = env.booleanValue("KME_MITM_ENABLED", "mitm", false)


        // Galaxy at war configuration
        gawReadinessDecay = env.floatValue("KME_GAW_READINESS_DECAY", "gaw.readinessDailyDecay", 0f)
        gawEnabledPromotions = env.booleanValue("KME_GAW_ENABLE_PROMOTIONS", "gaw.enablePromotions", true)


        // Database configuration
        val databaseType = env.stringValue("KME_DATABASE_TYPE", "database.type", "sqlite")
            .lowercase()
        database = when (databaseType) {
            "mysql" -> {
                val host = env.stringValue("KME_MYSQL_HOST", "database.host", "127.0.0.1")
                val port = env.intValue("KME_MYSQL_PORT", "database.port", 3306)
                val user = env.stringValue("KME_MYSQL_USER", "database.user", "root")
                val password = env.stringValue("KME_MYSQL_PASSWORD", "database.password", "password")
                val database = env.stringValue("KME_MYSQL_DATABASE", "database.db", "kme")
                MySQLDatabaseAdapter(host, port, user, password, database)
            }

            "sqlite" -> {
                val file = env.stringValue("KME_SQLITE_FILE", "database.file", "data/app.db")
                SQLiteDatabaseAdapter(file)
            }

            else -> Logger.fatal("Unknwon database type: $databaseType (expected mysql, or sqlite)")
        }
        try {
            database.setup()
        } catch (e: DatabaseException) {
            Logger.fatal("Failed to setup database", e)
        }
    }

    private fun createBlazeLoggingOutput(): BlazeLoggingOutput {
        return object : BlazeLoggingOutput {
            override fun debug(text: String) {
                Logger.debug(text)
            }

            override fun warn(text: String) {
                Logger.warn(text)
            }

            override fun warn(text: String, cause: Throwable) {
                Logger.warn(text, cause)
            }

            override fun error(text: String) {
                Logger.error(text)
            }

            override fun error(text: String, cause: Throwable) {
                Logger.error(text, cause)
            }
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
        val configFile = Path("app.properties")
        if (configFile.notExists()) {
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
            val inputStream = configFile.inputStream()
            properties.load(inputStream)
            try {
                inputStream.close()
            } catch (e: IOException) {
                Logger.warn("Failed to close stream", e)
            }
        }
    }
}