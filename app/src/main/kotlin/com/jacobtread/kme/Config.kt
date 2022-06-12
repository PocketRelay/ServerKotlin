package com.jacobtread.kme

import com.jacobtread.kme.database.DatabaseConfig
import com.jacobtread.kme.utils.logging.Logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment

/**
 * Config the configuration for this application
 *
 * @property externalAddress The address that clients will use to connect to this server. This address should point to
 * the machine that this server is hosted on and must be accessible to the clients. If you aren't using a custom
 * domain then the default domain should be added to the system hosts file
 * @property ports The different ports that all the different servers should use
 * @property logging The configuration for the logger
 * @property menuMessage The message that should be displayed on the main menu
 * @property database The database configuration
 * @property panel The configuration for the web panel authentication
 * @property gaw The Galaxy At War configuration
 * @constructor Create empty Config
 */
@Serializable
data class Config(
    @Comment(
        """
        This is the address used for telling the clients where the servers are located.
        This should be the same address that the client's put into their setup.
    """
    )
    @SerialName("external_address")
    val externalAddress: String = "383933-gosprapp396.ea.com",

    @Comment("The ports for each sub server")
    val ports: Ports = Ports(),

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

    @Comment("Authentication for the web manager panel")
    val panel: PanelConfig = PanelConfig(),

    @Comment("Galaxy At War config")
    val gaw: GalaxyAtWarConfig = GalaxyAtWarConfig(),
) {
    /**
     * Ports Configuration for the different ports that the servers will use
     *
     * NOTE: Telemetry and ticker servers have been merged into one single
     * discard server because they are unused. Original ports were
     * ticker = 8999, telemetry = 9988
     *
     * @property redirector The port for the redirector server
     * @property main The port for the main server
     * @property discard The port for the ticker & telemetry server
     * @property http The port for the http server
     * @constructor Create empty Ports
     */
    @Serializable
    data class Ports(
        @Comment(
            """
        The port the redirector server will listen on. NOTE: Clients will only
        connect to 42127 so changing this will make users unable to connect unless
        you are behind some sort of proxy that's mapping the port
        """
        )
        val redirector: Int = 42127,
        @Comment("Port for the main server")
        val main: Int = 14219,
        @Comment("Port for the ticker & telemetry discard server")
        val discard: Int = 9988,
        @Comment("Port for the http server")
        val http: Int = 80,
    )


    /**
     * GalaxyAtWarConfig The configuration of the Galaxy at War
     *
     * @property readinessDailyDecay The amount of readiness level to decay each day from last update 0.5 = -1%
     * Set this value defaults to 0 for no decay
     * @property enablePromotions Whether to include promotions in the galaxy at war level
     * @constructor Create empty GalaxyAtWarConfig
     */
    @Serializable
    data class GalaxyAtWarConfig(
        @Comment(
            """
        The amount of readiness level to decay each day from last update 0.5 = -1%.
        Set this value defaults to 0 for no decay
    """
        )
        val readinessDailyDecay: Float = 0f,
        val enablePromotions: Boolean = true,
    )

    /**
     * PanelConfig The configuration for the web panel
     *
     * @property enabled Whether the panel is enabled or not
     * @property username The panel username
     * @property password The panel password
     * @constructor Create empty PanelConfig
     */
    @Serializable
    data class PanelConfig(
        val enabled: Boolean = true,
        val username: String = "admin",
        val password: String = "admin",
    )
}