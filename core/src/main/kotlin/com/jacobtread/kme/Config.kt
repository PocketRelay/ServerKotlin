package com.jacobtread.kme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment

@Serializable
data class Config(
    @Comment("The host address to use when listening applied to all servers")
    val host: String = "127.0.0.1",

    @Comment("The level of logging that should be used: INFO,WARN,ERROR,FATAL,DEBUG")
    @SerialName("log_level")
    val logLevel: String = "INFO",

    @Comment("Ports for the different servers")
    val ports: Ports = Ports(),
    @Comment("Settings for the redirect packet")
    @SerialName("redirector_packet")
    val redirectorPacket: RedirectorPacket = RedirectorPacket(),

    @Comment("Database connection info")
    val database: Database = Database(),

    @Comment("OriginDummy")
    val origin: Origin = Origin(),

    val natType: Int = 4,

    @Comment("ME3Data")
    val me3data: Map<String, String> = mapOf("" to ""),

    @Comment(
        """
    The message displayed in the main menu format codes:
    {v}  : KME3 Version
    {n}  : Player Name
    {ip} : Player IP
    """
    )
    @SerialName("menu_message")
    val menuMessage: String = "<font color='#B2B2B2'>KME3</font> - <font color='#FFFF66'>Logged as: {n}</font>",
) {


    @Serializable
    enum class DatabaseType {
        @SerialName("mysql")
        MySQL,

        @SerialName("sqlite")
        SQLite
    }

    @Serializable
    data class MySQLConfig(
        val host: String = "127.0.0.1",
        val port: String = "3306",
        val user: String = "root",
        val password: String = "password",
        val database: String = "kme",
    )

    @Serializable
    data class SQLiteConfig(
        val file: String = "data/app.db",
    )

    @Serializable
    data class Database(
        @Comment("The type of database to use MySQL or SQLite")
        val type: DatabaseType = DatabaseType.MySQL,
        @Comment("Settings for connecting to MySQL database")
        val mysql: MySQLConfig = MySQLConfig(),
        @Comment("Settings used for connecting to SQLite database")
        val sqlite: SQLiteConfig = SQLiteConfig(),
    )


    @Serializable
    data class Ports(
        @Comment("Port for the redirector server")
        val redirector: Int = 42127,
        @Comment("Port for the ticker server")
        val ticker: Int = 8999,
        @Comment("Port for the telemetry server")
        val telemetry: Int = 9988,
        @Comment("Port for the main server")
        val main: Int = 14219,
        @Comment("Port for the http server")
        val http: Int = 80,
    )

    @Serializable
    data class RedirectorPacket(
        val addr: Int = 0x0,
        @Comment("Host to redirect to")
        val host: String = "383933-gosprapp396.ea.com",
        @Comment("Port to redirect to")
        val port: Int = 14219,
        val secu: Int = 0x0,
        val xdns: Int = 0x0,
    )

    @Serializable
    data class Origin(
        val name: String = "OriginPlayer",
        val pid: Int = 0x12345678,
        val uid: Int = 0x12345678,
    )

}