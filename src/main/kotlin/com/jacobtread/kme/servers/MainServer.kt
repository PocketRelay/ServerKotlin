package com.jacobtread.kme.servers

import com.jacobtread.kme.Config
import com.jacobtread.kme.LOGGER
import com.jacobtread.kme.blaze.PacketCommand
import com.jacobtread.kme.blaze.PacketComponent
import com.jacobtread.kme.blaze.PacketDecoder
import com.jacobtread.kme.blaze.RawPacket
import com.jacobtread.kme.blaze.builder.Packet
import com.jacobtread.kme.database.Database
import com.jacobtread.kme.database.repos.PlayersRepository
import com.jacobtread.kme.game.Player
import com.jacobtread.kme.utils.NULL_CHAR
import com.jacobtread.kme.utils.customThreadFactory
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.io.IOException


fun startMainServer(config: Config, database: Database) {
    Thread {
        val bossGroup = NioEventLoopGroup(customThreadFactory("Main Server Boss #{ID}"))
        val workerGroup = NioEventLoopGroup(customThreadFactory("Main Server Worker #{ID}"))
        val bootstrap = ServerBootstrap() // Create a server bootstrap
        try {
            val bind = bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<Channel>() {
                    override fun initChannel(ch: Channel) {
                        println("Main Server Connection")
                        ch.pipeline()
                            // Add handler for decoding packet
                            .addLast(PacketDecoder())
                            // Add handler for processing packets
                            .addLast(MainClient(config, database))
                    }
                })
                // Bind the server to the host and port
                .bind(config.host, config.ports.main)
                // Wait for the channel to bind
                .sync()
            LOGGER.info("Started Main Server (${config.host}:${config.ports.main})")
            bind.channel()
                // Get the closing future
                .closeFuture()
                // Wait for the closing
                .sync()
        } catch (e: IOException) {
            LOGGER.error("Exception in redirector server", e)
        }
    }.apply {
        // Name the main server thread
        name = "Main Server"
        // Close this thread when the JVM requests close
        isDaemon = true
        // Start the main server thread
        start()
    }
}

private class MainClient(private val config: Config, private val database: Database) : SimpleChannelInboundHandler<RawPacket>() {

    companion object {
        val CIDS = listOf(1, 24, 4, 28, 7, 9, 63490, 30720, 15, 30721, 30722, 30723, 30725, 30726, 2000)
    }

