package com.jacobtread.kme.servers

import com.jacobtread.blaze.*
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.tdf.types.GroupTdf
import com.jacobtread.kme.Environment
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.logging.Logger.info
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.io.IOException
import java.net.URL

/**
 * startMITMServer Starts the MITM server
 *
 * @param bossGroup The netty boss event loop group
 * @param workerGroup The netty worker event loop group
 */
fun startMITMServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
    try {
        PacketLogger.isEnabled = false
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                    ch.pipeline().addLast(MITMHandler(workerGroup))
                }

            })
            // Bind the server to the host and port
            .bind(Environment.mainPort)
            // Wait for the channel to bind
            .addListener { info("Started MITM Server on port ${Environment.mainPort}") }
    } catch (e: IOException) {
        Logger.error("Exception in redirector server", e)
    }
}

class MITMHandler(private val eventLoopGroup: NioEventLoopGroup) : ChannelInboundHandlerAdapter() {

    private fun lookupRedirectorHost(): String {
        val url = URL("https://dns.google/resolve?name=gosredirector.ea.com&type=A")
        val response = url.openStream().use { it.readAllBytes().decodeToString() }
        val regex = Regex("\"data\": \"(.*)\"")
        val match = regex.matchAt(response, 0)
        require(match != null) { "Unable to find redirector ip" }
        return match.groupValues[0]
    }

    data class ServerDetails(
        val host: String,
        val ip: String,
        val port: Int,
        val secure: Boolean,
    )

    private fun getOfficalServerDetails(): ServerDetails {
        val redirectorHost = lookupRedirectorHost() // gosredirector.ea.com
        val redirectorPort = 42127
        var result: ServerDetails? = null
        val channelFuture = Bootstrap()
            .group(DefaultEventLoopGroup())
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInboundHandlerAdapter() {

                override fun handlerAdded(ctx: ChannelHandlerContext) {
                    val context = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build()
                    val channel = ctx.channel()
                    channel.pipeline()
                        .addFirst(PacketDecoder())
                        .addFirst(context.newHandler(channel.alloc()))
                        .addLast(PacketEncoder)
                }

                @Suppress("DeprecatedCallableAddReplaceWith")
                @Deprecated("Deprecated in Java")
                override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
                    Logger.error("Exception", cause)
                }

                override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                    if (msg !is Packet) {
                        println("NOn message packet")
                        ctx.fireChannelRead(msg)
                        return
                    }
                    if (msg.component == Components.REDIRECTOR && msg.command == Commands.GET_SERVER_INSTANCE) {
                        val address = msg.optional("ADDR")
                        val value = address.value as GroupTdf
                        val host = value.text("HOST")
                        val ip = value.number("IP")
                        val port = value.number("PORT")
                        val secure: Boolean = msg.number("SECU") == 0x1uL
                        val ipString = ((ip shr 24) and 255u).toString() + "." + ((ip shr 16) and 255u) + "." + ((ip shr 8) and 255u) + "." + (ip and 255u)
                        result = ServerDetails(host, ipString, port.toInt(), secure)
                        ctx.close()
                    }
                }
            })
            .connect(redirectorHost, redirectorPort)
            .sync()
        val channel = channelFuture.channel()
        info("Connected to server")
        info("Sending REDIRECTOR / GET_SERVER_INSTANCE packet")
        val packet = createRedirectPacket()
        channel.write(packet)
        channel.flush()
        info("Waiting for closed response")
        channel.closeFuture().sync()
        requireNotNull(result) { "Server details were null" }
        info("Obtained server details: $result")

        return result!!
    }

    private fun createRedirectPacket(): Packet {
        return clientPacket(Components.REDIRECTOR, Commands.GET_SERVER_INSTANCE, 0x0) {
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
    }


    private val serverDetails = getOfficalServerDetails()
    var clientChannel: Channel? = null
    var officialChannel: Channel? = null

    private var unlockCheat = false
    private var forwardHttp = false

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        super.handlerAdded(ctx)
        val channel = ctx.channel()
        channel.attr(PacketEncoder.ENCODER_CONTEXT_KEY)
            .set("Connection to Client")
        channel.pipeline()
            .addFirst(PacketDecoder())
            .addLast(PacketEncoder)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        clientChannel = ctx.channel()
        officialChannel = createOfficialConnection()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)
        clientChannel = null
        officialChannel?.apply {
            if (isOpen) close()
            officialChannel = null
        }
    }

    private fun createOfficialConnection(): Channel {
        val channelFuture = Bootstrap()
            .group(eventLoopGroup)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInboundHandlerAdapter() {
                override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                    channelReadOffical(msg)
                }
            })
            .connect(serverDetails.host, serverDetails.port)
            .sync()
        info("Created new MITM connection")
        val channel = channelFuture.channel()
        channel.attr(PacketEncoder.ENCODER_CONTEXT_KEY)
            .set("Connection to EA")
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

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is Packet) {
            return
        }
        PacketLogger.log("DECODED FROM CLIENT", officialChannel!!, msg)
        if (unlockCheat
            && msg.component == Components.UTIL
            && msg.command == Commands.USER_SETTINGS_LOAD_ALL
        ) {
            // Unlock everything cheat
            createUnlockPackets()
        }

        //  Forward HTTP traffic to local
        if (forwardHttp
            && msg.component == Components.UTIL
            && msg.command == Commands.FETCH_CLIENT_CONFIG
        ) {
            val type = msg.text("CFID")
            if (type == "ME3_DATA") {
                clientChannel?.apply {
                    write(msg.respond {
                        map("CONF", Data.createDataConfig())
                    })
                    flush()
                }
                return
            }
        }
        officialChannel
            ?.writeAndFlush(msg)
            ?.addListener {
                Packet.release(msg)
            }
    }

    private fun createUnlockPackets() {
        info("UNLOCKING EVERYTHING")
        var id = 999
        officialChannel?.apply {
            write(clientPacket(Components.UTIL, Commands.USER_SETTINGS_SAVE, id++) {
                val out = "20;4;${Int.MAX_VALUE};-1;0;0;0;50;180000;0;${"f".repeat(1342)}"
                text("DATA", out)
                text("KEY", "Base")
                number("UID", 0x0)
            })
            flush()
            val names = arrayOf("Adept", "Soldier", "Engineer", "Sentinel", "Infiltrator", "Vanguard")
            for (i in 1..6) {
                val name = names[i - 1]
                val out = "20;4;$name;20;0;${Integer.MAX_VALUE}"
                write(clientPacket(Components.UTIL, Commands.USER_SETTINGS_SAVE, id++) {
                    text("DATA", out)
                    text("KEY", "class$i")
                    number("UID", 0x0)
                })
                flush()
            }
        }
    }


    fun channelReadOffical(msg: Any) {
        if (msg !is Packet) return
        PacketLogger.log("DECODED FROM EA", officialChannel!!, msg)

        clientChannel
            ?.writeAndFlush(msg)
            ?.addListener {
                Packet.release(msg)
            }
    }
}
