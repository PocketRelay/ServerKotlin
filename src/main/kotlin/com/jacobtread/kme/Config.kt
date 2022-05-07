package com.jacobtread.kme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment

@Serializable
data class Config(
    @Comment("The host address to use when listening applied to all servers")
    val host: String = "0.0.0.0",

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

    @Comment("Telemetry client config")
    val telemetry: TelemetryConfig = TelemetryConfig()
) {

    @Serializable
    data class Database(
        @Comment("The database host address")
        val host: String = "127.0.0.1",
        @Comment("The database port")
        val port: String = "3306",
        @Comment("The database account username")
        val user: String = "root",
        @Comment("The database account password")
        val password: String = "password",
        @Comment("The database to use")
        val database: String = "kme",
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
        @Comment("The address to redirect to")
        val ip: String = "127.0.0.1",
        @Comment("Port to redirect to")
        val port: Int = 14219,
        val secu: Int = 0x0,
        val xdns: Int = 0x0,
    )

    @Serializable
    data class TelemetryConfig(
        val anon: Int = 0x0,
        val disabled: List<String> = listOf(
            "AD", "AF", "AG", "AI", "AL", "AM", "AN", "AO", "AQ", "AR", "AS",
            "AW", "AX", "AZ", "BA", "BB", "BD","BF", "BH", "BI", "BJ", "BM",
            "BN", "BO", "BR", "BS", "BT", "BV", "BW", "BY", "BZ", "CC", "CD",
            "CF", "CG", "CI", "CK", "CL", "CM", "CN", "CO", "CR", "CU", "CV",
            "CX", "DJ", "DM", "DO", "DZ", "EC", "EG", "EH", "ER", "ET", "FJ",
            "FK", "FM", "FO", "GA", "GD", "GE", "GF", "GG", "GH", "GI", "GL",
            "GM", "GN", "GP", "GQ", "GS", "GT", "GU", "GW", "GY", "HM", "HN",
            "HT", "ID", "IL", "IM", "IN", "IO", "IQ", "IR", "IS", "JE", "JM",
            "JO", "KE", "KG", "KH", "KI", "KM", "KN", "KP", "KR", "KW", "KY",
            "KZ", "LA", "LB", "LC", "LI", "LK", "LR", "LS", "LY", "MA", "MC",
            "MD",
            "ME",
            "MG",
            "MH",
            "ML",
            "MM",
            "MN",
            "MO",
            "MP",
            "MQ",
            "MR",
            "MS",
            "MU",
            "MV",
            "MW",
            "MY",
            "MZ",
            "NA",
            "NC",
            "NE",
            "NF",
            "NG",
            "NI",
            "NP",
            "NR",
            "NU",
            "OM",
            "PA",
            "PE",
            "PF",
            "PG",
            "PH",
            "PK",
            "PM",
            "PN",
            "PS",
            "PW",
            "PY",
            "QA",
            "RE",
            "RS",
            "RW",
            "SA",
            "SB",
            "SC",
            "SD",
            "SG",
            "SH",
            "SJ",
            "SL",
            "SM",
            "SN",
            "SO",
            "SR",
            "ST",
            "SV",
            "SY",
            "SZ",
            "TC",
            "TD",
            "TF",
            "TG",
            "TH",
            "TJ",
            "TK",
            "TL",
            "TM",
            "TN",
            "TO",
            "TT",
            "TV",
            "TZ",
            "UA",
            "UG",
            "UM",
            "UY",
            "UZ",
            "VA",
            "VC",
            "VE",
            "VG",
            "VN",
            "VU",
            "WF",
            "WS",
            "YE",
            "YT",
            "ZM",
            "ZW",
            "ZZ",
            ),

        )
}