package com.jacobtread.kme

import com.jacobtread.kme.database.setupMySQLDatabase
import com.jacobtread.kme.database.setupSQLiteDatabase
import com.jacobtread.kme.utils.logging.Logger
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.path.Path
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Environment This object stores the names of different system
 * environment variables that can control the server as well as
 * providing a function for applying said controls
 *
 * @constructor Create empty Environment
 */
object Environment {

    // The version of KME
    const val KME_VERSION = "1.0.0"

    val externalAddress: String

    val redirectorPort: Int
    val mainPort: Int
    val discardPort: Int
    val httpPort: Int

    val menuMessage: String

    val mitmHost: String
    val mitmPort: Int
    val mitmEnabled: Boolean
    val mitmSecure: Boolean

    val gawReadinessDecay: Float
    val gawEnabledPromotions: Boolean

    val panelEnabled: Boolean
    val panelUsername: String
    val panelPassword: String

    /**
     * Initializes the environment values from the system
     * environment variables as well as the config file if
     * KME_ENVIRONMENT_CONFIG then only the environment
     * variables and default configuration values will be
     * used.
     */
    init {
        Logger.info("Environment Initialized")
        Logger.info("Starting ME3 Server")

        val env = System.getenv() // Wrapper over the environment variables

        // Choose whether to load the config or use the default
        val config = if (env.booleanValue("KME_ENVIRONMENT_CONFIG", false)) {
            Config() // Generate the default config
        } else {
            loadConfigFile() // Load the existing config file
        }

        // External address string
        externalAddress = env.stringValue("KME_EXTERNAL_ADDRESS", config.externalAddress)


        // Server ports
        val portsConfig = config.ports
        redirectorPort = env.intValue("KME_REDIRECTOR_PORT", portsConfig.redirector)
        mainPort = env.intValue("KME_MAIN_PORT", portsConfig.main)
        discardPort = env.intValue("KME_DISCARD_PORT", portsConfig.discard)
        httpPort = env.intValue("KME_HTTP_PORT", portsConfig.http)

        val loggingConfig = config.logging

        // Initialize the logger with its configuration
        Logger.init(
            env.stringValue("KME_LOGGER_LEVEL", loggingConfig.level),
            env.booleanValue("KME_LOGGER_SAVE", loggingConfig.save),
            env.booleanValue("KME_LOGGER_PACKETS", loggingConfig.packets)
        )

        // Main menu message string
        menuMessage = env.stringValue("KME_MENU_MESSAGE", config.menuMessage)

        // Man in the middle configuration
        val mitmConfig = config.mitm
        mitmEnabled = env.booleanValue("KME_MITM_ENABLED", mitmConfig.enabled)
        mitmHost = env.stringValue("KME_MITM_HOST", mitmConfig.host)
        mitmPort = env.intValue("KME_MITM_PORT", mitmConfig.port)
        mitmSecure = env.booleanValue("KME_MITM_SECURE", mitmConfig.secure)

        // Galaxy at war configuration
        val gawConfig = config.gaw
        gawReadinessDecay = env.floatValue("KME_GAW_READINESS_DECAY", gawConfig.readinessDailyDecay)
        gawEnabledPromotions = env.booleanValue("KME_GAW_ENABLE_PROMOTIONS", gawConfig.enablePromotions)

        // Panel configuration
        val panelConfig = config.panel
        panelEnabled = env.booleanValue("KME_PANEL_ENABLED", panelConfig.enabled)
        panelUsername = env.stringValue("KME_PANEL_USERNAME", panelConfig.username)
        panelPassword = env.stringValue("KME_PANEL_PASSWORD", panelConfig.password)

        // Database configuration
        val databaseConfig = config.database
        val databaseType = env.stringValue("KME_DATABASE_TYPE", databaseConfig.type)
            .lowercase();
        when (databaseType) {
            "mysql" -> setupMySQLDatabase(
                env.stringValue("KME_MYSQL_HOST", databaseConfig.host),
                env.intValue("KME_MYSQL_PORT", databaseConfig.port),
                env.stringValue("KME_MYSQL_USER", databaseConfig.user),
                env.stringValue("KME_MYSQL_PASSWORD", databaseConfig.password),
                env.stringValue("KME_MYSQL_DATABASE", databaseConfig.database)
            )
            "sqlite" -> setupSQLiteDatabase(env.stringValue("KME_SQLITE_FILE", databaseConfig.file))
        }

        System.gc() // Clean up all the created objects
    }

    private fun Map<String, String>.stringValue(key: String, default: String): String = getOrDefault(key, default)
    private fun Map<String, String>.booleanValue(key: String, default: Boolean): Boolean = get(key)?.toBooleanStrictOrNull() ?: default
    private fun Map<String, String>.intValue(key: String, default: Int): Int = get(key)?.toIntOrNull() ?: default
    private fun Map<String, String>.floatValue(key: String, default: Float): Float = get(key)?.toFloatOrNull() ?: default

    /**
     * loadConfigFile Loads the configuration from the JSON config file
     * config.yml if the config file doesn't exist a new one is created
     *
     * @return The loaded config file or default config if none
     */
    private fun loadConfigFile(): Config {
        val json = Json {
            encodeDefaults = true
            prettyPrint = true
        }
        val configFile = Path("config.json") // The path to the config file
        if (configFile.notExists()) { // Check the config file doesn't exsit
            // Config file doesn't exist
            Logger.info("No configuration found. Using default")
            val config = Config() // Create default config
            try {
                // Write config to config file
                Logger.debug("Saving newly created config to: $configFile")
                configFile.writeText(json.encodeToString(config))
            } catch (e: Exception) {
                Logger.error("Failed to write newly created config file", e)
            }
            return config
        }
        val contents = configFile.readText()
        return json.decodeFromString(contents)
    }
}