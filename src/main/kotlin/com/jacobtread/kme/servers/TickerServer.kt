package com.jacobtread.kme.servers

import com.jacobtread.kme.Config
import com.jacobtread.kme.LOGGER
import com.jacobtread.kme.utils.customThreadFactory
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.io.IOException

/**
 * startTickerServer Simple discard server. Reads all the input bytes and discards
 * them unless debug is enabled if debug is enabled they are printed to debug log
 * as an array
 *
 * @param config The server configuration
 */
fun startTickerServer(config: Config) {
    Thread {
        val bossGroup = NioEventLoopGroup(customThreadFactory("Ticker Boss #{ID}"))
        val workerGroup = NioEventLoopGroup(customThreadFactory("Ticker Worker #{ID}"))
        val bootstrap = ServerBootstrap() // Create a server bootstrap
        try {
            val bind = bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<Channel>() {
                    override fun initChannel(ch: Channel) {
                        ch.pipeline()
                            // Add handler for processing packets
                            .addLast(object : ChannelInboundHandlerAdapter() {
                                override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                                    super.channelRead(ctx, msg)
                                    if (msg is ByteBuf) {
                                        val readable = msg.readableBytes()
                                        if (readable > 0) {
                                            val out = ByteArray(readable)
                                            msg.readBytes(out)
                                            ctx.flush()
                                            if (LOGGER.isDebugEnabled) {
                                                LOGGER.debug(out.contentToString())
                                            }
                                        }
                                    }
                                }
                            })
                    }
                })
                // Bind the server to the host and port
                .bind(config.host, config.ports.redirector)
                // Wait for the channel to bind
                .sync();
            LOGGER.info("Started Ticker Server (${config.host}:${config.ports.ticker})")
            bind.channel()
                // Get the closing future
                .closeFuture()
                // Wait for the closing
                .sync()
        } catch (e: IOException) {
            LOGGER.error("Exception in ticker server", e)
        }
    }.apply {
        // Name the redirector thread
        name = "Ticker"
        // Close this thread when the JVM requests close
        isDaemon = true
        // Start the redirector thread
        start()
    }
}