package com.jacobtread.kme

import com.jacobtread.kme.blaze.Packet
import com.jacobtread.kme.blaze.PacketDecoder
import com.jacobtread.kme.utils.createContext
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslHandshakeCompletionEvent
import io.netty.util.concurrent.GenericFutureListener

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

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Packet) {
        println(msg)
    }
}