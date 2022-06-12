package com.jacobtread.kme.servers

import com.jacobtread.kme.Environment
import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.utils.IPAddress
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.logging.Logger.error
import com.jacobtread.kme.utils.logging.Logger.info
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.FutureListener
import java.io.IOException
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.Security
import javax.net.ssl.KeyManagerFactory

/**
 * startRedirector
 *
 * @param bossGroup The netty boss event loop group
 * @param workerGroup The netty worker event loop group
 */
fun startRedirector(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
    try {
        val listenPort = Environment.Config.ports.redirector
        val handler = RedirectorHandler()
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(handler)
            .bind(listenPort)
            .addListener(handler)
    } catch (e: UnknownHostException) {
        Logger.fatal("Unable to lookup server address \"${Environment.Config.externalAddress}\"", e)
    } catch (e: IOException) {
        error("Exception in redirector server", e)
    }
}

/**
 * RedirectorHandler Handles all logic for channels that are created
 * for the redirector server. This includes SSLv3 decoding/encoding,
 * packet encoding/decoding as well as handling of decoded packets
 *
 * @constructor
 *
 */
@Sharable
class RedirectorHandler : ChannelInboundHandlerAdapter(), FutureListener<Void> {

    /**
     * handlerAdded Handles initialization of the channel when the
     * handler is added to it. Adds the SSL handler along with the
     * packet encoder and decoder
     *
     * @param ctx
     */
    override fun handlerAdded(ctx: ChannelHandlerContext) {
        val channel = ctx.channel()
        channel.pipeline()
            // Add handler for decoding packets
            .addFirst(PacketDecoder())
            // Add handler for decoding SSLv3
            .addFirst(context.newHandler(channel.alloc()))
            // Add handler for encoding packets
            .addLast(PacketEncoder())
    }

    /**
     * channelRead Handles interacting with the result of a read if the
     * read value is a packet then it is handled and if it's a GET_SERVER_INSTANCE
     * REDIRECTOR packet it is responded to with the redirect details
     *
     * @param ctx The channel context
     * @param msg The message that was read
     */
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is Packet) {
            ctx.fireChannelRead(msg)
            return
        }
        val channel = ctx.channel()
        val packet = msg.respond {
            if (msg.component == Components.REDIRECTOR && msg.command == Commands.GET_SERVER_INSTANCE) {
                optional("ADDR", group("VALU") {
                    if (target.isHostname) {
                        text("HOST", target.host)
                    } else {
                        number("IP", target.address)
                    }
                    number("PORT", target.port)
                })
                bool("SECU", false)
                bool("XDNS", false)
                val remoteAddress = channel.remoteAddress()
                info("Sent redirection to client at $remoteAddress. Closing Connection.")
            }
        }
        channel.writeAndFlush(packet)
        channel.close()
    }

    private val target = getRedirectTarget()
    private val context = createSslContext()

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
        val keyStoreStream = RedirectorHandler::class.java.getResourceAsStream("/redirector.pfx")
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

    /**
     * operationComplete For listening to the future of the
     * redirector server bind completion
     *
     * @param future Ignored
     */
    override fun operationComplete(future: Future<Void>) {
        val listenPort = Environment.Config.ports.redirector
        info("Started Redirector on port $listenPort redirecting to:")
        if (target.isHostname) {
            info("Host: ${target.host}")
        } else {
            info("Address: ${target.address}")
        }
        info("Port: ${target.port}")
    }

    /**
     * RedirectTarget Represents the target at which the redirector
     * should redirect clients to
     *
     * @property host The hostname to redirect to (this will be the ip in the case of non host redirects)
     * @property address The encoded IP address for ip redirects or zero if it's a hostname
     * @property port The port to redirect to
     * @property isHostname Whether to use hostname redirection
     * @constructor Create empty RedirectTarget
     */
    data class RedirectTarget(
        val host: String,
        val address: Long,
        val port: Int,
        val isHostname: Boolean,
    )

    /**
     * getRedirectTarget Creates a redirect target based on the
     * external address provided via the config.
     *
     * @return The created redirect target
     */
    private fun getRedirectTarget(): RedirectTarget {
        val config = Environment.Config
        val externalAddress = config.externalAddress
        val targetPort = config.ports.main
        // Regex pattern for matching IPv4 addresses
        val ipv4Regex = Regex("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)(\\.(?!\$)|\$)){4}\$")
        return if (externalAddress.matches(ipv4Regex)) {
            RedirectTarget(
                externalAddress,
                IPAddress.asLong(externalAddress),
                targetPort,
                false
            )
        } else {
            RedirectTarget(externalAddress, 0, targetPort, true)
        }
    }
}