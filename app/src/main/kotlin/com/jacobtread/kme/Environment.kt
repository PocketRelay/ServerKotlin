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
import com.jacobtread.kme.utils.logging.Logger
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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

        val env = System.getenv() // Wrapper over the environment variables

        val properties = Properties()

        // Choose whether to load the config or use the default
        if (!env.booleanValue("KME_ENVIRONMENT_CONFIG", false)) {
            loadProperties(properties) // Load the existing config file
        }

        val unpooledNetty = env.booleanValue("KME_NETTY_UNPOOLED", false)
        if (unpooledNetty) {
            Logger.warn("Netty pooling disabled.")
            System.setProperty("io.netty.allocator.type", "unpooled")
        }

        // Initialize the logger with its configuration
        Logger.init(
            env.stringValue(
                "KME_LOGGER_LEVEL",
                properties.stringOrDefault("logging.level", "info")
            ),
            env.booleanValue(
                "KME_LOGGER_SAVE",
                properties.booleanOrDefault("logging.save", true)
            ),
        )

        val logPackets = env.booleanValue(
            "KME_LOGGER_PACKETS",
            properties.booleanOrDefault("logging.packets", false)
        )

        if (logPackets && Logger.debugEnabled) { // Load command and component names for debugging
            PacketLogger.init(Components, Commands, createBlazeLoggingOutput())
        }

        // External address string
        externalAddress = env.stringValue(
            "KME_EXTERNAL_ADDRESS",
            properties.stringOrDefault("externalAddress", "383933-gosprapp396.ea.com")
        )


        // Server ports
        redirectorPort = env.intValue(
            "KME_REDIRECTOR_PORT",
            properties.intOrDefault("ports.redirector", 42127)
        )
        mainPort = env.intValue(
            "KME_MAIN_PORT",
            properties.intOrDefault("ports.main", 14219)
        )
        discardPort = env.intValue(
            "KME_DISCARD_PORT",
            properties.intOrDefault("ports.discard", 9988)
        )
        httpPort = env.intValue(
            "KME_HTTP_PORT",
            properties.intOrDefault("ports.http", 80)
        )

        // Main menu message string
        menuMessage = env.stringValue(
            "KME_MENU_MESSAGE",
            properties.stringOrDefault("menuMessage", "<font color='#B2B2B2'>KME3</font> - <font color='#FFFF66'>Logged as: {n}</font>")
        )

        // Man in the middle configuration
        mitmEnabled = env.booleanValue(
            "KME_MITM_ENABLED",
            properties.booleanOrDefault("mitm", false)
        )

        // Galaxy at war configuration
        gawReadinessDecay = env.floatValue(
            "KME_GAW_READINESS_DECAY",
            properties.floatOrDefault("gaw.readinessDailyDecay", 0f)
        )
        gawEnabledPromotions = env.booleanValue(
            "KME_GAW_ENABLE_PROMOTIONS",
            properties.booleanOrDefault("gaw.enablePromotions", true)
        )

        // Database configuration
        val databaseType = env.stringValue(
            "KME_DATABASE_TYPE",
            properties.stringOrDefault("database.type", "sqlite")
        ).lowercase()
        database = when (databaseType) {
            "mysql" -> {
                val host = env.stringValue(
                    "KME_MYSQL_HOST",
                    properties.stringOrDefault("database.host", "127.0.0.1")
                )
                val port = env.intValue(
                    "KME_MYSQL_PORT",
                    properties.intOrDefault("database.port", 3306)
                )
                val user = env.stringValue(
                    "KME_MYSQL_USER",
                    properties.stringOrDefault("database.user", "root")
                )
                val password = env.stringValue(
                    "KME_MYSQL_PASSWORD",
                    properties.stringOrDefault("database.password", "password")
                )
                val database = env.stringValue(
                    "KME_MYSQL_DATABASE",
                    properties.stringOrDefault("database.db", "kme")
                )
                MySQLDatabaseAdapter(host, port, user, password, database)
            }
            "sqlite" -> {
                val file = env.stringValue(
                    "KME_SQLITE_FILE",
                    properties.stringOrDefault("database.file", "data/app.db")
                )
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

    private fun Map<String, String>.stringValue(key: String, default: String): String = getOrDefault(key, default)
    private fun Map<String, String>.booleanValue(key: String, default: Boolean): Boolean = get(key)?.toBooleanStrictOrNull() ?: default
    private fun Map<String, String>.intValue(key: String, default: Int): Int = get(key)?.toIntOrNull() ?: default
    private fun Map<String, String>.floatValue(key: String, default: Float): Float = get(key)?.toFloatOrNull() ?: default

    private fun Properties.booleanOrDefault(key: String, default: Boolean): Boolean {
        return when (val value = get(key)) {
            is String -> value.toBooleanStrictOrNull() ?: default
            is Boolean -> value
            else -> default
        }
    }    private fun Properties.stringOrDefault(key: String, default: String): String {
        return when (val value = get(key)) {
            is String -> value
            else -> default
        }
    }

    private fun Properties.intOrDefault(key: String, default: Int): Int {
        return when (val value = get(key)) {
            is String -> value.toIntOrNull() ?: default
            is Int -> value
            else -> default
        }
    }

    private fun Properties.floatOrDefault(key: String, default: Float): Float {
        return when (val value = get(key)) {
            is String -> value.toFloatOrNull() ?: default
            is Float -> value
            else -> default
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