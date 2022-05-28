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
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
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
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.Security
import javax.net.ssl.KeyManagerFactory


fun startRedirector(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup, config: Config) {
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
        val targetPort = config.ports.main
        val listenPort = config.ports.redirector
        val address = lookupServerAddress(config.externalAddress)
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
        Logger.fatal("Unable to lookup server address \"${config.externalAddress}\"", e)
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
class RedirectHandler(val target: ServerAddress, val port: Int) : SimpleChannelInboundHandler<Packet>() {

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet) {
        if (msg.component == Components.REDIRECTOR
            && msg.command == Commands.GET_SERVER_INSTANCE
        ) {
            val channel = ctx.channel()
            val remoteAddress = channel.remoteAddress()
            channel.respond(msg) {
                optional("ADDR", group("VALU") {
                    text("HOST", target.host)
                    number("IP", target.address)
                    number("PORT", port)
                })
                number("SECU", 0x0)
                number("XDNS", 0x0)
            }
            info("Sent redirection to client at $remoteAddress. Closing Connection.")
            channel.close()
        }
    }
}
