package com.jacobtread.kme.servers

import com.jacobtread.kme.Config
import com.jacobtread.kme.LOGGER
import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.builder.Packet
import com.jacobtread.kme.utils.createContext
import com.jacobtread.kme.utils.customThreadFactory
import com.jacobtread.kme.utils.getIp
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.io.IOException

/**
 * startRedirector Starts the Redirector server in a new thread
 *
 * @param config The server configuration
 */
fun startRedirector(config: Config) {
    Thread {
        LOGGER.info("===== Redirection Configuration =====")
        LOGGER.info("Host: ${config.redirectorPacket.host}")
        LOGGER.info("IP:   ${config.redirectorPacket.ip}")
        LOGGER.info("Port: ${config.redirectorPacket.port}")
        LOGGER.info("=====================================")
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
                            .addLast(RedirectClient(config.redirectorPacket))
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
private class RedirectClient(private val config: Config.RedirectorPacket) : SimpleChannelInboundHandler<RawPacket>() {

    /**
     * channelRead0 Handles incoming RawPackets and sends back a redirect packet
     * when the REDIRECTOR + REQUEST_REDIRECT packet is received
     *
     * @param ctx
     * @param msg
     */
    override fun channelRead0(ctx: ChannelHandlerContext, msg: RawPacket) {
        if (msg.component == PacketComponent.REDIRECTOR
            && msg.command == PacketCommand.GET_SERVER_INSTANCE
        ) {
            val platform = msg.getValue(StringTdf::class, "PLAT")
            val channel = ctx.channel()
            val remoteAddress = channel.remoteAddress()
            LOGGER.info("Sending redirection to client ($remoteAddress) -> on platform ${platform ?: "Unknown"}")

            // Create a packet to redirect the client to the target server
            val packet = Packet(msg.component, msg.command, 0x1000, msg.id) {
                union("ADDR",
                    config.addr,
                    struct("VALU") {
                        text("HOST", config.host)
                        number("IP", config.ip.getIp())
                        number("PORT", config.port)
                    }
                )
                number("SECU", config.secu)
                number("XDNS", config.xdns)
            }

            // Write the packet, flush and then close the channel
            channel.write(packet)
            channel.flush()
            channel.close()
            LOGGER.info("Terminating connection to $remoteAddress (Finished redirect)")
        }
    }
}
