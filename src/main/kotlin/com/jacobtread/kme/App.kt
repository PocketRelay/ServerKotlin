package com.jacobtread.kme

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContextBuilder
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*


fun main() {
    Security.setProperty("jdk.tls.disabledAlgorithms", "");

    val privateKey = readPrivateKey()
    val certificate = readCertificate()

    val context = SslContextBuilder.forServer(privateKey, certificate)
        .ciphers(listOf("TLS_RSA_WITH_RC4_128_MD5", "TLS_RSA_WITH_RC4_128_SHA"))
        .protocols("SSLv3")
        .build()

    val bossGroup = NioEventLoopGroup()
    val workerGroup = NioEventLoopGroup()
    try {
        val b = ServerBootstrap()
        val f = b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .handler(LoggingHandler(LogLevel.INFO))
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                   println("New channel started ${ch.remoteAddress()}")
                    ch.pipeline()
                        .addLast(context.newHandler(ch.alloc()))
                }
            })
            .bind(42127).sync()
        f.channel().closeFuture().sync()



    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        workerGroup.shutdownGracefully()
        bossGroup.shutdownGracefully()
    }
    while (true) {

    }
}

class ServerHandler : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        super.channelRead(ctx, msg)
    }

}