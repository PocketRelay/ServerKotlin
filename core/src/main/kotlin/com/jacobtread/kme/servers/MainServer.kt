package com.jacobtread.kme.servers

import com.jacobtread.kme.Config
import com.jacobtread.kme.LOGGER
import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.Command.*
import com.jacobtread.kme.blaze.Component.*
import com.jacobtread.kme.blaze.exception.UnexpectBlazePairException
import com.jacobtread.kme.blaze.utils.IPAddress
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.database.Database
import com.jacobtread.kme.database.repos.PlayerCreationException
import com.jacobtread.kme.database.repos.PlayerNotFoundException
import com.jacobtread.kme.database.repos.ServerErrorException
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
import java.nio.charset.StandardCharsets
import java.time.Instant
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
                        val remoteAddress = ch.remoteAddress()
                        val session = SessionData(
                            clientId.getAndIncrement(),
                            config.origin.uid,
                            NetData(0, 0),
                            NetData(0, 0)
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
    var userId: Int,
    val exip: NetData,
    val inip: NetData,
) {

    private var player: Player? = null
    var sendOffers: Boolean = false
    var lastPingTime: Long = -1L

    fun getPlayer(): Player = player!!
    fun hasPlayer(): Boolean = player != null

    fun setPlayer(player: Player) {
        this.player = player
    }

    fun createAddrUnion(label: String): UnionTdf =
        UnionTdf(label, 0x02, struct("VALU") {
            +struct("EXIP") {
                number("IP", exip.address)
                number("PORT", exip.port)
            }
            +struct("INIP") {
                number("IP", inip.address)
                number("PORT", inip.port)
            }
        })


}

data class NetData(var address: Long, var port: Int)

private class MainClient(private val session: SessionData, private val config: Config, private val database: Database) : SimpleChannelInboundHandler<RawPacket>() {

    companion object {
        private val EMPTY_BYTE_ARRAY = ByteArray(0)
        private val CIDS = listOf(1, 25, 4, 28, 7, 9, 63490, 30720, 15, 30721, 30722, 30723, 30725, 30726, 2000)
        private val TELE_DISA =
            "AD,AF,AG,AI,AL,AM,AN,AO,AQ,AR,AS,AW,AX,AZ,BA,BB,BD,BF,BH,BI,BJ,BM,BN,BO,BR,BS,BT,BV,BW,BY,BZ,CC,CD,CF,CG,CI,CK,CL,CM,CN,CO,CR,CU,CV,CX,DJ,DM,DO,DZ,EC,EG,EH,ER,ET,FJ,FK,FM,FO,GA,GD,GE,GF,GG,GH,GI,GL,GM,GN,GP,GQ,GS,GT,GU,GW,GY,HM,HN,HT,ID,IL,IM,IN,IO,IQ,IR,IS,JE,JM,JO,KE,KG,KH,KI,KM,KN,KP,KR,KW,KY,KZ,LA,LB,LC,LI,LK,LR,LS,LY,MA,MC,MD,ME,MG,MH,ML,MM,MN,MO,MP,MQ,MR,MS,MU,MV,MW,MY,MZ,NA,NC,NE,NF,NG,NI,NP,NR,NU,OM,PA,PE,PF,PG,PH,PK,PM,PN,PS,PW,PY,QA,RE,RS,RW,SA,SB,SC,SD,SG,SH,SJ,SL,SM,SN,SO,SR,ST,SV,SY,SZ,TC,TD,TF,TG,TH,TJ,TK,TL,TM,TN,TO,TT,TV,TZ,UA,UG,UM,UY,UZ,VA,VC,VE,VG,VN,VU,WF,WS,YE,YT,ZM,ZW,ZZ"
        private val SKEY = createSKey()

        private fun createSKey(): String {
           return String(byteArrayOf(
                94, -118, -53, -35, -8, -20, -63, -107, -104, -103, -7, -108, -64, -83, -18,
                -4, -50, -92, -121, -34, -118, -90, -50, -36, -80, -18, -24, -27, -77, -11,
                -83, -102, -78, -27, -28, -79, -103, -122, -57, -114, -101, -80, -12, -64, -127,
                -93, -89, -115, -100, -70, -62, -119, -45, -61, -84, -104, -106, -92, -32, -64,
                -127, -125, -122, -116, -104, -80, -32, -52, -119, -109, -58, -52, -102, -28, -56,
                -103, -29, -126, -18, -40, -105, -19, -62, -51, -101, -41, -52, -103, -77, -27,
                -58, -47, -21, -78, -90, -117, -72, -29, -40, -60, -95, -125, -58, -116, -100,
                -74, -16, -48, -63, -109, -121, -53, -78, -18, -120, -107, -46, -128, -128
            ), Charsets.UTF_8)
        }
    }

