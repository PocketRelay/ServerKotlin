package com.jacobtread.kme.tools

import com.jacobtread.kme.Environment
import com.jacobtread.kme.utils.logging.Logger
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.net.URL

class AutoMITM : ChannelInboundHandlerAdapter() {

    companion object {
        @Serializable
        data class LookupResponse(
            @SerialName("Answer")
            val answer: List<Answer>
        ) {
            @Serializable
            data class Answer(
                val name: String,
                val data: String
            )
        }

        private fun lookupRedirectorHost(): String {
            val json = Json { ignoreUnknownKeys = true }
            val response = URL("https://dns.google/resolve?name=gosredirector.ea.com&type=A").openStream().use {
                val responseText = it.readAllBytes().decodeToString()
                json.decodeFromString<LookupResponse>(responseText)
            }
            val first = response.answer.firstOrNull()
            requireNotNull(first) { "Failed to lookup redirector hostname"}
            return first.data
        }

        fun create(workerGroup: NioEventLoopGroup) {
            val autoMITM = AutoMITM()
            val channelFuture = Bootstrap()
                .group(workerGroup)
                .channel(NioSocketChannel::class.java)
                .handler(autoMITM)
                .connect(Environment.mitmHost, Environment.redirectorPort)
                .addListener {
                    Logger.info("Created new MITM connection")
                }.sync()
            val channel = channelFuture.channel()
            val context = SslContextBuilder.forClient()
                .ciphers(listOf("TLS_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_RC4_128_MD5"))
                .protocols("SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3")
                .startTls(true)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()
            channel.pipeline().addFirst(context.newHandler(channel.alloc()))
        }
    }

}