import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.tdf.GroupTdf
import com.jacobtread.kme.utils.IPAddress
import com.jacobtread.kme.utils.logging.Level
import com.jacobtread.kme.utils.logging.Logger
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import java.util.concurrent.ThreadFactory

fun main() {
    getOfficalServerDetails()
}

data class ServerDetails(
    val host: String,
    val ip: String,
    val port: Int,
    val secure: Boolean,
)

fun getOfficalServerDetails(): ServerDetails {
    val redirectorHost = "gosredirector.ea.com"
    val redirectorPort = 42127
    val handler = ClientHandler()
    val channelFuture = Bootstrap()
        .group(NioEventLoopGroup(1, ThreadFactory { r ->
            val thread = Thread(r)
            thread.name = "Server Details Worker"
            thread.isDaemon = true
            thread
        }))
        .channel(NioSocketChannel::class.java)
        .handler(handler)
        .connect(redirectorHost, redirectorPort)
        .sync()
    val channel = channelFuture.channel()
    Logger.info("Connected to server")
    Logger.info("Sending REDIRECTOR / GET_SERVER_INSTANCE packet")
    val packet = createRedirectPacket()
    channel.write(packet)
    channel.flush()
    Logger.info("Waiting for closed response")
    channel.closeFuture().sync()
    val serverDetails = handler.serverDetails
    requireNotNull(serverDetails) { "Server details were null" }
    Logger.info("Obtained server details: $serverDetails")

    return serverDetails
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

class ClientHandler : ChannelInboundHandlerAdapter() {
    var serverDetails: ServerDetails? = null

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        val context = SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build()
        val channel = ctx.channel()
        channel.pipeline()
            .addFirst(PacketDecoder())
            .addFirst(context.newHandler(channel.alloc()))
            .addLast(PacketEncoder())
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
            serverDetails = ServerDetails(host, IPAddress.fromULongStr(ip) ,port.toInt(), secure)
            ctx.close()
        }
    }
}
