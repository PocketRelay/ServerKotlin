package com.jacobtread.kme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment

@Serializable
data class Config(
    @Comment("Settings for the redirector server")
    @SerialName("redirector_server")
    val redirectorServer: RedirectorServer = RedirectorServer(),
    @Comment("Settings for the redirect packet")
    @SerialName("redirector_packet")
    val redirectorPacket: RedirectorPacket = RedirectorPacket(),
) {

    @Serializable
    data class RedirectorServer(
        @Comment("Address to listen for connections on")
        val host: String = "0.0.0.0",
        @Comment("Port to listen for connections on")
        val port: Int = 42127,
    )

    @Serializable
    data class RedirectorPacket(
        @Comment("Address of the redirector")
        val addr: String = "127.0.0.1",
        @Comment("Host to redirect to")
        val host: String = "383933-gosprapp396.ea.com",
        @Comment("Port to redirect to")
        val port: Int = 14219,
        val secu: Int = 0x0,
        val xdns: Int = 0x0,
    )
}