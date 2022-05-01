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
fun startTelemetryServer(config: Config) {
    Thread {
        val bossGroup = NioEventLoopGroup(customThreadFactory("Telemetry Boss #{ID}"))
        val workerGroup = NioEventLoopGroup(customThreadFactory("Telemetry Worker #{ID}"))
        val bootstrap = ServerBootstrap() // Create a server bootstrap
        try {
            val bind = bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<Channel>() {
                    override fun initChannel(ch: Channel) {
                        ch.pipeline()
                            // Add handler for processing telemetry data
                            .addLast(TelemetryClient())
                    }
                })
                // Bind the server to the host and port
                .bind(config.host, config.ports.telemetry)
                // Wait for the channel to bind
                .sync();
            LOGGER.info("Started Telemetry Server (${config.host}:${config.ports.telemetry})")
            bind.channel()
                // Get the closing future
                .closeFuture()
                // Wait for the closing
                .sync()
        } catch (e: IOException) {
            LOGGER.error("Exception in telemetry server", e)
        }
    }.apply {
        // Name the telemetry thread
        name = "Telemetry"
        // Close this thread when the JVM requests close
        isDaemon = true
        // Start the telemetry thread
        start()
    }
}

class TelemetryClient : ChannelInboundHandlerAdapter() {
    companion object {
        private val TLM3key = intArrayOf(0x54, 0x68, 0x65, 0x20, 0x74, 0x72, 0x75, 0x74, 0x68, 0x20, 0x69, 0x73, 0x20, 0x62, 0x61, 0x63, 0x6B, 0x20, 0x69, 0x6E, 0x20, 0x73, 0x74, 0x79, 0x6C, 0x65, 0x2E)

        fun decodeTLM3Line(buff: ByteArray): ByteArray {
            var start = -1
            for (i in buff.indices) if (buff[i].toInt() == 0x2D) {
                start = i + 1
                break
            }
            if (start != -1) for (i in start until buff.size) {
                val b = buff[i]
                val k = TLM3key[(i - start) % 0x1B]
                if (b.toInt() xor k <= 0x80) buff[i] = (b.toInt() xor k).toByte() else buff[i] = (k xor b - 0x80).toByte()
            }
            return buff
        }
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is ByteBuf) {
            val length = msg.readableBytes()
            if (length == 0) return
            val bytes = ByteArray(length)
            msg.readBytes(bytes)
            val contents = StringBuilder()
            for (i in 0..(length - 4)) {
                if (bytes[i] == 0x54.toByte()
                    && bytes[i + 1] == 0x4C.toByte()
                    && bytes[i + 2] == 0x4D.toByte()
                    && bytes[i + 3] == 0x33.toByte()
                ) {
                    for (j in (i + 4)..(length - 4)) {
                        if (bytes[j] == 0x41.toByte()
                            && bytes[j + 1] == 0x55.toByte()
                            && bytes[j + 2] == 0x54.toByte()
                            && bytes[j + 3] == 0x48.toByte()
                        ) {
                            val chunk = ByteArray(j - (i + 5))
                            for (k in (i + 5)..j) {
                                chunk[k - (i + 5)] - bytes[k]
                            }
                            val line = decodeTLM3Line(chunk)
                            line.forEach {
                                contents.append(it.toInt().toChar())
                            }
                        }
                    }
                }
            }
            LOGGER.debug("Received data ($length bytes) ===")
            LOGGER.debug(contents.toString())
            LOGGER.debug("=================================")
        }
    }
}