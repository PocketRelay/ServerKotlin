package com.jacobtread.kme.servers

import com.jacobtread.blaze.PacketDecoder
import com.jacobtread.blaze.PacketEncoder
import com.jacobtread.blaze.group
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.respond
import com.jacobtread.kme.Environment
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.logging.Logger.error
import com.jacobtread.kme.utils.logging.Logger.info
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.DecoderException
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.io.IOException
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.Security
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
            .addListener {
                info("Started Redirector on port ${Environment.redirectorPort} redirecting to:")
                info("Host: ${Environment.externalAddress}")
                info("Port: ${Environment.mainPort}")
            }
    } catch (e: UnknownHostException) {
        Logger.fatal("Unable to lookup server address \"${Environment.externalAddress}\"", e)
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
class RedirectorHandler : ChannelInboundHandlerAdapter() {

    private val context = createSslContext()

    private val targetAddress: ULong
    private val isHostname: Boolean

    init {
        val externalAddress = Environment.externalAddress
        // Regex pattern for matching IPv4 addresses
        val ipv4Regex = Regex("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)(\\.(?!\$)|\$)){4}\$")
        if (externalAddress.matches(ipv4Regex)) { // Check if the address is an IPv4 Address
            val ipParts = externalAddress.split('.', limit = 4) // Split the address into 4 parts
            require(ipParts.size == 4) { "Invalid IPv4 Address" } // Ensure that the address is 4 parts
            // Encoding the address as an unsigned long value
            val ipEncoded: ULong = (ipParts[0].toULong() shl 24)
                .or(ipParts[1].toULong() shl 16)
                .or(ipParts[2].toULong() shl 8)
                .or(ipParts[3].toULong())

            targetAddress = ipEncoded
            isHostname = false
        } else {
            targetAddress = 0u
            isHostname = true
        }
    }

    /**
     * handlerAdded Handles initialization of the channel when the
     * handler is added to it. Adds the SSL handler along with the
     * packet encoder and decoder
     *
     * @param ctx
     */
    override fun handlerAdded(ctx: ChannelHandlerContext) {
        val channel = ctx.channel()

        val remoteAddress = channel.remoteAddress()
        Logger.debug("Connection at $remoteAddress to Redirector Server")

        channel.pipeline()
            .addFirst(PacketDecoder())
            .addFirst(context.newHandler(channel.alloc()))
            .addLast(PacketEncoder)
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
        } else  if (cause is IOException) {
            val message = cause.message
            if (message!= null && message.startsWith("Connection reset")) {
                Logger.debug("Connection to client at $ipAddress lost")
                return
            }
        }
        ctx.close()
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
        if (msg !is Packet) {
            ctx.fireChannelRead(msg)
            return
        }
        val channel = ctx.channel()
        channel.writeAndFlush(msg.respond {
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
        }).addListener {
            val remoteAddress = channel.remoteAddress()
            info("Sent redirection to client at $remoteAddress. Closing Connection.")
        }
        channel.close()
        Packet.release(msg)
    }

    /**
     * createSslContext Creates an SSLv3 capable context for the
     * redirector server
     *
     * @return The created context
     */
    private fun createSslContext(): SslContext {
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
    }
}
