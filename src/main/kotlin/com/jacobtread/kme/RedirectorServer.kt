package com.jacobtread.kme

import com.jacobtread.kme.blaze.InPacket
import com.jacobtread.kme.blaze.PacketCommand
import com.jacobtread.kme.blaze.PacketComponent
import com.jacobtread.kme.blaze.PacketDecoder
import com.jacobtread.kme.blaze.builder.Packet
import com.jacobtread.kme.utils.NULL_CHAR
import com.jacobtread.kme.utils.createContext
import com.jacobtread.kme.utils.getIp
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.io.IOException

class RedirectorServer(
    val config: Config,
) : SimpleChannelInboundHandler<InPacket>() {

    companion object {
        fun start(config: Config) {
            val redirector = config.redirectorServer;
            LOGGER.info("Creating redirector server at ${config.host}:${redirector.port}")
            val context = createContext()
            val bossGroup = NioEventLoopGroup()
            val workerGroup = NioEventLoopGroup()
            val redirect = RedirectorServer(config)
            val bootstrap = ServerBootstrap()
            try {
                bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel::class.java)
                    .childHandler(object : ChannelInitializer<Channel>() {
                        override fun initChannel(ch: Channel) {
                            val handler = context.newHandler(ch.alloc());
                            ch.pipeline()
                                .addLast(handler)
                                .addLast(PacketDecoder())
                                .addLast(redirect)
                        }
                    })
                    .bind(config.host, redirector.port)
                    .sync()
                    .channel()
                    .closeFuture().sync()
            } catch (e: IOException) {
                LOGGER.error("Redirector server error", e)
            }
        }
    }

    lateinit var channel: Channel

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        channel = ctx.channel()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: InPacket) {
        if (msg.component == PacketComponent.REDIRECTOR && msg.command == PacketCommand.REQUEST_REDIRECT) {
            val channel = ctx.channel()
            val remoteAddress = channel.remoteAddress()

            LOGGER.info("Sending redirection to client -> $remoteAddress")
            val redirectorPacket = config.redirectorPacket
            val packet = Packet(msg.component, msg.command, 0, 0x1000) {
                Union(
                    "ADDR", redirectorPacket.addr,
                    StructInline("VALU") {
                        Text("HOST", redirectorPacket.host)
                        VarInt("IP$NULL_CHAR$NULL_CHAR", redirectorPacket.ip.getIp())
                        VarInt("PORT", redirectorPacket.port)
                    }
                )
                VarInt("SECU", redirectorPacket.secu)
                VarInt("XDNS", redirectorPacket.xdns)
            }
            LOGGER.info(packet.array().contentToString())
            channel.write(packet)
        }
    }
}