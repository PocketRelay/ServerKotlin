package com.jacobtread.kme

import com.jacobtread.kme.database.DatabaseConfig
import com.jacobtread.kme.utils.logging.Logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment
import net.mamoe.yamlkt.Yaml
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * loadConfigFile Loads the configuration from the YAML config file
 * config.yml if the config file doesn't exist a new one is created
 *
 * @return The loaded config file or default config if none
 */
fun loadConfigFile(): Config {
    val config: Config
    val configFile = Path("main.yml")
    if (configFile.exists()) {
        val contents = configFile.readText()
        config = Yaml.decodeFromString(Config.serializer(), contents)
        Logger.info("Loaded Configuration from: $configFile")
    } else {
        Logger.info("No configuration found. Using default")
        config = Config()
        try {
            Logger.debug("Saving newly created config to: $configFile")
            configFile.writeText(Yaml.encodeToString(config))
        } catch (e: Exception) {
            Logger.error("Failed to write newly created config file", e)
        }
    }
    return config
}

@Serializable
data class Config(
    @Comment("Port for the main server")
    val main: Int = 14219,
    @Comment("Port for the ticker server")
    val ticker: Int = 8999,
    @Comment("Port for the telemetry server")
    val telemetry: Int = 9988,

    val logging: Logger.Config = Logger.Config(),
    @Comment(
        """
    The message displayed in the main menu format codes:
    {v} = version, {n} = player name, {ip} = player ip
    """
    )
    @SerialName("menu_message")
    val menuMessage: String = "<font color='#B2B2B2'>KME3</font> - <font color='#FFFF66'>Logged as: {n}</font>",

    @Comment("Database connection info")
    val database: DatabaseConfig = DatabaseConfig(),
)