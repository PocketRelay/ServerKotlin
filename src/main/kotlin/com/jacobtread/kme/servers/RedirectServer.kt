package com.jacobtread.kme.servers

import com.jacobtread.blaze.group
import com.jacobtread.blaze.logging.PacketLogger
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.packet.Packet.Companion.addPacketHandlers
import com.jacobtread.blaze.respond
import com.jacobtread.kme.Environment
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.utils.getIPv4Encoded
import com.jacobtread.kme.utils.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.DecoderException
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLException

/**
 * startRedirector
 *
 * @param bossGroup The netty boss event loop group
 * @param workerGroup The netty worker event loop group
 */
fun startRedirector(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
    try {
        val listenPort = Environment.redirectorPort
        val handler = RedirectorHandler()
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(handler)
            .bind(listenPort)
            .sync()
        Logger.info("Started Redirector on port ${Environment.redirectorPort}")
    } catch (e: IOException) {
        val reason = e.message ?: e.javaClass.simpleName
        Logger.fatal("Unable to start redirector server: $reason")
    }
}


/**
 * This handler checks incoming packets to see if they are
 * [Components.REDIRECTOR] / [Commands.GET_SERVER_INSTANCE]
 * and if they are it sends them the connection details of
 * the main server (i.e. The host address / ip and port)
 *
 * @constructor Creates a new redirector handler
 */
class RedirectorHandler : ChannelInboundHandlerAdapter() {

    private val targetAddress: ULong
    private val isHostname: Boolean
    private val context = createServerSslContext()

    init {
        val externalAddress = Environment.externalAddress
        targetAddress = getIPv4Encoded(externalAddress)
        isHostname = targetAddress == 0uL
    }

    override fun isSharable(): Boolean = true

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        val channel = ctx.channel()
        val remoteAddress = channel.remoteAddress()
        Logger.debug("Connection at $remoteAddress to Redirector Server")
        channel.addPacketHandlers(context)
        PacketLogger.setEnabled(channel, true)
    }

    /**
     * Handles interacting with the result of a read if the
     * read value is a packet then it is handled and if it's a GET_SERVER_INSTANCE
     * REDIRECTOR packet it is responded to with the redirect details
     *
     * @param ctx The channel context
     * @param msg The message that was read
     */
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is Packet) {
            val response = msg.respond {
                if (msg.component == Components.REDIRECTOR && msg.command == Commands.GET_SERVER_INSTANCE) {
                    optional("ADDR", group("VALU") {
                        if (isHostname) {
                            text("HOST", Environment.externalAddress)
                        } else {
                            number("IP", targetAddress)
                        }
                        number("PORT", Environment.mainPort)
                    })
                    // Determines if SSLv3 should be used when connecting to the main server
                    bool("SECU", false)
                    bool("XDNS", false)
                }
            }
            ctx.writeAndFlush(response)
                .addListener(ChannelFutureListener.CLOSE)
            Packet.release(msg)
        } else {
            ctx.fireChannelRead(msg)
        }
    }

    @Suppress("OVERRIDE_DEPRECATION") // Not actually depreciated.
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        if (cause == null) return
        val channel = ctx.channel()
        val ipAddress = channel.remoteAddress()
        if (cause is DecoderException) {
            val underlying = cause.cause
            if (underlying != null) {
                if (underlying is SSLException) {
                    Logger.debug("Connection at $ipAddress tried to connect without vaid SSL (Did someone try to connect with a browser?)")
                    ctx.close()
                    return
                }
            }
        } else if (cause is IOException) {
            val message = cause.message
            if (message != null && message.startsWith("Connection reset")) {
                Logger.debug("Connection to client at $ipAddress lost")
                return
            }
        }
        ctx.close()
    }


    /**
     * Creates a new [SslContext] for Netty to create SslHandlers from
     * so that we can accept the SSLv3 traffic. Any exceptions
     *
     * @return The created [SslContext]
     */
    private fun createServerSslContext(): SslContext {
        try {
            val keyStorePassword = charArrayOf('1', '2', '3', '4', '5', '6')
            val keyStoreStream = RedirectorHandler::class.java.getResourceAsStream("/redirector.pfx")
            checkNotNull(keyStoreStream) { "Missing required keystore for SSLv3" }
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStoreStream.use {
                keyStore.load(keyStoreStream, keyStorePassword)
            }
            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(keyStore, keyStorePassword)

            // Create new SSLv3 compatible context
            val context = SslContextBuilder.forServer(kmf)
                .ciphers(listOf("TLS_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_RC4_128_MD5"))
                .protocols("SSLv3", "TLSv1.2", "TLSv1.3")
                .startTls(true)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()
            checkNotNull(context) { "Unable to create SSL Context" }
            return context
        } catch (e: SSLException) {
            Logger.fatal("Failed to create SSLContext for redirector", e)
        } catch (e: GeneralSecurityException) {
            Logger.fatal("Failed to create SSLContext for redirector", e)
        }
    }
}
