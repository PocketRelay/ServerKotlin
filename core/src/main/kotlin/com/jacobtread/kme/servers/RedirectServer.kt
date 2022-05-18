package com.jacobtread.kme.servers

import com.jacobtread.kme.Config
import com.jacobtread.kme.LOGGER
import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.utils.IPAddress
import com.jacobtread.kme.utils.customThreadFactory
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.io.IOException
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory

/**
 * startRedirector Starts the Redirector server in a new thread
 *
 * @param config The server configuration
 */
fun startRedirector(config: Config) {
    Thread {
        val keyStorePassword = charArrayOf('1', '2', '3', '4', '5', '6')
        val keyStoreStream = RedirectClient::class.java.getResourceAsStream("/redirector.pfx")
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
        val bossGroup = NioEventLoopGroup(customThreadFactory("Redirector Boss #{ID}"))
        val workerGroup = NioEventLoopGroup(customThreadFactory("Redirector Worker #{ID}"))
        val bootstrap = ServerBootstrap() // Create a server bootstrap
        try {
            val bind = bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<Channel>() {
                    override fun initChannel(ch: Channel) {
                        ch.pipeline()
                            // Add handler for decoding SSLv3
                            .addLast(context.newHandler(ch.alloc()))
                            // Add handler for decoding packet
                            .addLast(PacketDecoder())
                            // Add handler for processing packets
                            .addLast(RedirectClient(config))
                            .addLast(PacketEncoder())
                    }
                })
                // Bind the server to the host and port
                .bind(config.host, config.ports.redirector)
                // Wait for the channel to bind
                .sync()
            LOGGER.info("Started Redirector Server (${config.host}:${config.ports.redirector})")
            bind.channel()
                // Get the closing future
                .closeFuture()
                // Wait for the closing
                .sync()
        } catch (e: IOException) {
            LOGGER.error("Exception in redirector server", e)
        }
    }.apply {
        // Name the redirector thread
        name = "Redirector"
        // Close this thread when the JVM requests close
        isDaemon = true
        // Start the redirector thread
        start()
    }
}


/**
 * RedirectClient Creates a client that handles the redirect handshake
 * to direct the client to the desired main server
 *
 * @property config The config for redirection packets
 * @constructor Create empty RedirectClient
 */
private class RedirectClient(private val config: Config) : SimpleChannelInboundHandler<RawPacket>() {

    /**
     * channelRead0 Handles incoming RawPackets and sends back a redirect packet
     * when the REDIRECTOR + REQUEST_REDIRECT packet is received
     *
     * @param ctx
     * @param msg
     */
    override fun channelRead0(ctx: ChannelHandlerContext, msg: RawPacket) {
        if (msg.component == Component.REDIRECTOR
            && msg.command == Command.GET_SERVER_INSTANCE
        ) {
            val platform = msg.getValueOrNull(StringTdf::class, "PLAT")
            val channel = ctx.channel()
            val remoteAddress = channel.remoteAddress()
            LOGGER.info("Sending redirection to client ($remoteAddress) -> on platform ${platform ?: "Unknown"}")

            val redirectorConfig = config.redirectorPacket

            // Create a redirect response packet for the client
            val packet = respond(msg) {
                union("ADDR",
                    redirectorConfig.addr,
                    struct("VALU") {
                        text("HOST", redirectorConfig.host)
                        number("IP", IPAddress.asLong(config.host))
                        number("PORT", config.ports.main)
                    }
                )
                number("SECU", redirectorConfig.secu)
                number("XDNS", redirectorConfig.xdns)
            }

            // Write the packet, flush and then close the channel
            channel.write(packet)
            channel.flush()
            channel.close()
            LOGGER.info("Terminating connection to $remoteAddress (Finished redirect)")
        }
    }
}
