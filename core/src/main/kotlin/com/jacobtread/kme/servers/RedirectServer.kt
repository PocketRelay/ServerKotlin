package com.jacobtread.kme.servers

import com.jacobtread.kme.Config
import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.utils.IPAddress
import com.jacobtread.kme.logging.Logger.error
import com.jacobtread.kme.logging.Logger.info
import com.jacobtread.kme.utils.customThreadFactory
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
import java.io.IOException
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory

/**
 * startRedirector Starts the Redirector server in a new thread. This server
 * handles pointing clients to the correct IP address and port of the main server.
 * This is the only one of the servers that requires SSLv3
 */
fun startRedirector(
    listenPort: Int,
    targetPort: Int,
) {
    Thread {
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
        val bossGroup = NioEventLoopGroup(customThreadFactory("Redirector Boss #{ID}"))
        val workerGroup = NioEventLoopGroup(customThreadFactory("Redirector Worker #{ID}"))
        val bootstrap = ServerBootstrap() // Create a server bootstrap
        try {
            val handler = RedirectHandler(targetPort)
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
                            .addLast(handler)
                            .addLast(PacketEncoder())
                    }
                })
                // Bind the server to the host and port
                .bind(listenPort)
                // Wait for the channel to bind
                .sync()
            info("Started Redirector Server $listenPort")
            bind.channel()
                // Get the closing future
                .closeFuture()
                // Wait for the closing
                .sync()
        } catch (e: IOException) {
            error("Exception in redirector server", e)
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
 * RedirectHandler a handler shared by all connections
 * that connect to the redirect server. Ignores all packets that aren't
 * REDIRECTOR + GET_SERVER_INSTANCE and when it gets them it sends the
 * client a redirect
 *
 * @constructor Create empty RedirectHandler
 */
@Sharable
class RedirectHandler(private val targetPort: Int) : SimpleChannelInboundHandler<Packet>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet) {
        if (msg.component == Component.REDIRECTOR
            && msg.command == Command.GET_SERVER_INSTANCE
        ) {
            val mainAddress = "127.0.0.1"
            val channel = ctx.channel()
            val remoteAddress = channel.remoteAddress()
            channel.respond(msg) {
                union("ADDR", struct("VALU") {
                    text("HOST", "jacobtread.local" /* Main server host must stay the same */)
                    number("IP", IPAddress.asLong(mainAddress))
                    number("PORT", targetPort)
                })
                number("SECU", 0x0)
                number("XDNS", 0x0)
            }
            info("Sent redirection to client at $remoteAddress. Closing Connection.")
            channel.close()
        }
    }
}
