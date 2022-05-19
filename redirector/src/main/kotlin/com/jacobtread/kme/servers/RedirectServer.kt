@file:JvmName("RedirectServer")

package com.jacobtread.kme.servers

import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.utils.ServerAddress
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.logging.Logger.error
import com.jacobtread.kme.utils.logging.Logger.info
import com.jacobtread.kme.utils.lookupServerAddress
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment
import net.mamoe.yamlkt.Yaml
import java.io.IOException
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.Security
import javax.net.ssl.KeyManagerFactory
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Serializable
data class RedirectorConfig(
    @Comment(
        """
        The port the redirector server will listen on. NOTE: Clients will only
        connect to 42127 so changing this will make users unable to connect unless
        you are behind some sort of proxy that's mapping the port
        """
    )
    val port: Int = 42127,
    @Comment(
        """
        The host to redirect to. If you are hosting the server at a custom domain you
        can use that domain here and it will be used otherwise leave it as default.
        NOTE: If you don't change this domain this value needs to be added to the user's
        hosts file as a redirect 
        NOTE 2: You can specify a direct IP address here instead of a domain 
        """
    )
    val targetHost: String = "383933-gosprapp396.ea.com",
    @Comment(
        """
        The port to redirect to. This is dependant on your configuration of the main server.
        Make sure this matches up with whats in the main config server as the port.
        """
    )
    val targetPort: Int = 14219,
)

/**
 * main Entry point for standalone redirector server will use
 */
fun main() {
    val bossGroup = NioEventLoopGroup()
    val workerGroup = NioEventLoopGroup()
    startRedirector(loadConfig(), bossGroup, workerGroup)
}

private fun loadConfig(): RedirectorConfig {
    val config: RedirectorConfig
    val configFile = Path("redirector.yml")
    if (configFile.exists()) {
        val contents = configFile.readText()
        config = Yaml.decodeFromString(RedirectorConfig.serializer(), contents)
    } else {
        info("No redirector configuration found. Using default")
        config = RedirectorConfig()
        try {
            configFile.writeText(Yaml.encodeToString(config))
        } catch (e: Exception) {
            error("Failed to write newly created redirector config file", e)
        }
    }
    return config
}

/**
 * startRedirector Starts the Redirector server in a new thread. This server
 * handles pointing clients to the correct IP address and port of the main server.
 * This is the only one of the servers that requires SSLv3
 *
 * @param config The configuration for the redirector
 */
fun startRedirector(config: RedirectorConfig, bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
    // Clears the disabled algorithms necessary for SSLv3
    Security.setProperty("jdk.tls.disabledAlgorithms", "")
    val keyStorePassword = charArrayOf('1', '2', '3', '4', '5', '6')
    val keyStoreStream = RedirectHandler::class.java.getResourceAsStream("/redirector.pfx")
        ?: throw IllegalStateException("Missing required keystore for SSLv3")
    val keyStore = KeyStore.getInstance("PKCS12")
    keyStore.load(keyStoreStream, keyStorePassword)
    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    kmf.init(keyStore, keyStorePassword)

    // Create new SSLv3 compatible context
    val context = SslContextBuilder.forServer(kmf)
        .ciphers(listOf("TLS_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_RC4_128_MD5"))
        .protocols("SSLv3", "TLSv1.2", "TLSv1.3")
        .startTls(true)
        .trustManager(InsecureTrustManagerFactory.INSTANCE)
        .build() ?: throw IllegalStateException("Unable to create SSL Context")
    try {
        val targetPort = config.targetPort
        val listenPort = config.port
        val address = lookupServerAddress(config.targetHost)
        val handler = RedirectHandler(address, targetPort)
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                    ch.pipeline()
                        // Add handler for decoding SSLv3
                        .addLast(context.newHandler(ch.alloc()))
                        // Add handler for decoding packet
                        .addLast(PacketDecoder())
                        // Add handler for processing packets
                        .addLast(handler)
                        .addLast(PacketEncoder())
                }
            })
            // Bind the server to the host and port
            .bind(listenPort)
            // Wait for the channel to bind
            .addListener {
                info("Started Redirector on port $listenPort redirecting to:")
                info("Host: ${address.host}")
                info("IP: ${address.ip}")
                info("Port: $targetPort")
            }
    } catch (e: UnknownHostException) {
        Logger.fatal("Unable to lookup server address \"${config.targetHost}\"", e)
    } catch (e: IOException) {
        error("Exception in redirector server", e)
    }
}

/**
 * RedirectHandler a handler shared by all connections
 * that connect to the redirect server. Ignores all packets that aren't
 * REDIRECTOR + GET_SERVER_INSTANCE and when it gets them it sends the
 * client a redirect
 *
 * @constructor Create empty RedirectHandler
 */
@Sharable
class RedirectHandler(target: ServerAddress, port: Int) : SimpleChannelInboundHandler<Packet>() {

    private val packetBody: ByteArray

    init {
        val packetContents = TdfBuilder()
        packetContents.apply {
            union("ADDR", struct("VALU") {
                text("HOST", target.host)
                number("IP", target.address)
                number("PORT", port)
            })
            number("SECU", 0x0)
            number("XDNS", 0x0)
        }
        packetBody = packetContents.createByteArray()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet) {
        if (msg.component == Component.REDIRECTOR
            && msg.command == Command.GET_SERVER_INSTANCE
        ) {
            val channel = ctx.channel()
            val remoteAddress = channel.remoteAddress()
            channel.respond(msg, packetBody)
            info("Sent redirection to client at $remoteAddress. Closing Connection.")
            channel.close()
        }
    }
}