    lateinit var channel: Channel
    var player: Player? = null

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        this.channel = ctx.channel()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: RawPacket) {

        LOGGER.info("Incoming packet:")
        print(msg.toDebugString())
        when (msg.component) {
            PacketComponent.AUTHENTICATION -> handleAuthentication(msg)
            PacketComponent.GAME_MANAGER -> handleGameManager(msg)
            PacketComponent.STATS -> handleStats(msg)
            PacketComponent.MESSAGING -> handleMessaging(msg)
            PacketComponent.ASSOCIATION_LISTS -> handleAssociationLists(msg)
            PacketComponent.GAME_REPORTING -> handleGameReporting(msg)
            PacketComponent.USER_SESSIONS -> handleUserSessions(msg)
            PacketComponent.UTIL -> handleUtil(msg)
            else -> {}
        }
    }

    fun handleUtil(packet: RawPacket) {
        when (packet.command) {
            PacketCommand.PRE_AUTH -> handlePreAuth(packet)
            PacketCommand.POST_AUTH -> handlePostAuth(packet)
        }
    }

    fun handlePreAuth(packet: RawPacket) {
        val bootPacket = Packet(packet.component, packet.command, 0, 0x1000, packet.id) {
            Number("ANON", 0x0)
            Number("ASRC", 303107)
            List("CIDS", CIDS)
            Text("CNGN", "")
            Struct("CONF") {
                Map("CONF", mapOf(
                    "pingPeriod" to "15s",
                    "voipHeadsetUpdateRate" to "1000",
                    "xlspConnectionIdleTimeout" to "300"
                ))
            }
            Text("INST", "masseffect-3-pc")
            Number("MINR", 0x0)
            Text("NASP", "cem_ea_id")
            Text("PLID", "")
            Text("PLAT", "pc") // Platform
            Text("PTAG", "")
            Struct("QOSS") {
                Struct("BWPS") {
                    Text("PSA", "gossjcprod-qos01.ea.com")
                    Number("PSP", 17502)
                    Text("SNA", "prod-sjc")
                }

                Number("LNP", 0xA)
                Map("LTPS", mapOf(
                    "ea-sjc" to MakeStruct {
                        Text("PSA", "gossjcprod-qos01.ea.com")
                        Number("PSP", 17502)
                        Text("SNA", "prod-sjc")
                    },
                    "rs-iad" to MakeStruct {
                        Text("PSA", "gosiadprod-qos01.ea.com")
                        Number("PSP", 17502)
                        Text("SNA", "rs-prod-iad")
                    },
                    "rs-lhr" to MakeStruct {
                        Text("PSA", "gosgvaprod-qos01.ea.com")
                        Number("PSP", 17502)
                        Text("SNA", "rs-prod-lhr")
                    }
                ))
                Number("SVID", 0x45410805)
            }
            Text("RSRC", "303107")
            Text("SVER", "Blaze 3.15.08.0 (CL# 750727)") // Server Version
        }
        channel.writeAndFlush(bootPacket)
    }

    fun handlePostAuth(packet: RawPacket) {
        val bootPacket = Packet(packet.component, packet.command, 0, 0x1000, packet.id) {


            // Player Sync Service Details
            Struct("PSS") {
                Text("ADRS", "playersyncservice.ea.com") // Address
                Blob("CSIG") // Signature?
                Number("PJID", 303107)
                Number("PORT", 443) // Port
                Number("RPRT", 0xF)
                Number("TIID", 0x0)
            }

            Struct("TELE") {
                Text("ADRS", config.host)
                Number("ANON", 0)
            }

        }
        channel.writeAndFlush(bootPacket)
    }

    fun handleAuthentication(packet: RawPacket) {
        when (packet.command) {
            PacketCommand.LIST_USER_ENTITLEMENTS_2 -> {

            }
            PacketCommand.GET_AUTH_TOKEN -> {

            }
            PacketCommand.LOGIN -> {

            }
            PacketCommand.SILENT_LOGIN -> {

            }
            PacketCommand.LOGIN_PERSONA -> {

            }
            PacketCommand.ORIGIN_LOGIN -> {

            }
            PacketCommand.LOGOUT,
            PacketCommand.GET_PRIVACY_POLICY_CONTENT,
            PacketCommand.GET_LEGAL_DOCS_INFO,
            PacketCommand.GET_TERMS_OF_SERVICE_CONTENT,
            PacketCommand.ACCEPT_LEGAL_DOCS,
            PacketCommand.CHECK_AGE_REQUIREMENT,
            PacketCommand.CREATE_ACCOUNT,
            PacketCommand.CREATE_PERSONA,
            -> {

            }
        }
    }

    fun handleLogin(packet: RawPacket) {
        val content = packet.content
        val playerName = packet.getStringAt(1).trim()
        val password = packet.getStringAt(2).trim()
        if (playerName.isBlank() || password.isBlank()) {
            LoginErrorPacket(packet, LoginError.INVALID_INFORMATION)
            return
        }
        val playerRepo = database.playerRepository
        try {
            val player = playerRepo.getPlayer(playerName)
            if (!player.isMatchingPassword(password)) {
                LoginErrorPacket(packet, LoginError.WRONG_PASSWORD)
                return
            }
        } catch (e: PlayersRepository.PlayerNotFoundException) {
            LoginErrorPacket(packet, LoginError.INVALID_EMAIL)
        } catch (e: PlayersRepository.ServerErrorException) {
            LoginErrorPacket(packet, LoginError.SERVER_UNAVAILABLE)
        }
    }

    enum class LoginError(val value: Int) {
        SERVER_UNAVAILABLE(0x0),
        EMAIL_NOT_FOUND(0xB),
        WRONG_PASSWORD(0x0C),
        EMAIL_ALREADY_IN_USE(0x0F),
        AGE_RESTRICTION(0x10),
        INVALID_ACCOUNT(0x11),
        BANNED_ACCOUNT(0x13),
        INVALID_INFORMATION(0x15),
        INVALID_EMAIL(0x16),
        LEGAL_GUARDIAN_REQUIRED(0x2A),
        CODE_REQUIRED(0x32),
        KEY_CODE_ALREADY_IN_USE(0x33),
        INVALID_CERBERUS_KEY(0x34),
        SERVER_UNAVAILABLE_FINAL(0x4001),
        FAILED_NO_LOGIN_ACTION(0x4004),
        SERVER_UNAVAILABLE_NOTHING(0x4005),
        CONNECTION_LOST(0x4007)
    }

    fun LoginErrorPacket(packet: RawPacket, reason: LoginError) {
        channel.write(Packet(packet.component, packet.command, reason.value, 0x3000, packet.id) {
            Text("PNAM", "")
            Number("UID$NULL_CHAR", 0)
        })
        val remoteAddress = channel.remoteAddress()
        LOGGER.info("Client login failed for address $remoteAddress reason: $reason")
    }

    fun handleGameManager(packet: RawPacket) {

    }

    fun handleStats(packet: RawPacket) {

    }

    fun handleMessaging(packet: RawPacket) {

    }

    fun handleAssociationLists(packet: RawPacket) {

    }

    fun handleGameReporting(packet: RawPacket) {

    }

    fun handleUserSessions(packet: RawPacket) {

    }

}
