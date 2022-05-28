package com.jacobtread.kme

import com.jacobtread.kme.database.DatabaseConfig
import com.jacobtread.kme.database.DatabaseType
import com.jacobtread.kme.database.MySQLConfig
import com.jacobtread.kme.database.SQLiteConfig
import com.jacobtread.kme.utils.logging.Level
import com.jacobtread.kme.utils.logging.Logger

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


    // Disabled config creation and only uses environment variables
    private const val ENV_CONFIG = "KME_ENVIRONMENT_CONFIG"

    // External address
    private const val EXTERNAL_ADDRESS = "KME_EXTERNAL_ADDRESS"

    // Server ports
    private const val REDIRECTOR_PORT = "KME_REDIRECTOR_PORT"
    private const val MAIN_PORT = "KME_MAIN_PORT"
    private const val TICKER_PORT = "KME_TICKER_PORT"
    private const val TELEMETRY_PORT = "KME_TELEMETRY_PORT"
    private const val HTTP_PORT = "KME_HTTP_PORT"

    // Panel configuration
    private const val PANEL_ENABLED = "KME_PANEL_ENABLED"
    private const val PANEL_USERNAME = "KME_PANEL_USERNAME"
    private const val PANEL_PASSWORD = "KME_PANEL_PASSWORD"

    // Database typ
    private const val DATABASE_TYPE = "KME_DATABASE_TYPE"

    // SQLite database config
    private const val SQLITE_FILE = "KME_SQLITE_FILE"

    // MySQL database config
    private const val MYSQL_HOST = "KME_MYSQL_HOST"
    private const val MYSQL_PORT = "KME_MYSQL_PORT"
    private const val MYSQL_USER = "KME_MYSQL_USER"
    private const val MYSQL_PASSWORD = "KME_MYSQL_PASSWORD"
    private const val MYSQL_DATABASE = "KME_MYSQL_DATABASE"

    // Menu message
    private const val MENU_MESSAGE = "KME_MENU_MESSAGE"

    // Galaxy at war
    private const val GAW_READINESS_DECAY = "KME_GAW_READINESS_DECAY"
    private const val GAW_ENABLE_PROMOTIONS = "KME_GAW_ENABLE_PROMOTIONS"

    private const val LOGGER_LEVEL = "KME_LOGGER_LEVEL"
    private const val LOGGER_SAVE = "KME_LOGGER_SAVE"
    private const val LOGGER_PACKETS = "KME_LOGGER_PACKETS"

    /**
     * recreateConfig Recreates the config based on the environment
     * variables present. The config is immutable so new objects must
     * be created.
     *
     * @param config The original configuration
     * @return The modified environment configuration
     */
    private fun recreateConfig(env: Map<String, String>, config: Config): Config {
        return Config(
            externalAddress = env.str(EXTERNAL_ADDRESS, config.externalAddress),
            ports = createPortsConfig(env, config.ports),
            logging = createLoggingConfig(env, config.logging),
            menuMessage = env.str(MENU_MESSAGE, config.menuMessage),
            database = createDatabaseConfig(env, config.database),
            panel = createPanelConfig(env, config.panel),
            gaw = createGawConfig(env, config.gaw)
        )
    }

    /**
     * createConfig Creates a configuration. If the environment variable ENV_CONFIG
     * is set to true then a config file will not be created and instead ONLY the
     * environment variables will be respected. Otherwise, the loaded config file
     * will be recreated with the environment variable adjustments
     *
     * @return The modified / created configuration
     */
    fun createConfig(): Config {
        val env = System.getenv()
        val initial = if (env.bool(ENV_CONFIG, false)) {
            Config()
        } else {
            loadConfigFile()
        }
        return recreateConfig(env, initial)
    }


    /**
     * createGawConfig Creates a new galaxy at war config from the
     * following environment variables
     * - KME_GAW_READINESS_DECAY
     * - KME_GAW_ENABLE_PROMOTIONS
     *
     * @param env The system environment
     * @param gawConfig The default galaxy at war config
     * @return The modified galaxy at war config
     */
    private fun createGawConfig(env: Map<String, String>, gawConfig: Config.GalaxyAtWarConfig): Config.GalaxyAtWarConfig {
        return Config.GalaxyAtWarConfig(
            env.float(GAW_READINESS_DECAY, gawConfig.readinessDailyDecay),
            env.bool(GAW_ENABLE_PROMOTIONS, gawConfig.enablePromotions)
        )
    }

    /**
     * createPanelConfig Creates a new panel config from the
     * following environment variables:
     * - KME_PANEL_ENABLED
     * - KME_PANEL_USERNAME
     * - KME_PANEL_PASSWORD
     *
     * @param env The system environment
     * @param panelConfig The default panel config
     * @return The modified panel config
     */
    private fun createPanelConfig(env: Map<String, String>, panelConfig: Config.PanelConfig): Config.PanelConfig {
        return Config.PanelConfig(
            enabled = env.bool(PANEL_ENABLED, panelConfig.enabled),
            username = env.str(PANEL_USERNAME, panelConfig.username),
            password = env.str(PANEL_PASSWORD, panelConfig.password),
        )
    }

    /**
     * createPortsConfig Creates a new port config from the
     * following environment variables:
     * - KME_REDIRECTOR_PORT
     * - KME_MAIN_PORT
     * - KME_TICKER_PORT
     * - KME_TELEMETRY_PORT
     * - KME_HTTP_PORT
     *
     * @param env The system environment
     * @param portsConfig The default ports config
     * @return The modified ports config
     */
    private fun createPortsConfig(env: Map<String, String>, portsConfig: Config.Ports): Config.Ports {
        return Config.Ports(
            redirector = env.int(REDIRECTOR_PORT, portsConfig.redirector),
            main = env.int(MAIN_PORT, portsConfig.main),
            ticker = env.int(TICKER_PORT, portsConfig.ticker),
            telemetry = env.int(TELEMETRY_PORT, portsConfig.telemetry),
            http = env.int(HTTP_PORT, portsConfig.http),
        )
    }

    /**
     * createLoggingConfig Creates a new logging config from the
     * following environment variables
     * - KME_LOGGER_LEVEL
     * - KME_LOGGER_SAVE
     * - KME_LOGGER_PACKETS
     *
     * @param env The system environment
     * @param loggingConfig The default logging config
     * @return The modified logging config
     */
    private fun createLoggingConfig(env: Map<String, String>, loggingConfig: Logger.Config): Logger.Config {
        return Logger.Config(
            level = env.env(LOGGER_LEVEL, Level::fromName, loggingConfig.level),
            save = env.bool(LOGGER_SAVE, loggingConfig.save),
            packets = env.bool(LOGGER_PACKETS, loggingConfig.packets)
        )
    }

    /**
     * createDatabaseConfig Creates a new database config from the
     * following environment variables
     * - KME_DATABASE_TYPE
     * - KME_MYSQL_HOST
     * - KME_MYSQL_PORT
     * - KME_MYSQL_USER
     * - KME_MYSQL_PASSWORD
     * - KME_MYSQL_DATABASE
     * - KME_SQLITE_FILE
     *
     * @param env The system environment
     * @param databaseConfig The default database config
     * @return The modified database config
     */
    private fun createDatabaseConfig(env: Map<String, String>, databaseConfig: DatabaseConfig): DatabaseConfig {
        val databaseType = env.env(DATABASE_TYPE, DatabaseType::fromName, databaseConfig.type)
        var mysqlConfig = databaseConfig.mysql
        var sqliteConfig = databaseConfig.sqlite
        when (databaseType) {
            DatabaseType.MYSQL -> {
                mysqlConfig = MySQLConfig(
                    host = env.str(MYSQL_HOST, mysqlConfig.host),
                    port = env.int(MYSQL_PORT, mysqlConfig.port),
                    user = env.str(MYSQL_USER, mysqlConfig.user),
                    password = env.str(MYSQL_PASSWORD, mysqlConfig.password),
                    database = env.str(MYSQL_DATABASE, mysqlConfig.database)
                )
            }
            DatabaseType.SQLITE -> {
                sqliteConfig = SQLiteConfig(
                    file = env.str(SQLITE_FILE, sqliteConfig.file)
                )
            }
        }
        return DatabaseConfig(databaseType, mysqlConfig, sqliteConfig)
    }

    private fun Map<String, String>.str(key: String, default: String): String = getOrDefault(key, default)
    private inline fun <V> Map<String, String>.env(key: String, transform: (String) -> V, default: V): V = get(key)?.let(transform) ?: default
    private fun Map<String, String>.bool(key: String, default: Boolean): Boolean = get(key)?.toBooleanStrictOrNull() ?: default
    private fun Map<String, String>.int(key: String, default: Int): Int = get(key)?.toIntOrNull() ?: default
    private fun Map<String, String>.float(key: String, default: Float): Float = get(key)?.toFloatOrNull() ?: default

}