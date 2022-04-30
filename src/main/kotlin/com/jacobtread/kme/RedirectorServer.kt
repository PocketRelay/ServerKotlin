package com.jacobtread.kme

import com.jacobtread.kme.utils.createContext
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel

class RedirectorServer : SimpleChannelInboundHandler<>() {

    companion object {
        private const val DEFAULT_HOST = "127.0.0.1"
        private const val DEFAULT_PORT = 42127

        fun create(host: String = DEFAULT_HOST, port: Int = DEFAULT_PORT) {
            val context = createContext()

            // TODO: Platform specific event loop groups?

            val bossGroup = NioEventLoopGroup()
            val workerGroup = NioEventLoopGroup()

            val bootstrap = ServerBootstrap()
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<Channel>() {
                    override fun initChannel(ch: Channel) {
                        ch.pipeline()
                            .addLast(context.newHandler(ch.alloc()))
                    }
                })

        }
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        super.channelRead(ctx, msg)
    }


}