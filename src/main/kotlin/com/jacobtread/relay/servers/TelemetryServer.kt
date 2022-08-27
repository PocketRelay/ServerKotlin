package com.jacobtread.relay.servers

import com.jacobtread.blaze.logging.PacketLogger
import com.jacobtread.relay.Environment
import com.jacobtread.relay.utils.logging.Logger
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import java.io.IOException
import java.util.concurrent.CompletableFuture

/**
 * startRedirector
 *
 * @param bossGroup The netty boss event loop group
 * @param workerGroup The netty worker event loop group
 */
fun startTelemetryServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup): CompletableFuture<Void> {
    val startupFuture = CompletableFuture<Void>()
    try {
        val listenPort = 9988
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                    val remoteAddress = ch.remoteAddress()
                    Logger.debug("Connection at $remoteAddress to telemetry Server")
                    PacketLogger.setEnabled(ch, true)
                    ch.pipeline().addLast(TelemetryHandler(ch))
                }
            })
            .bind(listenPort)
            .addListener {
                Logger.info("Started Telemetry on port 9988")
                startupFuture.complete(null)
            }
    } catch (e: IOException) {
        val reason = e.message ?: e.javaClass.simpleName
        Logger.fatal("Unable to start telemetry server: $reason")
    }
    return startupFuture
}

class TelemetryHandler(val clientChannel: Channel) : ChannelInboundHandlerAdapter() {

    private var sessionId: Int = -1
    private fun createServerChannel(): Channel {
        val channelFuture = Bootstrap()
            .group(NioEventLoopGroup())
            .channel(NioSocketChannel::class.java)
            .handler(OfficialHandler())
            .connect("159.153.235.32", 9988)
            .sync()
        return channelFuture.channel()
    }
    private var serverChannel: Channel = createServerChannel()

    inner class OfficialHandler : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            if (msg is ByteBuf) {
                Logger.info("[TELEMETRY] [SERVER] Sent bytes ${msg.readableBytes()}")
            }
            clientChannel.writeAndFlush(msg)
        }
    }

    private fun decodeTLM3(value: String): String {

        val telemtryKey = "The truth is back in style."
        val keyChars = telemtryKey.toCharArray()
        val startIndex = value.indexOf('-')
        val out = StringBuilder()
        if (startIndex != -1) {
            val start = startIndex + 1
            val chars = value.toCharArray()
            for (i in start until value.length) {
                val charAt = chars[i]
                val newChar = keyChars[(i - start) % telemtryKey.length]
                val xorValue = charAt.code xor newChar.code
                if (xorValue <= 128) {
                    out.append(xorValue.toChar())
                } else {
                    out.append((newChar.code xor (charAt.code - 128)).toChar())
                }
            }
        }
        return out.toString()
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is ByteBuf) {
            Logger.info("[TELEMETRY] [CLIENT] Sent bytes ${msg.readableBytes()}")

            msg.skipBytes(10) // Skip Heading
            val text = msg.readCharSequence(msg.readableBytes(), Charsets.UTF_8)
            val lines = text.split('\n')
            val dataMap = HashMap<String, String>()
            lines.forEach { line ->
                val parts = line.split('=', limit = 2)
                if (parts.size == 2) {
                    dataMap[parts[0]] = parts[1]
                }
            }

            val auth = dataMap["AUTH"]
            if (auth != null) {
                sessionId = auth.toInt()
            }
            val tlm3 = dataMap["TLM3"]
            if (tlm3 != null) {
                val decoded = decodeTLM3(tlm3).replace('/', '\n')
                Logger.info("Telemetry TLM3: $decoded")
            }


            Logger.info(dataMap.toString())
            Logger.debug("TELEMETRY DATA: $text")
        }
        if (serverChannel.isOpen) {
            serverChannel.writeAndFlush(msg)
        } else {
            serverChannel = createServerChannel()
        }
    }
}