    fun empty(packet: RawPacket, qtype: Int = RESPONSE) = channel.send(RawPacket(packet.rawComponent, packet.rawCommand, 0, qtype, packet.id, EMPTY_BYTE_ARRAY))

    lateinit var channel: Channel

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        this.channel = ctx.channel()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: RawPacket) {
        try {
            when (msg.component) {
                AUTHENTICATION -> handleAuthentication(msg)
                GAME_MANAGER -> handleGameManager(msg)
                STATS -> handleStats(msg)
                MESSAGING -> handleMessaging(msg)
                ASSOCIATION_LISTS -> handleAssociationLists(msg)
                GAME_REPORTING -> handleGameReporting(msg)
                USER_SESSIONS -> handleUserSessions(msg)
                UTIL -> handleUtil(msg)
                else -> {}
            }
        } catch (e: Exception) {
            LOGGER.warn("Failed to handle packet: $msg", e)
        }
    }

    //region Authentication Component Region

    private fun handleAuthentication(packet: RawPacket) {
        when (packet.command) {
            LIST_USER_ENTITLEMENTS_2 -> handleListUserEntitlements2(packet)
            GET_AUTH_TOKEN -> handleGetAuthToken(packet)
            LOGIN -> handleLogin(packet)
            SILENT_LOGIN -> handleSilentLogin(packet)
            LOGIN_PERSONA -> handleLoginPersona(packet)
            ORIGIN_LOGIN -> {}
            CREATE_ACCOUNT -> handleCreateAccount(packet)
            LOGOUT,
            GET_PRIVACY_POLICY_CONTENT,
            GET_LEGAL_DOCS_INFO,
            GET_TERMS_OF_SERVICE_CONTENT,
            ACCEPT_LEGAL_DOCS,
            CHECK_AGE_REQUIREMENT,
            CREATE_PERSONA,
            -> empty(packet, 0x1000)
            else -> throw UnexpectBlazePairException()
        }
    }


    private fun handleListUserEntitlements2(packet: RawPacket) {
        val etag = packet.getValue(StringTdf::class, "ETAG")
        if (etag.isEmpty()) {
            channel.send(Data.makeUserEntitlements2(packet))

            if (!session.sendOffers) {
                session.sendOffers = true
                @Suppress("SpellCheckingInspection")
                val sessPacket = unique(USER_SESSIONS, START_SESSION) {
                    +struct("DATA") {
                        +session.createAddrUnion("ADDR")
                        text("BPS", "ea-sjc")
                        text("CTY", "")
                        varList("CVAR", emptyList())
                        map("DMAP", mapOf(0x70001 to 0x2e))
                        number("HWFG", 0x0)
                        list("PSLM", listOf(0xfff0fff, 0xfff0fff, 0xfff0fff))
                        +struct("QDAT") {
                            number("DBPS", 0x0)
                            number("NATT", config.natType)
                            number("UBPS", 0x0)
                        }
                        number("UATT", 0x0)
                    }
                    number("USID", session.userId)
                }
                channel.send(sessPacket)
                channel.send(sessPacket)
            }

        } else {
            empty(packet, 0x1000)
        }
    }

    private fun handleGetAuthToken(packet: RawPacket) {
    }

    private fun handleLogin(packet: RawPacket) {
        val email = packet.getValue(StringTdf::class, "MAIL")
        val password = packet.getValue(StringTdf::class, "PASS")
        if (email.isBlank() || password.isBlank()) {
            loginErrorPacket(packet, LoginError.INVALID_INFORMATION)
            return
        }

        val emailRegex = Regex("^[\\p{L}\\p{N}._%+-]+@[\\p{L}\\p{N}.\\-]+\\.\\p{L}{2,}$")
        if (!email.matches(emailRegex)) {
            loginErrorPacket(packet, LoginError.INVALID_EMAIL)
            return
        }

        val playerRepo = database.playerRepository
        try {
            val player = playerRepo.getPlayerByEmail(email)
            if (!player.isMatchingPassword(password)) {
                loginErrorPacket(packet, LoginError.WRONG_PASSWORD)
                return
            }

            session.setPlayer(player)

            val sessionToken = getSessionToken()
            val lastLoginTime = Instant.now().epochSecond

            channel.respond(packet) {
                text("LDHT", "")
                number("NTOS", 0)
                text("PCTK", sessionToken)

                list("PLST", listOf(
                    struct {
                        text("DSNM", player.displayName)
                        number("LAST", lastLoginTime)
                        number("PID", player.id)
                        number("STAS", 0)
                        number("XREF", 0)
                        number("XTYP", 0)
                    }
                ))

                text("PRIV", "")
                text("SKEY", "11229301_9b171d92cc562b293e602ee8325612e7")
                number("SPAM", 0)
                text("THST", "")
                text("TSUI", "")
                text("TURI", "")
                number("UID", player.id)
            }


        } catch (e: PlayerNotFoundException) {
            loginErrorPacket(packet, LoginError.EMAIL_NOT_FOUND)
        } catch (e: ServerErrorException) {
            loginErrorPacket(packet, LoginError.SERVER_UNAVAILABLE)
        }
    }

    private fun handleSilentLogin(packet: RawPacket) {
        val pid = packet.getValue(VarIntTdf::class, "PID")
        val auth = packet.getValue(StringTdf::class, "AUTH")
        try {
            val player = database.playerRepository.getPlayerByID(pid)
            if (player.sessionToken == auth) {
                session.setPlayer(player)
                authResponsePacket(packet)
                sessionDetailsPackets()
            } else {
                loginErrorPacket(packet, LoginError.INVALID_ACCOUNT)
            }
        } catch (e: PlayerNotFoundException) {
            loginErrorPacket(packet, LoginError.INVALID_ACCOUNT)
        } catch (e: ServerErrorException) {
            loginErrorPacket(packet, LoginError.SERVER_UNAVAILABLE)
        }
    }

    private fun createSessionToken(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPSQRSTUVWXYZ0123456789-"
        val output = StringBuilder()
        repeat(128) { output.append(chars.random()) }
        return output.toString()
    }

    private fun getSessionToken(): String {
        val player = session.getPlayer()
        var sessionToken = player.sessionToken
        if (sessionToken == null) {
            sessionToken = createSessionToken()
            database.playerRepository.setPlayerSessionToken(player, sessionToken)
        }
        return sessionToken
    }

    private fun authResponsePacket(packet: RawPacket) {
        val player = session.getPlayer()

        val sessionToken = getSessionToken()
        val lastLoginTime = Instant.now().epochSecond

        @Suppress("SpellCheckingInspection")
        channel.respond(packet) {
            number("AGUP", 0)
            text("LDHT", "")
            number("NTOS", 0)
            text("PCTK", sessionToken)
            text("PRIV", "")
            +struct("SESS") {
                number("BUID", player.id)
                number("FRST", 0)
                text("KEY", "11229301_9b171d92cc562b293e602ee8325612e7")
                number("LLOG", lastLoginTime)
                text("MAIL", player.email)
                +struct("PDTL") {
                    text("DSNM", player.displayName)
                    number("LAST", lastLoginTime)
                    number("PID", player.id)
                    number("STAS", 0)
                    number("XREF", 0)
                    number("XTYP", 0)
                }
                number("UID", player.id)
            }
            number("SPAM", 0)
            text("THST", "")
            text("TSUI", "")
            text("TURI", "")
        }
    }

    private fun sessionDetailsPackets() {
        val player = session.getPlayer()
        @Suppress("SpellCheckingInspection")
        channel.unique(
            USER_SESSIONS,
            SESSION_DETAILS,
        ) {
            +struct("DATA") {
                union("ADDR")
                text("BPS", "")
                text("CTY", "")
                varList("CVAR", emptyList())
                map("DMAP", mapOf(0x70001 to 0x22))
                number("HWFG", 0)

                +struct("QDAT") {
                    number("DBPS", 0)
                    number("NATT", config.natType)
                    number("UBPS", 0)
                }

                number("UATT", 0)
            }

            +struct("USER") {
                number("AID", player.id)
                number("ALOC", 0x64654445)
                blob("EXBB", EMPTY_BYTE_ARRAY)
                number("EXID", 0)
                number("ID", player.id)
                text("NAME", player.displayName)
            }
        }

        @Suppress("SpellCheckingInspection")
        channel.unique(
            USER_SESSIONS,
            UPDATE_EXTENDED_DATA_ATTRIBUTE
        ) {
            number("FLGS", 3)
            number("ID", player.id)
        }
    }

    private enum class LoginError(val value: Int) {
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

    private fun loginErrorPacket(packet: RawPacket, reason: LoginError) {
        @Suppress("SpellCheckingInspection")
        channel.error(packet, reason.value) {
            text("PNAM", "")
            number("UID", 0)
        }
    }

    private fun handleCreateAccount(packet: RawPacket) {
        val email = packet.getValue(StringTdf::class, "MAIL")
        val password = packet.getValue(StringTdf::class, "PASS")
        try {
            val player = database.playerRepository.createPlayer(email, email, password)
            session.setPlayer(player)
            @Suppress("SpellCheckingInspection")
            channel.respond(packet) {
                text("PNAM", player.displayName)
                number("UID", player.id)
            }
            sessionDetailsPackets()
        } catch (e: PlayerCreationException) {
            when (e.reason) {
                PlayerCreationException.Reason.EMAIL_TAKEN ->
                    loginErrorPacket(packet, LoginError.EMAIL_ALREADY_IN_USE)
                else -> {}
            }
            LOGGER.error("Failed to create player: ${e.message}")
        }
    }


    private fun handleLoginPersona(packet: RawPacket) {
        val player = session.getPlayer()
        val lastLoginTime = Instant.now().epochSecond
        @Suppress("SpellCheckingInspection")
        channel.respond(packet) {
            number("BUID", player.id)
            number("FRST", 0)
            text("KEY", "11229301_9b171d92cc562b293e602ee8325612e7")
            number("LLOG", lastLoginTime)
            text("MAIL", player.email)
            +struct("PDTL") {
                text("DSNM", player.displayName)
                number("LAST", lastLoginTime)
                number("PID", player.id)
                number("STAS", 0)
                number("XREF", 0)
                number("XTYP", 0)
            }
            number("UID", player.id)
        }
    }

    //endregion

    //region Game Manager Component Region

    private fun handleGameManager(packet: RawPacket) {

    }

    //endregion

    //region Stats Component Region

    private fun handleStats(packet: RawPacket) {

    }

    //endregion

    //region Messaging Component Region

    private fun handleMessaging(packet: RawPacket) {

    }

    //endregion

    //region Association Lists Component Region

    private fun handleAssociationLists(packet: RawPacket) {

    }

    //endregion

    //region Game Reporting Component Region

    fun handleGameReporting(packet: RawPacket) {

    }

    //endregion

    //region User Sessions Component Region

    private fun handleUserSessions(packet: RawPacket) {
        when (packet.command) {
            UPDATE_HARDWARE_FLAGS,
            UPDATE_NETWORK_INFO,
            -> {
                val addr = packet.get(UnionTdf::class, "ADDR")
                val value = addr.value as StructTdf
                val inip = value.get(StructTdf::class, "INIP")
                val port = inip.get(VarIntTdf::class, "PORT")
                val remoteAddress = channel.remoteAddress()
                val addressEncoded = IPAddress.asLong(remoteAddress)
                session.inip.address = addressEncoded
                session.inip.port = port.value.toInt()

                session.exip.address = addressEncoded
                session.exip.port = port.value.toInt()
            }
            else -> throw UnexpectBlazePairException()
        }
        empty(packet, 0x1000)
    }

    //endregion

    //region Util Component Region

    private fun handleUtil(packet: RawPacket) {
        when (packet.command) {
            FETCH_CLIENT_CONFIG -> handleFetchClientConfig(packet)
            PING -> handlePing(packet)
            PRE_AUTH -> handlePreAuth(packet)
            POST_AUTH -> handlePostAuth(packet)
            else -> throw UnexpectBlazePairException()
        }
    }

    private fun handleFetchClientConfig(packet: RawPacket) {
        val type = packet.getValue(StringTdf::class, "CFID")
        if (type.startsWith("ME3_LIVE_TLK_PC_")) {
            val lang = type.substring(16)
            val tlk = Data.loadTLK(lang)
            channel.respond(packet) {
                map("CONF", tlk)
            }
        } else {
            val conf: Map<String, String> = when (type) {
                "ME3_DATA" -> Data.makeME3Data()
                "ME3_MSG" -> Data.makeME3MSG()
                "ME3_ENT" -> Data.makeME3ENT()
                "ME3_DIME" -> Data.makeME3DIME()
                "ME3_BINI_VERSION" -> Data.makeBiniVersion()
                "ME3_BINI_PC_COMPRESSED" -> Data.loadBiniCompressed()
                else -> emptyMap()
            }
            channel.respond(packet) {
                map("CONF", conf)
            }
        }
    }

    private fun handlePing(packet: RawPacket) {
        LOGGER.logIfDebug { "Received ping update from client: ${session.id}" }
        session.lastPingTime = System.currentTimeMillis()
        channel.respond(packet) {
            // Server Time (in Epoch Seconds)
            number("STIM", Instant.now().epochSecond)
        }
    }

    private fun handlePreAuth(packet: RawPacket) {
        channel.respond(packet) {
            number("ANON", 0x0)
            number("ASRC", 303107)
            list("CIDS", CIDS)
            text("CNGN", "")
            +struct("CONF") {
                map(
                    "CONF", mapOf(
                        "pingPeriod" to "15s",
                        "voipHeadsetUpdateRate" to "1000",
                        "xlspConnectionIdleTimeout" to "300"
                    )
                )
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
    }

    private fun handlePostAuth(packet: RawPacket) {
        channel.respond(packet) {
            +struct("PSS") {
                text("ADRS", "playersyncservice.ea.com")
                blob("CSIG", EMPTY_BYTE_ARRAY)
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
                text("SKEY", SKEY)
                number("SPCT", 0x4B)
                text("STIM", "")
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
    }

    //endregion

}
