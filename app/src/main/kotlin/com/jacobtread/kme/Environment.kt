package com.jacobtread.kme

import com.jacobtread.kme.database.RuntimeDriver
import com.jacobtread.kme.database.createDatabaseTables
import com.jacobtread.kme.utils.logging.Logger
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.sql.Driver
import java.sql.DriverManager
import kotlin.io.path.*
import org.jetbrains.exposed.sql.Database as ExposedDatabase


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
        Logger.info("Starting ME3 Server")

        val env = System.getenv() // Wrapper over the environment variables

        // Choose whether to load the config or use the default
        val config = if (env.booleanValue("KME_ENVIRONMENT_CONFIG", false)) {
            Config() // Generate the default config
        } else {
            loadConfigFile() // Load the existing config file
        }

        val loggingConfig = config.logging

        // Initialize the logger with its configuration
        Logger.init(
            env.stringValue("KME_LOGGER_LEVEL", loggingConfig.level),
            env.booleanValue("KME_LOGGER_SAVE", loggingConfig.save),
            env.booleanValue("KME_LOGGER_PACKETS", loggingConfig.packets)
        )

        // External address string
        externalAddress = env.stringValue("KME_EXTERNAL_ADDRESS", config.externalAddress)


        // Server ports
        val portsConfig = config.ports
        redirectorPort = env.intValue("KME_REDIRECTOR_PORT", portsConfig.redirector)
        mainPort = env.intValue("KME_MAIN_PORT", portsConfig.main)
        discardPort = env.intValue("KME_DISCARD_PORT", portsConfig.discard)
        httpPort = env.intValue("KME_HTTP_PORT", portsConfig.http)

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
            "mysql" -> {
                val version = "8.0.29"
                val url = "https://repo1.maven.org/maven2/mysql/mysql-connector-java/$version/mysql-connector-java-$version.jar"
                setupDatabaseDriver(url, "com.mysql.cj.jdbc.Driver", "mysql.jar")
                val host = env.stringValue("KME_MYSQL_HOST", databaseConfig.host)
                val port = env.intValue("KME_MYSQL_PORT", databaseConfig.port)
                val user = env.stringValue("KME_MYSQL_USER", databaseConfig.user)
                val password = env.stringValue("KME_MYSQL_PASSWORD", databaseConfig.password)
                val database = env.stringValue("KME_MYSQL_DATABASE", databaseConfig.database)
                ExposedDatabase.connect({ DriverManager.getConnection("jdbc:mysql://${host}:${port}/${database}", user, password) })
            }
            "sqlite" -> {
                val version = "3.36.0.3"
                val url = "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/$version/sqlite-jdbc-$version.jar"
                setupDatabaseDriver(url, "org.sqlite.JDBC", "sqlite.jar")
                val file = env.stringValue("KME_SQLITE_FILE", databaseConfig.file)
                val parentDir = Paths.get(file).absolute().parent
                if (parentDir.notExists()) parentDir.createDirectories()
                ExposedDatabase.connect({ DriverManager.getConnection("jdbc:sqlite:$file") })
            }
            "postgres" -> {
                val version = "42.4.0"
                val url = "https://repo1.maven.org/maven2/org/postgresql/postgresql/$version/postgresql-$version.jar"
                setupDatabaseDriver(url, "org.postgresql.Driver", "postgres.jar")
                val host = env.stringValue("KME_MYSQL_HOST", databaseConfig.host)
                val port = env.intValue("KME_MYSQL_PORT", databaseConfig.port)
                val user = env.stringValue("KME_MYSQL_USER", databaseConfig.user)
                val password = env.stringValue("KME_MYSQL_PASSWORD", databaseConfig.password)
                val database = env.stringValue("KME_MYSQL_DATABASE", databaseConfig.database)
                ExposedDatabase.connect({ DriverManager.getConnection("jdbc:postgresql://${host}:${port}/${database}", user, password) })
            }
            else -> Logger.fatal("Unknwon database type: $databaseType (expected mysql, postgres, or sqlite)")
        }
        createDatabaseTables()
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

    /**
     * Setup database driver Downloads the jar file from the provided url if It's
     * not already downloaded then loads the jar file and registers the driver inside
     *
     * @param url The url where the driver can be downloaded from
     * @param clazz The java class for the driver
     * @param fileName The file name for the downloaded jar file
     */
    private fun setupDatabaseDriver(
        url: String,
        clazz: String,
        fileName: String,
    ) {
        val driversPath = Path("drivers")
        if (!driversPath.exists()) driversPath.createDirectories()
        val path = driversPath.resolve(fileName)
        if (path.notExists()) {
            Logger.info("Database driver not installed. Downloading $fileName...")
            try {
                URL(url).openStream().use { input ->
                    path.outputStream(StandardOpenOption.CREATE_NEW).use { output ->
                        input.copyTo(output)
                        Logger.info("Download Completed.")
                    }
                }
            } catch (e: Exception) {
                Logger.fatal("Failed to downlaod database driver", e)
            }
        }
        // Load the jar file and create the wrapped runtime driver
        val classLoader = URLClassLoader.newInstance(arrayOf(path.toUri().toURL()))
        classLoader.loadClass(clazz)
        val driver: Driver = Class.forName(clazz, true, classLoader)
            .getDeclaredConstructor()
            .newInstance() as Driver
        DriverManager.registerDriver(RuntimeDriver(driver))
    }
}