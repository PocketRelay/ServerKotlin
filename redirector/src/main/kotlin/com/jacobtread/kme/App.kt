package com.jacobtread.kme

import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.logging.Logger
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.io.IOException
import java.security.KeyStore
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.KeyManagerFactory

val LOGGER = Logger.get()

/**
 * main
 *
 * @param args
 */
fun main(args: Array<String>) {

    Thread.currentThread().name = "Redirector"

    val parser = ArgParser(args)

    val selfHost: String by parser.storing("--host", help = "The address to listen on")
        .default("127.0.0.1")
    val selfPort: Int by parser.storing("--port", help = "The port to listen on", transform = String::toInt)
        .default(42127)

    val redirectHost: String by parser.storing("--rhostname", help = "The hostname to redirect to")
        .default("383933-gosprapp396.ea.com")
    val redirectIp: String by parser.storing("--rhost", help = "The host ip to redirect to")
        .default("127.0.0.1")
    val redirectPort: Int by parser.storing("--rport", help = "The port to redirect to", transform = String::toInt)
        .default(14219)

    val config = Config(
        selfHost,
        selfPort,
        redirectHost,
        redirectIp,
        redirectPort
    )
    startRedirector(config)
}

data class Config(
    val selfHost: String,
    val selfPort: Int,
    val redirectHost: String,
    val redirectIp: String,
    val redirectPort: Int,
)

fun createContext(): SslContext {
    java.security.Security.setProperty("jdk.tls.disabledAlgorithms", "");
    val keyStore = loadKeystore()
    return SslContextBuilder.forServer(keyStore)
        .ciphers(listOf("TLS_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_RC4_128_MD5"))
        .protocols("SSLv3", "TLSv1.2", "TLSv1.3")
        .startTls(true)
        .trustManager(InsecureTrustManagerFactory.INSTANCE)
        .build() ?: throw IllegalStateException("Unable to create SSL Context")
}

private fun loadKeystore(): KeyManagerFactory {
    val password = "123456".toCharArray()
    val stream = Logger::class.java.getResourceAsStream("/redirector.pfx")
        ?: throw IllegalStateException("Missing required keystore")
    val keyStore = KeyStore.getInstance("PKCS12")
    keyStore.load(stream, password)
    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    kmf.init(keyStore, password)
    return kmf
}

fun customThreadFactory(name: String): ThreadFactory {
    return object : ThreadFactory {
        val ID = AtomicInteger(0)
        override fun newThread(r: Runnable): Thread {
            val thread = Thread(r)
            thread.name = name.replace("{ID}", ID.getAndIncrement().toString())
            thread.isDaemon = true
            return thread
        }
    }
}

/**
 * startRedirector Starts the Redirector server in a new thread
 *
 * @param config The server configuration
 */
fun startRedirector(config: Config) {
    val context = createContext() // Create a SSL context
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
            .bind(config.selfHost, config.selfPort)
            // Wait for the channel to bind
            .sync()
        LOGGER.info("Started Redirector Server (${config.selfHost}:${config.selfPort})")
        bind.channel()
            // Get the closing future
            .closeFuture()
            // Wait for the closing
            .sync()
    } catch (e: IOException) {
        LOGGER.error("Exception in redirector server", e)
    }
}

private fun getIpAddressLong(value: String): Long {
    val parts = value.split('.', limit = 4)
    require(parts.size == 4) { "Invalid IPv4 Address" }
    return (parts[0].toLong() shl 24)
        .or(parts[1].toLong() shl 16)
        .or(parts[2].toLong() shl 8)
        .or(parts[3].toLong())
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
            val platform = msg.getValueOrNull(StringTdf::class, "PLAT") ?: "Unknown"
            val channel = ctx.channel()
            val remoteAddress = channel.remoteAddress()

            // Create a redirect response packet for the client
            val packet = respond(msg) {
                union("ADDR",
                    0x0,
                    struct("VALU") {
                        text("HOST", config.redirectHost)
                        number("IP", getIpAddressLong(config.redirectIp))
                        number("PORT", config.redirectPort)
                    }
                )
                number("SECU", 0x0)
                number("XDNS", 0x0)
            }
            // Write the packet, flush and then close the channel
            channel.write(packet)
            channel.flush()
            channel.close()
            LOGGER.info("Sending redirection to client ($remoteAddress) -> on platform $platform")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
    }
}
