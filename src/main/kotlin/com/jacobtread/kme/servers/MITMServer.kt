package com.jacobtread.kme.servers

import com.jacobtread.blaze.*
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.tdf.types.GroupTdf
import com.jacobtread.kme.Environment
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.utils.logging.Logger
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
import java.util.regex.Pattern

fun startMITMServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
    try {
        PacketLogger.isEnabled = false
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                    ch.attr(PacketEncoder.ENCODER_CONTEXT_KEY)
                        .set("Connection to Client")
                    ch.pipeline()
                        .addLast(MITMHandler(ch))
                }
            })
            .bind(Environment.mainPort)
            .addListener { Logger.info("Started MITM Server on port ${Environment.mainPort}") }
    } catch (e: IOException) {
        Logger.error("Exception in man-in-the-middle server", e)
    }
}

class MITMHandler(
    private val clientChannel: Channel,
) : ChannelInboundHandlerAdapter() {


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
            val pattern = Pattern.compile("\"data\": \"(.*)\"")

            val matcher = pattern.matcher(responseText)

            if (!matcher.find()) {
                Logger.fatal("Failed to retreive official redirector IP address. Cannot start in MITM mode.")
            }
            return matcher.group(0)
                ?: Logger.fatal("Failed to retreive official redirector IP address. Cannot start in MITM mode.")
        } catch (e: IOException) {
            Logger.fatal("Failed to retreive official redirector IP address. Cannot start in MITM mode.", e)
        }
    }

    private var host: String = ""
    private var port: Int = 0
    private var secure: Boolean = false
    private val serverChannel: Channel

    private var forwardHttp: Boolean = false
    private var unlockCheat: Boolean = false

    init {
        initialSetup()
        serverChannel = createOfficialChannel()
    }

    /**
     * Connects to the official redirector server and obtains the
     * information of the main server for creating the MITM
     * bridge connection.
     */
    private fun initialSetup() {
        val redirectorHost = getRedirectorHost()
        val redirectorPort = 42127

        val channelFuture = Bootstrap()
            .group(DefaultEventLoop())
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

                        host = addressGroup.text("HOST")
                        port = addressGroup.numberInt("PORT")
                        secure = msg.numberInt("SECU") == 0x1
                        ctx.close()
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
    }

    /**
     * Creates a connection to the official server and uses this
     * as the channel for communicating between the client and
     * the server.
     *
     * @return The created channel
     */
    private fun createOfficialChannel(): Channel {
        val channelFuture = Bootstrap()
            .group(DefaultEventLoopGroup())
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInboundHandlerAdapter() {
                override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                    if (msg !is Packet) return
                    channelReadOfficial(msg)
                }
            })
            .connect(host, port)
            .sync()

        val channel = channelFuture.channel()
        channel.attr(PacketEncoder.ENCODER_CONTEXT_KEY)
            .set("Connection to Official EA Server")
        val pipeline = channel.pipeline()
        pipeline.addFirst(PacketDecoder())
        if (secure) {
            val context = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()
            pipeline.addFirst(context.newHandler(channel.alloc()))
        }
        pipeline.addFirst(PacketEncoder)
        return channel
    }

    /**
     * Handles inactivity on the channel which closes the
     * server channel connection
     *
     * @param ctx
     */
    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)
        if (serverChannel.isOpen) {
            serverChannel.close()
        }
    }

    /**
     * Handles reading the packets from the official server channel and writing
     * them to the client channel. Also handles HTTP forwarding
     *
     * @param packet The recieved packet
     */
    private fun channelReadOfficial(packet: Packet) {
        PacketLogger.log("DECODED FROM EA SERVER", serverChannel, packet)

        if (tryForwardHttp(packet)) return

        // Optionally modify the contents of the packet or create custom response
        clientChannel.writeAndFlush(packet)
            .addListener { Packet.release(packet) }
    }

    /**
     * Tries to activate the "unlock everything cheat". Triggered when
     * the client asks the server to load all settings. Sends lots of
     * packets which updates all the client settings and data for
     * inventory, classes, challenges, etc.
     *
     * @param packet The packet to check
     */
    private fun tryUnlockCheat(packet: Packet) {
        if (!unlockCheat) return
        if (packet.component == Components.UTIL && packet.command == Commands.USER_SETTINGS_LOAD_ALL) {

            Logger.info("Unlocking everything with cheat.")

            var id = packet.id

            // Base settings cheat (Unlocks all inventory items and gives max currency value)
            serverChannel.writeAndFlush(
                clientPacket(Components.UTIL, Commands.USER_SETTINGS_SAVE, id++) {
                    val baseBuilder = StringBuilder("20;4;")
                        .append(Int.MAX_VALUE)
                        .append(";-1;0;0;0;50;180000;0;")
                    repeat(671) {
                        baseBuilder.append("FF")
                    }
                    text("DATA", baseBuilder.toString())
                    text("KEY", "Base")
                    number("UID", 0)
                }
            )


            // Class level and promotions chea
            val classNames = arrayOf("Adept", "Soldier", "Engineer", "Sentinel", "Infiltrator", "Vanguard")
            classNames.forEachIndexed { index, className ->
                serverChannel.write(
                    clientPacket(Components.UTIL, Commands.USER_SETTINGS_SAVE, id++) {
                        val builder = StringBuilder("20;4;")
                            .append(className)
                            .append(";20;0;")
                            .append(Int.MAX_VALUE)
                        text("DATA", builder.toString())
                        text("KEY", "class$index")
                        number("UID", 0)
                    }
                )
            }
            serverChannel.flush()

            // Challenge completion cheat
            serverChannel.writeAndFlush(
                clientPacket(Components.UTIL, Commands.USER_SETTINGS_SAVE, id++) {
                    val builder = StringBuilder("22")
                    repeat(221) { builder.append(",255") }
                    text("DATA", builder.toString())
                    text("KEY", "cscompletion")
                }
            )
        }
    }

    /**
     * Tries to forward the HTTP traffic that would normally go to the
     * official http servers to the localhost servers. This returns
     * true if the redirect was applied and false if not. This value
     * depends on the [forwardHttp] field
     *
     * @param packet The receieved packet
     * @return Whether to ignore sending this packet to the client
     */
    private fun tryForwardHttp(packet: Packet): Boolean {
        if (forwardHttp
            && packet.component == Components.UTIL
            && packet.command == Commands.FETCH_CLIENT_CONFIG) {
            val type = packet.text("CFID")
            if (type == "ME3_DATA") {
                clientChannel.writeAndFlush(
                    packet.respond {
                        map("CONF", Data.createDataConfig())
                    }
                )
                return true
            }
        }
        return false
    }

    /**
     * Handles reading the packets from the clent and writing
     * them to the official server channel. Also handles the
     * unlocking cheat logic
     *
     * @param ctx The channel handler context
     * @param msg The receieved packet
     */
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is Packet) return

        PacketLogger.log("DECODED FROM CLIENT", clientChannel, msg)

        tryUnlockCheat(msg)

        // Release the message when it's been written and flushed
        serverChannel.writeAndFlush(msg)
            .addListener { Packet.release(msg) }
    }
}
