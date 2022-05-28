@file:JvmName("RedirectServer")

package com.jacobtread.kme.servers

import com.jacobtread.kme.Config
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
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.io.IOException
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.Security
import javax.net.ssl.KeyManagerFactory


fun startRedirector(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup, config: Config) {
    try {
        val listenPort = config.ports.redirector
        val targetAddress = lookupServerAddress(config.externalAddress)
        val targetPort = config.ports.main
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(RedirectorInitializer(targetAddress, targetPort))
            // Bind the server to the host and port
            .bind(listenPort)
            // Wait for the channel to bind
            .addListener {
                info("Started Redirector on port $listenPort redirecting to:")
                info("Host: ${targetAddress.host}")
                info("IP: ${targetAddress.ip}")
                info("Port: $targetPort")
            }
    } catch (e: UnknownHostException) {
        Logger.fatal("Unable to lookup server address \"${config.externalAddress}\"", e)
    } catch (e: IOException) {
        error("Exception in redirector server", e)
    }
}

/**
 * RedirectorInitializer Sets up channels for the redirector server. Creates
 * a SSLv3 context and applies that along with packet decoding, encoding, and
 *
 *
 * @constructor
 *
 * @param targetAddress The target address that should be redirected to
 * @param targetPort The target port that should be redirected to
 */
class RedirectorInitializer(targetAddress: ServerAddress, targetPort: Int) : ChannelInitializer<Channel>() {

    private val context = createSslContext()
    private val handler = RedirectHandler(targetAddress, targetPort)

    override fun initChannel(ch: Channel) {
        ch.pipeline()
            // Add handler for decoding SSLv3
            .addLast(context.newHandler(ch.alloc()))
            // Add handler for decoding packets
            .addLast(PacketDecoder())
            // Add handler for processing packets
            .addLast(handler)
            // Add handler for encoding packets
            .addLast(PacketEncoder())
    }

    /**
     * createSslContext Creates an SSLv3 capable context for the
     * redirector server
     *
     * @return The created context
     */
    private fun createSslContext(): SslContext {
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
        return SslContextBuilder.forServer(kmf)
            .ciphers(listOf("TLS_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_RC4_128_MD5"))
            .protocols("SSLv3", "TLSv1.2", "TLSv1.3")
            .startTls(true)
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build() ?: throw IllegalStateException("Unable to create SSL Context")
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
class RedirectHandler(
    private val target: ServerAddress,
    private val port: Int,
) : SimpleChannelInboundHandler<Packet>() {

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet) {
        val channel = ctx.channel()
        if (msg.component == Components.REDIRECTOR
            && msg.command == Commands.GET_SERVER_INSTANCE
        ) {
            val remoteAddress = channel.remoteAddress()
            channel.respond(msg) {
                optional("ADDR", group("VALU") {
                    if (target.isHostname) {
                        text("HOST", target.host)
                    }
                    number("IP", target.address)
                    number("PORT", port)
                })
                bool("SECU", false)
                bool("XDNS", false)
            }
            info("Sent redirection to client at $remoteAddress. Closing Connection.")
            channel.close()
        } else {
            channel.respond(msg)
        }
    }
}
