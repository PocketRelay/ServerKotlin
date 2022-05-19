@file:JvmName("App")

package com.jacobtread.kme

import com.jacobtread.kme.database.DatabaseConfig
import com.jacobtread.kme.database.startDatabase
import com.jacobtread.kme.servers.startDiscardServer
import com.jacobtread.kme.servers.startHttpServer
import com.jacobtread.kme.servers.startMainServer
import com.jacobtread.kme.utils.customThreadFactory
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.nameThread
import io.netty.channel.nio.NioEventLoopGroup
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment
import net.mamoe.yamlkt.Yaml
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

// The version of KME
const val KME_VERSION = "1.0.0"

@Serializable
data class Config(
    @Comment("Port for the main server")
    val main: Int = 14219,
    @Comment("Port for the ticker server")
    val ticker: Int = 8999,
    @Comment("Port for the telemetry server")
    val telemetry: Int = 9988,

    @Comment(
        """
        This is the address used for telling the clients where the telemetry and ticker servers are located.
        This should be the same address that the client's put into their redirector servers.
    """
    )
    val address: String = "383933-gosprapp396.ea.com",

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


fun main() {
    val config = loadConfigFile()
    startMainServerGroup(config)
}

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

fun startMainServerGroup(config: Config) {

    Logger.init(config.logging)
    Logger.info("Starting ME3 Server")

    val bossGroup = NioEventLoopGroup(customThreadFactory("Netty Boss #{ID}"))
    val workerGroup = NioEventLoopGroup(customThreadFactory("Netty Worker #{ID}"))

    startDatabase(config.database)

    // Telemetry & Ticker servers just discard any contents they receive
    startDiscardServer("Telemetry", config.telemetry, bossGroup, workerGroup)
    startDiscardServer("Ticker", config.ticker, bossGroup, workerGroup)

    startHttpServer(bossGroup, workerGroup)
    startMainServer(bossGroup, workerGroup, config)
}
