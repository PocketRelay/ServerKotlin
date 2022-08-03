package com.jacobtread.kme.data.retriever

import com.jacobtread.blaze.*
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.tdf.types.GroupTdf
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.utils.logging.Logger
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.io.IOException
import java.net.URL
import java.util.regex.Pattern

object Retriever {

    data class ServerDetails(
        val host: String,
        val port: Int,
        val secure: Boolean,
    )

    private val clientEventLoopGroup: NioEventLoopGroup = NioEventLoopGroup()
    private val serverDetails: ServerDetails? = getMainServerDetails()

    internal var isEnabled: Boolean = true

    /**
     * Creates a connection to the official server and uses this
     * as the channel for communicating between the client and
     * the server.
     *
     * @return The created channel
     */
    fun createOfficialChannel(handler: ChannelHandler): Channel? {
        if (serverDetails == null || !isEnabled) {
            return null
        }
        val channelFuture = Bootstrap()
            .group(clientEventLoopGroup)
            .channel(NioSocketChannel::class.java)
            .handler(handler)
            .connect(serverDetails.host, serverDetails.port)
            .sync()
        val channel = channelFuture.channel()
        channel.attr(PacketEncoder.ENCODER_CONTEXT_KEY)
            .set("Connection to Official EA Server")
        val pipeline = channel.pipeline()
        pipeline.addFirst(PacketDecoder())
        if (serverDetails.secure) {
            val context = SslContextBuilder.forClient()
                .ciphers(listOf("TLS_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_RC4_128_MD5"))
                .protocols("SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3")
                .startTls(true)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()
            pipeline.addFirst(context.newHandler(channel.alloc()))
        }
        pipeline.addLast(PacketEncoder)
        return channel
    }


    /**
     * Performs a lookup for the official redirector host
     * ip address using the Google DNS api, this is because
     * the host running this server may have a hosts file
     * redirect for this domain
     *
     * @return The redirect host ip
     */
    private fun getRedirectorHost(): String {
        try {
            val url = URL("https://dns.google/resolve?name=gosredirector.ea.com&type=A")

            val inputStream = url.openStream()
            val responseBytes = inputStream.use { it.readAllBytes() }
            val responseText = String(responseBytes, Charsets.UTF_8)

            // Pattern for matching the response data value
            val pattern = Pattern.compile("\"data\":\"([0-9.]+)\"")

            val matcher = pattern.matcher(responseText)

            if (!matcher.find()) {
                Logger.fatal("Failed to retreive official redirector IP address. Cannot start in MITM mode. (No Match)")
            }
            return matcher.group(1)
                ?: Logger.fatal("Failed to retreive official redirector IP address. Cannot start in MITM mode. (Group was null)")
        } catch (e: IOException) {
            Logger.fatal("Failed to retreive official redirector IP address. Cannot start in MITM mode.", e)
        }
    }


    /**
     * Connects to the official redirector server and retrieve
     * the connection information for the official main server.
     */
    private fun getMainServerDetails(): ServerDetails? {
        val redirectorHost = getRedirectorHost()
        Logger.info("Located official redirector address: $redirectorHost")
        val redirectorPort = 42127
        var details: ServerDetails? = null

        val channelFuture = Bootstrap()
            .group(clientEventLoopGroup)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInboundHandlerAdapter() {

                override fun handlerAdded(ctx: ChannelHandlerContext) {
                    super.handlerAdded(ctx)
                    val sslContext = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build()
                    val channel = ctx.channel()
                    val pipeline = channel.pipeline()
                    pipeline.addFirst(PacketDecoder())
                        .addFirst(sslContext.newHandler(channel.alloc()))
                        .addLast(PacketEncoder)
                }

                override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                    if (msg !is Packet) {
                        ctx.fireChannelRead(msg)
                        return
                    }

                    if (msg.component == Components.REDIRECTOR && msg.command == Commands.GET_SERVER_INSTANCE) {
                        val address = msg.optional("ADDR")
                        val addressGroup = address.value as GroupTdf

                        details = ServerDetails(
                            host = addressGroup.text("HOST"),
                            port = addressGroup.numberInt("PORT"),
                            secure = msg.numberInt("SECU") == 0x1,
                        )
                        ctx.channel().close()
                        Logger.info("MITM Recieved server instance result. Closing now.")
                    }
                }
            })
            .connect(redirectorHost, redirectorPort)
            .sync()

        val channel = channelFuture.channel()
        Logger.info("Connected to official redirector server")
        Logger.info("Sending redirect request packet")

        channel.writeAndFlush(
            clientPacket(Components.REDIRECTOR, Commands.GET_SERVER_INSTANCE, 0x0) {
                text("BSDK", "3.15.6.0")
                text("BTIM", "Dec 21 2012 12:47:10")
                text("CLNT", "MassEffect3-pc")
                number("CLTP", 0x0)
                text("CSKU", "134845")
                text("CVER", "05427.124")
                text("DSDK", "8.14.7.1")
                text("ENV", "prod")
                optional("FPID")
                number("LOC", 0x656e4e5a)
                text("NAME", "masseffect-3-pc")
                text("PLAT", "Windows")
                text("PROF", "standardSecure_v3")
            }
        )

        Logger.info("Waiting for server response and close")
        channel.closeFuture().sync()
        Logger.info("Finished MITM Redirect finding")
        if (details == null) {
            Logger.info("Failed to retrieve redirect information. Disabled data retrieving")
        }
        return details
    }
}
