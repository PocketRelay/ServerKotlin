package com.jacobtread.kme

import com.jacobtread.kme.blaze.Packet
import com.jacobtread.kme.blaze.PacketComponent
import com.jacobtread.kme.blaze.PacketDecoder
import com.jacobtread.kme.utils.createContext
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.io.IOException

class RedirectorServer : SimpleChannelInboundHandler<Packet>() {

    companion object {
        fun start(config: Config) {
            val redirector = config.redirectorServer;
            LOGGER.info("Creating redirector server at ${redirector.host}:${redirector.port}")
            val context = createContext()
            val bossGroup = NioEventLoopGroup()
            val workerGroup = NioEventLoopGroup()
            val redirect = RedirectorServer()
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
                    .bind(redirector.host, redirector.port)
                    .sync()
                    .channel()
                    .closeFuture().sync()
            } catch (e: IOException) {
                LOGGER.error("Redirector server error", e)
            }
        }
    }


    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet) {
        if (msg.component == PacketComponent.REDIRECTOR /* Redirect Component*/
            && msg.command == 0x1 /* Authenticate Command*/) {
            val channel = ctx.channel()
            val remoteAddress = channel.remoteAddress()

            LOGGER.info("Sending redirection to client -> $remoteAddress")

            val packet = Packet(msg.component, msg.command, 0, 0x1000)

        }
    }
}