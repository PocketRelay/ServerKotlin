package com.jacobtread.kme.servers

import com.jacobtread.kme.Config
import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.logging.Logger.error
import com.jacobtread.kme.utils.logging.Logger.info
import com.jacobtread.kme.utils.lookupServerAddress
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
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

fun startRedirector(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup, config: Config) {
    try {
        val listenPort = config.ports.redirector
        val initializer = RedirectorHandler(config)
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(initializer)
            .bind(listenPort)
            .addListener(initializer)
    } catch (e: UnknownHostException) {
        Logger.fatal("Unable to lookup server address \"${config.externalAddress}\"", e)
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
 * @param config The server configuration
 */
@Sharable
class RedirectorHandler(private val config: Config) : ChannelInitializer<Channel>(), FutureListener<Void> {

    private val targetAddress = lookupServerAddress(config.externalAddress)
    private val targetPort = config.ports.main
    private val context = createSslContext()

    /**
     * initChannel Handles channel initialization this adds the
     * encoders and decoders for both SSL and packets
     *
     * @param ch The channel to initialize
     */
    override fun initChannel(ch: Channel) {
        ch.pipeline()
            // Add handler for decoding SSLv3
            .addLast(context.newHandler(ch.alloc()))
            // Add handler for decoding packets
            .addLast(PacketDecoder())
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
        val listenPort = config.ports.redirector
        info("Started Redirector on port $listenPort redirecting to:")
        info("Host: ${targetAddress.host}")
        info("IP: ${targetAddress.ip}")
        info("Port: $targetPort")
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
        if (msg !is Packet) return
        val channel = ctx.channel()
        channel.respond(msg) {
            if (msg.component == Components.REDIRECTOR && msg.command == Commands.GET_SERVER_INSTANCE) {
                optional("ADDR", group("VALU") {
                    if (targetAddress.isHostname) {
                        text("HOST", targetAddress.host)
                    }
                    number("IP", targetAddress.address)
                    number("PORT", targetPort)
                })
                bool("SECU", false)
                bool("XDNS", false)
                val remoteAddress = channel.remoteAddress()
                info("Sent redirection to client at $remoteAddress. Closing Connection.")
            }
        }
        channel.close()
    }
}