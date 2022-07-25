package com.jacobtread.kme

import kotlinx.serialization.Serializable

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
    val externalAddress: String = "383933-gosprapp396.ea.com",
    val ports: Ports = Ports(),
    val logging: LoggingConfig = LoggingConfig(),
    val menuMessage: String = "<font color='#B2B2B2'>KME3</font> - <font color='#FFFF66'>Logged as: {n}</font>",
    val database: DatabaseConfig = DatabaseConfig(),
    val panel: PanelConfig = PanelConfig(),
    val gaw: GalaxyAtWarConfig = GalaxyAtWarConfig(),
    val mitm: Boolean = false,
) {


    /**
     * DatabaseConfig Stores configuration information about the database
     *
     * @property type Defines which database connection type to use
     * @property mysql The config for a MySQL database connection
     * @property sqlite The config for a SQLite database connection
     * @constructor Create empty DatabaseConfig
     */
    @Serializable
    data class DatabaseConfig(
        val type: String = "sqlite",
        val host: String = "127.0.0.1",
        val port: Int = 3306,
        val user: String = "root",
        val password: String = "password",
        val database: String = "kme",
        val file: String = "data/app.db",
    )

    @Serializable
    data class LoggingConfig(
        val level: String = "info",
        val save: Boolean = true,
        val packets: Boolean = false,
    )


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
        val redirector: Int = 42127,
        val main: Int = 14219,
        val discard: Int = 9988,
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
        val enabled: Boolean = false,
        val username: String = "admin",
        val password: String = "admin",
    )
}