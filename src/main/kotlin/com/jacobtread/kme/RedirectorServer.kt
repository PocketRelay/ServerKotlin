package com.jacobtread.kme

import com.jacobtread.kme.blaze.Packet
import com.jacobtread.kme.blaze.PacketDecoder
import com.jacobtread.kme.utils.createContext
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel

class RedirectorServer : SimpleChannelInboundHandler<Packet>() {

    companion object {
        private const val DEFAULT_HOST = "127.0.0.1"
        private const val DEFAULT_PORT = 42127

        fun create(host: String = DEFAULT_HOST, port: Int = DEFAULT_PORT) {
            val context = createContext()
            val bossGroup = NioEventLoopGroup()
            val workerGroup = NioEventLoopGroup()
            val redirect = RedirectorServer()
            val bootstrap = ServerBootstrap()
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
                .bind(host, port)
                .sync()
                .channel()
                .closeFuture().sync()

        }
    }


    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet) {
        if (msg.component == 0x5 /* Redirect Component*/ && msg.command == 0x1 /* Authenticate Command*/) {
            val channel = ctx.channel()
            val remoteAddress = channel.remoteAddress()
            println("Sending redirection to client -> $remoteAddress")

        }
    }
}