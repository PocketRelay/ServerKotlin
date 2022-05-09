package com.jacobtread.kme.servers

import com.jacobtread.kme.Config
import com.jacobtread.kme.LOGGER
import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.exception.InvalidTdfException
import com.jacobtread.kme.blaze.utils.NULL_CHAR
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.database.Database
import com.jacobtread.kme.database.repos.PlayersRepository
import com.jacobtread.kme.game.Player
import com.jacobtread.kme.utils.customThreadFactory
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger


fun startMainServer(config: Config, database: Database) {
    Thread {
        val bossGroup = NioEventLoopGroup(customThreadFactory("Main Server Boss #{ID}"))
        val workerGroup = NioEventLoopGroup(customThreadFactory("Main Server Worker #{ID}"))
        val bootstrap = ServerBootstrap() // Create a server bootstrap
        try {
            val clientId = AtomicInteger(0)
            val bind = bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<Channel>() {
                    override fun initChannel(ch: Channel) {
                        println("Main Server Connection")
                        val session = SessionData(
                            clientId.getAndIncrement()
                        )
                        ch.pipeline()
                            // Add handler for decoding packet
                            .addLast(PacketDecoder())
                            // Add handler for processing packets
                            .addLast(MainClient(session, config, database))
                            .addLast(PacketEncoder())
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

class SessionData(
    val id: Int,
    var player: Player? = null,
    var sendOffers: Boolean = false,
)

private class MainClient(private val session: SessionData, private val config: Config, private val database: Database) : SimpleChannelInboundHandler<RawPacket>() {

    companion object {
        val CIDS = listOf(1, 25, 4, 28, 7, 9, 63490, 30720, 15, 30721, 30722, 30723, 30725, 30726, 2000)
        val TELE_DISA =
            "AD,AF,AG,AI,AL,AM,AN,AO,AQ,AR,AS,AW,AX,AZ,BA,BB,BD,BF,BH,BI,BJ,BM,BN,BO,BR,BS,BT,BV,BW,BY,BZ,CC,CD,CF,CG,CI,CK,CL,CM,CN,CO,CR,CU,CV,CX,DJ,DM,DO,DZ,EC,EG,EH,ER,ET,FJ,FK,FM,FO,GA,GD,GE,GF,GG,GH,GI,GL,GM,GN,GP,GQ,GS,GT,GU,GW,GY,HM,HN,HT,ID,IL,IM,IN,IO,IQ,IR,IS,JE,JM,JO,KE,KG,KH,KI,KM,KN,KP,KR,KW,KY,KZ,LA,LB,LC,LI,LK,LR,LS,LY,MA,MC,MD,ME,MG,MH,ML,MM,MN,MO,MP,MQ,MR,MS,MU,MV,MW,MY,MZ,NA,NC,NE,NF,NG,NI,NP,NR,NU,OM,PA,PE,PF,PG,PH,PK,PM,PN,PS,PW,PY,QA,RE,RS,RW,SA,SB,SC,SD,SG,SH,SJ,SL,SM,SN,SO,SR,ST,SV,SY,SZ,TC,TD,TF,TG,TH,TJ,TK,TL,TM,TN,TO,TT,TV,TZ,UA,UG,UM,UY,UZ,VA,VC,VE,VG,VN,VU,WF,WS,YE,YT,ZM,ZW,ZZ"
        val SKEY = intArrayOf(
            0x5E, 0x8A, 0xCB, 0xDD, 0xF8, 0xEC, 0xC1, 0x95, 0x98, 0x99,
            0xF9, 0x94, 0xC0, 0xAD, 0xEE, 0xFC, 0xCE, 0xA4, 0x87, 0xDE,
            0x8A, 0xA6, 0xCE, 0xDC, 0xB0, 0xEE, 0xE8, 0xE5, 0xB3, 0xF5,
            0xAD, 0x9A, 0xB2, 0xE5, 0xE4, 0xB1, 0x99, 0x86, 0xC7, 0x8E,
            0x9B, 0xB0, 0xF4, 0xC0, 0x81, 0xA3, 0xA7, 0x8D, 0x9C, 0xBA,
            0xC2, 0x89, 0xD3, 0xC3, 0xAC, 0x98, 0x96, 0xA4, 0xE0, 0xC0,
            0x81, 0x83, 0x86, 0x8C, 0x98, 0xB0, 0xE0, 0xCC, 0x89, 0x93,
            0xC6, 0xCC, 0x9A, 0xE4, 0xC8, 0x99, 0xE3, 0x82, 0xEE, 0xD8,
            0x97, 0xED, 0xC2, 0xCD, 0x9B, 0xD7, 0xCC, 0x99, 0xB3, 0xE5,
            0xC6, 0xD1, 0xEB, 0xB2, 0xA6, 0x8B, 0xB8, 0xE3, 0xD8, 0xC4,
            0xA1, 0x83, 0xC6, 0x8C, 0x9C, 0xB6, 0xF0, 0xD0, 0xC1, 0x93,
            0x87, 0xCB, 0xB2, 0xEE, 0x88, 0x95, 0xD2, 0x80, 0x80
        )
    }

    lateinit var channel: Channel
    var player: Player? = null

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        this.channel = ctx.channel()
    }

    fun send(packet: RawPacket) {
        channel.write(packet)
        channel.flush()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: RawPacket) {
        try {
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
        } catch (e: InvalidTdfException) {
            LOGGER.warn("Failed to handle packet $msg", e)
        }
    }

    fun handleUtil(packet: RawPacket) {
        when (packet.command) {
            PacketCommand.PRE_AUTH -> handlePreAuth(packet)
            PacketCommand.POST_AUTH -> handlePostAuth(packet)
        }
    }

    fun handlePreAuth(packet: RawPacket) {
        val bootPacket = packet(packet.component, packet.command, 0, 0x1000, packet.id) {
            number("ANON", 0x0)
            number("ASRC", 303107)
            list("CIDS", CIDS)
            text("CNGN", "")
            +struct("CONF") {
                map("CONF", mapOf(
                    "pingPeriod" to "15s",
                    "voipHeadsetUpdateRate" to "1000",
                    "xlspConnectionIdleTimeout" to "300"
                ))
            }
            text("INST", "masseffect-3-pc")
            number("MINR", 0x0)
            text("NASP", "cem_ea_id")
            text("PILD", "")
            text("PLAT", "pc") // Platform
            text("PTAG", "")
            +struct("QOSS") {
                +struct("BWPS") {
                    text("PSA", "gossjcprod-qos01.ea.com")
                    number("PSP", 17502)
                    text("SNA", "prod-sjc")
                }

                number("LNP", 0xA)
                map("LTPS", mapOf(
                    "ea-sjc" to struct {
                        text("PSA", "gossjcprod-qos01.ea.com")
                        number("PSP", 17502)
                        text("SNA", "prod-sjc")
                    },
                    "rs-iad" to struct {
                        text("PSA", "gosiadprod-qos01.ea.com")
                        number("PSP", 17502)
                        text("SNA", "rs-prod-iad")
                    },
                    "rs-lhr" to struct {
                        text("PSA", "gosgvaprod-qos01.ea.com")
                        number("PSP", 17502)
                        text("SNA", "rs-prod-lhr")
                    }
                ))
                number("SVID", 0x45410805)
            }
            text("RSRC", "303107")
            text("SVER", "Blaze 3.15.08.0 (CL# 750727)") // Server Version
        }
        send(bootPacket)
    }

    fun handlePostAuth(packet: RawPacket) {
        val bootPacket = packet(packet.component, packet.command, 0, 0x1000, packet.id) {
            +struct("PSS") {
                text("ADRS", "playersyncservice.ea.com")
                blob("CSIG", ByteArray(0))
                number("PJID", 303107)
                number("PORT", 443)
                number("RPRT", 0xF)
                number("TIID", 0x0)
            }

            +struct("TELE") {
                text("ADRS", config.host) // Server Address
                number("ANON", 0)
                text("DISA", TELE_DISA)
                text("FILT", "-UION/****") // Telemetry filter?
                number("LOC", 1701725253)
                text("NOOK", "US,CA,MX")
                number("PORT", config.ports.telemetry)
                number("SDLY", 15000)
                text("SESS", "JMhnT9dXSED")
                val skey = StringBuilder()
                SKEY.forEach { skey.append(it.toChar()) }
                println(skey.toString())
                number("SPCT", 0x4B)
                number("STIM", 0)
            }

            +struct("TICK") {
                text("ADRS", config.host)
                number("port", config.ports.ticker)
                text("SKEY", "823287263,10.23.15.2:8999,masseffect-3-pc,10,50,50,50,50,0,12")
            }

            +struct("UROP") {
                number("TMOP", 0x1)
                number("UID", session.id)
            }
        }
        send(bootPacket)
    }

    fun handleAuthentication(packet: RawPacket) {
        when (packet.command) {
            PacketCommand.LIST_USER_ENTITLEMENTS_2 -> handleListUserEntitlements2(packet)
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

    fun handleListUserEntitlements2(packet: RawPacket) {
        val etag = packet.getValue(StringTdf::class, "ETAG")
        if (etag.isEmpty()) {
            send(Data.makeUserEntitlements2(packet.id))
            if (!session.sendOffers) {
                session.sendOffers = true
                @Suppress("SpellCheckingInspection")
                val sessPacket = packet(PacketComponent.USER_SESSIONS, PacketCommand.START_SESSION, 0x2000, 0x0) {
                    struct("DATA") {
                        union("ADDR",
                            0x2,
                            struct("VALU") {
                                struct("EXIP") {
                                    number("IP", 0x5b32c8e4)
                                    number("PORT", 3659)
                                }
                                struct("INIP") {
                                    number("IP", 0xc0a8b220)
                                    number("PORT", 3659)
                                }
                            }
                        )
                        text("BPS", "ea-sjc")
                        text("CTY", "")

                        map("DMAP", mapOf(0x70001 to 0x2e))
                        number("HWFG", 0x0)
                        list("PSLM", listOf(0xfff0fff, 0xfff0fff, 0xfff0fff))
                        struct("QDAT") {
                            number("DBPS", 0x0)
                            number("NATT", 0x4)
                            number("UBPS", 0x0)
                        }
                        number("UATT", 0x0)
                    }
                    number("USID", 0x31125ddf)
                }
            }
        } else {
            sendEmpty(packet, 0x1000)
        }
    }

    fun sendEmpty(packet: RawPacket, qtype: Int) {
        send(RawPacket(packet.rawComponent, packet.rawCommand, 0, qtype, packet.id, ByteArray(0)))
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
        channel.write(packet(packet.component, packet.command, reason.value, 0x3000, packet.id) {
            text("PNAM", "")
            number("UID$NULL_CHAR", 0)
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
