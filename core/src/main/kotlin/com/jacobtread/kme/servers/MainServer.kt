package com.jacobtread.kme.servers

import com.jacobtread.kme.CONFIG
import com.jacobtread.kme.KME_VERSION
import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.Command.*
import com.jacobtread.kme.blaze.Component.*
import com.jacobtread.kme.blaze.tdf.StructTdf
import com.jacobtread.kme.blaze.tdf.UnionTdf
import com.jacobtread.kme.blaze.utils.IPAddress
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.database.Player
import com.jacobtread.kme.database.Players
import com.jacobtread.kme.logging.Logger
import com.jacobtread.kme.utils.comparePasswordHash
import com.jacobtread.kme.utils.customThreadFactory
import com.jacobtread.kme.utils.hashPassword
import com.jacobtread.kme.utils.unixTimeSeconds
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.IOException
import java.sql.Struct
import java.util.concurrent.atomic.AtomicInteger


fun startMainServer() {
    Thread {
        val bossGroup = NioEventLoopGroup(customThreadFactory("Main Server Boss #{ID}"))
        val workerGroup = NioEventLoopGroup(customThreadFactory("Main Server Worker #{ID}"))
        val bootstrap = ServerBootstrap() // Create a server bootstrap
        try {
            val port = CONFIG.ports.main
            val clientId = AtomicInteger(0)
            val bind = bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<Channel>() {
                    override fun initChannel(ch: Channel) {
                        println("Main Server Connection")
                        val session = SessionData(
                            clientId.getAndIncrement(),
                            0x12345678,
                            NetData(0, 0),
                            NetData(0, 0)
                        )
                        ch.pipeline()
                            // Add handler for decoding packet
                            .addLast(PacketDecoder())
                            // Add handler for processing packets
                            .addLast(MainClient(session))
                            .addLast(PacketEncoder())
                    }
                })
                // Bind the server to the host and port
                .bind(port)
                // Wait for the channel to bind
                .sync()
            Logger.info("Started Main Server on port $port")
            bind.channel()
                // Get the closing future
                .closeFuture()
                // Wait for the closing
                .sync()
        } catch (e: IOException) {
            Logger.error("Exception in redirector server", e)
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

    fun getPlayer(): Player = player ?: throw IllegalStateException("Tried to access player on session without logging in")


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

    fun createPDTL(player: Player): StructTdf = struct("PDTL") {
        val lastLoginTime = unixTimeSeconds()
        text("DSNM", player.displayName)
        number("LAST", lastLoginTime)
        number("PID", player.id.value)
        number("STAS", 0)
        number("XREF", 0)
        number("XTYP", 0)
    }

}

data class NetData(var address: Long, var port: Int)

@Suppress("SpellCheckingInspection")
private class MainClient(private val session: SessionData) : SimpleChannelInboundHandler<Packet>() {

    companion object {
        private val EMPTY_BYTE_ARRAY = ByteArray(0)
    }

    /**
     * respondEmpty Used for sending a respond that has no content
     *
     * @param packet The packet to respond to
     */
    fun respondEmpty(packet: Packet) = channel.respond(packet)

    lateinit var channel: Channel

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        this.channel = ctx.channel()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet) {
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
                else -> respondEmpty(msg)
            }
        } catch (e: IllegalStateException) {
            Logger.warn("Failed to handle packet: $msg", e)
        } catch (e: MissingTdfException) {
            Logger.warn("Failed to handle packet: $msg", e)
            // TODO: Custom handling for missing tdfs
        }  catch (e: InvalidTdfException) {
            Logger.warn("Failed to handle packet: $msg", e)
            // TODO: Custom handling for invalid tdfs
        } catch (e: Exception) {
            Logger.warn("Failed to handle packet: $msg", e)
        }
    }

    //region Authentication Component Region

    private fun handleAuthentication(packet: Packet) {
        when (packet.command) {
            LIST_USER_ENTITLEMENTS_2 -> handleListUserEntitlements2(packet)
            GET_AUTH_TOKEN -> handleGetAuthToken(packet)
            LOGIN -> handleLogin(packet)
            SILENT_LOGIN -> handleSilentLogin(packet)
            LOGIN_PERSONA -> handleLoginPersona(packet)
            ORIGIN_LOGIN -> {}
            CREATE_ACCOUNT -> handleCreateAccount(packet)
            else -> respondEmpty(packet)
        }
    }


    private fun handleListUserEntitlements2(packet: Packet) {
        val etag: String = packet["ETAG"]
        if (etag.isEmpty()) {
            channel.send(Data.makeUserEntitlements2(packet))

            if (!session.sendOffers) {
                session.sendOffers = true
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
                            number("NATT", Data.NAT_TYPE)
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
            respondEmpty(packet)
        }
    }

    private fun handleGetAuthToken(packet: Packet) {
        val player = session.getPlayer()
        channel.respond(packet) {
            text("AUTH", player.id.value.toString(16).uppercase())
        }
    }

    private fun handleLogin(packet: Packet) {
        val email: String = packet["MAIL"]
        val password: String = packet["PASS"]
        if (email.isBlank() || password.isBlank()) {
            loginErrorPacket(packet, LoginError.INVALID_INFORMATION)
            return
        }

        val emailRegex = Regex("^[\\p{L}\\p{N}._%+-]+@[\\p{L}\\p{N}.\\-]+\\.\\p{L}{2,}$")
        if (!email.matches(emailRegex)) {
            loginErrorPacket(packet, LoginError.INVALID_EMAIL)
            return
        }

        val player = transaction {
            Player
                .find { Players.email eq email }
                .limit(1)
                .firstOrNull()
        }

        if (player == null) {
            loginErrorPacket(packet, LoginError.EMAIL_NOT_FOUND)
            return
        }

        if (!comparePasswordHash(password, player.password)) {
            loginErrorPacket(packet, LoginError.WRONG_PASSWORD)
            return
        }

        session.setPlayer(player)
        val sessionToken = getSessionToken()
        channel.respond(packet) {
            text("LDHT", "")
            number("NTOS", 0)
            text("PCTK", sessionToken)
            list("PLST", listOf(session.createPDTL(player)))
            text("PRIV", "")
            text("SKEY", "11229301_9b171d92cc562b293e602ee8325612e7")
            number("SPAM", 0)
            text("THST", "")
            text("TSUI", "")
            text("TURI", "")
            number("UID", player.id.value)
        }
    }

    private fun handleSilentLogin(packet: Packet) {
        val pid: Long = packet["PID"]
        val auth: String = packet["AUTH"]
        val player = transaction { Player.findById(pid.toInt()) }
        if (player == null) {
            loginErrorPacket(packet, LoginError.INVALID_ACCOUNT)
            return
        }

        if (player.sessionToken == auth) {
            session.setPlayer(player)
            authResponsePacket(packet)
            sessionDetailsPackets()
        } else {
            loginErrorPacket(packet, LoginError.INVALID_ACCOUNT)
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
            transaction {
                player.sessionToken = sessionToken
            }
        }
        return sessionToken
    }

    private fun authResponsePacket(packet: Packet) {
        val player = session.getPlayer()

        val sessionToken = getSessionToken()
        val lastLoginTime = unixTimeSeconds()

        channel.respond(packet) {
            number("AGUP", 0)
            text("LDHT", "")
            number("NTOS", 0)
            text("PCTK", sessionToken)
            text("PRIV", "")
            +struct("SESS") {
                number("BUID", player.id.value)
                number("FRST", 0)
                text("KEY", "11229301_9b171d92cc562b293e602ee8325612e7")
                number("LLOG", lastLoginTime)
                text("MAIL", player.email)
                +session.createPDTL(player)
                number("UID", player.id.value)
            }
            number("SPAM", 0)
            text("THST", "")
            text("TSUI", "")
            text("TURI", "")
        }
    }

    private fun sessionDetailsPackets() {
        val player = session.getPlayer()
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
                    number("NATT", Data.NAT_TYPE)
                    number("UBPS", 0)
                }

                number("UATT", 0)
            }

            +struct("USER") {
                number("AID", player.id.value)
                number("ALOC", 0x64654445)
                blob("EXBB", EMPTY_BYTE_ARRAY)
                number("EXID", 0)
                number("ID", player.id.value)
                text("NAME", player.displayName)
            }
        }

        channel.unique(
            USER_SESSIONS,
            UPDATE_EXTENDED_DATA_ATTRIBUTE
        ) {
            number("FLGS", 3)
            number("ID", player.id.value)
        }
    }

    @Suppress("unused")
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

    private fun loginErrorPacket(packet: Packet, reason: LoginError) {
        channel.error(packet, reason.value) {
            text("PNAM", "")
            number("UID", 0)
        }
    }

    private fun handleCreateAccount(packet: Packet) {
        val email: String = packet["MAIL"]
        val password: String = packet["PASS"]
        // Check for existing emails
        if (transaction { !Player.find { Players.email eq email }.limit(1).empty() }) {
            loginErrorPacket(packet, LoginError.EMAIL_ALREADY_IN_USE)
        }

        val hashedPassword = hashPassword(password)

        val player = transaction {
            Player.new {
                this.email = email
                this.displayName = email
                this.password = hashedPassword
            }
        }
        session.setPlayer(player)
        channel.respond(packet) {
            text("PNAM", player.displayName)
            number("UID", player.id.value)
        }
        sessionDetailsPackets()
    }

    private fun handleLoginPersona(packet: Packet) {
        val playerName: String = packet["PNAM"]
        val player = session.getPlayer()
        if (playerName != player.displayName) {
            return
        }
        val lastLoginTime = unixTimeSeconds()
        channel.respond(packet) {
            number("BUID", player.id.value)
            number("FRST", 0)
            text("KEY", "11229301_9b171d92cc562b293e602ee8325612e7")
            number("LLOG", lastLoginTime)
            text("MAIL", "")
            +session.createPDTL(player)
            number("UID", player.id.value)
        }
        sessionDetailsPackets()
    }

    //endregion

    //region Game Manager Component Region

    private fun handleGameManager(packet: Packet) {

    }

    //endregion

    //region Stats Component Region

    private fun handleStats(packet: Packet) {
        when (packet.command) {
            GET_LEADERBOARD_GROUP -> handleLeaderboardGroup(packet)
            GET_FILTERED_LEADERBOARD -> handleFilteredLeaderboard(packet)
            GET_LEADERBOARD_ENTITY_COUNT -> handleLeaderboardEntityCount(packet)
            GET_CENTERED_LEADERBOARD -> handleCenteredLeadboard(packet)
            else -> {}
        }
    }

    private fun getLocalName(code: String): String {
        return when (code.lowercase()) {
            "global" -> "Global"
            "de" -> "Germany"
            "en" -> "English"
            "es" -> "Spain"
            "fr" -> "France"
            "it" -> "Italy"
            "ja" -> "Japan"
            "pl" -> "Poland"
            "ru" -> "Russia"
            else -> code
        }
    }

    private fun handleLeaderboardGroup(packet: Packet) {
        val name: String = packet.getOrNull("NAME") ?: return
        val isN7 = name.startsWith("N7Rating")
        if (isN7 || name.startsWith("ChallengePoints")) {
            val locale: String = name.substring(if (isN7) 8 else 15)
            val isGlobal = locale == "Global"
            val localeName = getLocalName(locale)
            val ksvl: Map<Int, Int>
            val lbsz: Int
            if (isGlobal) {
                ksvl = mapOf(0x0 to 0x0)
                lbsz = 0x7270e0
            } else {
                ksvl = mapOf(0x4445 to 0x4445)
                lbsz = 0x8a77c
            }
            val desc: String
            val sname: String
            val sdsc: String
            val gname: String
            if (isN7) {
                desc = "N7 Rating - $localeName"
                sname = "n7rating"
                sdsc = "N7 Rating"
                gname = "ME3LeaderboardGroup"
            } else {
                desc = "Challenge Points - $localeName"
                sname = "ChallengePoints"
                sdsc = "Challenge Points"
                gname = "ME3ChallengePoints"
            }
            channel.respond(packet) {
                number("ACSD", 0x0)
                text("BNAM", name)
                text("DESC", desc)
                pair("ETYP", 0x7802, 0x1)
                map("KSUM", mapOf(
                    "accountcountry" to struct {
                        map("KSVL", ksvl)
                    }
                ))
                number("LBSZ", lbsz)
                list("LIST", listOf(
                    struct {
                        text("CATG", "MassEffectStats")
                        text("DFLT", "0")
                        number("DRVD", 0x0)
                        text("FRMT", "%d")
                        text("KIND", "")
                        text("LDSC", sdsc)
                        text("META", "W=200, HMC=tableColHeader3, REMC=tableRowEntry3")
                        text("NAME", sname)
                        text("SDSC", sdsc)
                        number("TYPE", 0x0)
                    }
                ))
                text("META", "RF=@W=150, HMC=tableColHeader1, REMC=tableRowEntry1@ UF=@W=670, HMC=tableColHeader2, REMC=tableRowEntry2@")
                text("NAME", gname)
                text("SNAM", sname)
            }
        } else {
            respondEmpty(packet)
        }

    }

    private fun handleFilteredLeaderboard(packet: Packet) {
        val name: String = packet.getOrNull("NAME") ?: return
        val player = session.getPlayer()
        when (name) {
            "N7RatingGlobal" -> {
                val rating = player.getN7Rating().toString()
                channel.respond(packet) {
                    list("LDLS", listOf(
                        struct {
                            text("ENAM", player.displayName)
                            number("ENID", player.id.value)
                            number("RANK", 0x58c86)
                            text("RSTA", rating)
                            number("RWFG", 0x0)
                            union("RWST")
                            list("STAT", listOf(rating))
                            number("UATT", 0x0)
                        }
                    ))
                }
            }
            "ChallengePointsGlobal" -> {
                val challengePoints = "0"
                channel.respond(packet) {
                    list("LDLS", listOf(
                        struct {
                            text("ENAM", player.displayName)
                            number("ENID", player.id.value)
                            number("RANK", 0x48f8c)
                            text("RSTA", challengePoints)
                            number("RWFG", 0x0)
                            union("RWST")
                            list("STAT", listOf(challengePoints))
                            number("UATT", 0x0)
                        }
                    ))
                }
            }
            else -> respondEmpty(packet)
        }
    }

    private fun handleLeaderboardEntityCount(packet: Packet) {
        val playerCount = Player.count()
        channel.respond(packet) {
            number("CNT", playerCount)
        }
    }

    private fun handleCenteredLeadboard(packet: Packet) {
        // TODO: Currenlty not implemented
        channel.respond(packet) {
            list("LDLS", emptyList<StructTdf>())
        }
    }

    //endregion

    //region Messaging Component Region

    private fun handleMessaging(packet: Packet) {
        when (packet.command) {
            FETCH_MESSAGES -> {
                channel.write(respond(packet) {
                    number("MCNT", 0x1)
                })
                val ip = channel.remoteAddress().toString()
                val player = session.getPlayer()
                val menuMessage = CONFIG.menuMessage
                    .replace("{v}", KME_VERSION)
                    .replace("{n}", player.displayName)
                    .replace("{ip}", ip) + 0xA.toChar()
                channel.write(unique(
                    MESSAGING,
                    SEND_MESSAGE
                ) {
                    number("FLAG", 0x01)
                    number("MGID", 0x01)
                    text("NAME", menuMessage)
                    +struct("PYLD") {
                        map("ATTR", mapOf("B0000" to "160"))
                        number("FLAG", 0x01)
                        number("STAT", 0x00)
                        number("TAG", 0x00)
                        tripple("TARG", 0x7802, 0x01, player.id.value.toLong())
                        number("TYPE", 0x0)
                    }
                    tripple("SRCE", 0x7802, 0x01, player.id.value.toLong())
                    number("TIME", unixTimeSeconds())
                })

                channel.flush()
            }
            else -> {}
        }
    }

    //endregion

    //region Association Lists Component Region

    private fun handleAssociationLists(packet: Packet) {
        when (packet.command) {
            GET_LISTS -> {
                channel.respond(packet) {
                    list("LMAP", listOf(
                        struct {
                            +struct("INFO") {
                                tripple("BOID", 0x19, 0x1, 0x28557f3)
                                number("FLGS", 4)
                                +struct("LID") {
                                    text("LNM", "friendList")
                                    number("TYPE", 1)
                                }
                                number("LMS", 0xC8)
                                number("PRID", 0)
                            }
                            number("OFRC", 0)
                            number("TOCT", 0)
                        }
                    ))
                }
            }
            ADD_USERS_TO_LIST,
            REMOVE_USERS_FROM_LIST,
            CLEAR_LISTS,
            SET_USERS_TO_LIST,
            GET_LIST_FOR_USER,
            SUBSCRIBE_TO_LISTS,
            UNSUBSCRIBE_FROM_LISTS,
            GET_CONFIG_LISTS_INFO,
            -> respondEmpty(packet)
            else -> return
        }
    }

    //endregion

    //region Game Reporting Component Region

    fun handleGameReporting(packet: Packet) {

    }

    //endregion

    //region User Sessions Component Region

    private fun handleUserSessions(packet: Packet) {
        when (packet.command) {
            UPDATE_HARDWARE_FLAGS,
            UPDATE_NETWORK_INFO,
            -> {
                val addr: UnionTdf? = packet.getTdfOrNull("ADDR")
                if (addr != null) {
                    val value = addr.value as StructTdf
                    val inip: StructTdf = value.getTdf("INIP")
                    val port: Long = inip["PORT"]
                    val remoteAddress = channel.remoteAddress()
                    val addressEncoded = IPAddress.asLong(remoteAddress)
                    session.inip.address = addressEncoded
                    session.inip.port = port.toInt()

                    session.exip.address = addressEncoded
                    session.exip.port = port.toInt()
                }
            }
            else -> {}
        }
        respondEmpty(packet)
    }

    //endregion

    //region Util Component Region

    private fun handleUtil(packet: Packet) {
        when (packet.command) {
            FETCH_CLIENT_CONFIG -> handleFetchClientConfig(packet)
            PING -> handlePing(packet)
            PRE_AUTH -> handlePreAuth(packet)
            POST_AUTH -> handlePostAuth(packet)
            USER_SETTINGS_SAVE -> handleUserSettingsSave(packet)
            USER_SETTINGS_LOAD_ALL -> handleUserSettingsLoadAll(packet)
            SET_CLIENT_METRICS -> respondEmpty(packet)
            SUSPEND_USER_PING -> handleSuspendUserPing(packet)
            else -> respondEmpty(packet)
        }
    }

    private fun handleFetchClientConfig(packet: Packet) {
        val type: String = packet["CFID"]
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

    private fun handlePing(packet: Packet) {
        Logger.logIfDebug { "Received ping update from client: ${session.id}" }
        session.lastPingTime = System.currentTimeMillis()
        channel.respond(packet) {
            number("STIM", unixTimeSeconds())
        }
    }

    private fun handlePreAuth(packet: Packet) {
        channel.respond(packet) {
            number("ANON", 0x0)
            number("ASRC", 303107)
            list("CIDS", Data.CIDS)
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

    private fun handlePostAuth(packet: Packet) {
        channel.respond(packet) {
            +struct("PSS") {
                text("ADRS", "playersyncservice.ea.com")
                blob("CSIG", EMPTY_BYTE_ARRAY)
                text("PJID", "303107")
                number("PORT", 443)
                number("RPRT", 0xF)
                number("TIID", 0x0)
            }

            val telemetryAddress = "127.0.0.1"
            val tickerAddress = "127.0.0.1"

            +struct("TELE") {
                text("ADRS", telemetryAddress) // Server Address
                number("ANON", 0)
                text("DISA", Data.TELE_DISA)
                text("FILT", "-UION/****") // Telemetry filter?
                number("LOC", 1701725253)
                text("NOOK", "US,CA,MX")
                number("PORT", CONFIG.ports.telemetry)
                number("SDLY", 15000)
                text("SESS", "JMhnT9dXSED")
                text("SKEY", Data.SKEY)
                number("SPCT", 0x4B)
                text("STIM", "")
            }

            +struct("TICK") {
                text("ADRS", tickerAddress)
                number("port", CONFIG.ports.ticker)
                text("SKEY", "823287263,10.23.15.2:8999,masseffect-3-pc,10,50,50,50,50,0,12")
            }

            +struct("UROP") {
                number("TMOP", 0x1)
                number("UID", session.id)
            }
        }
    }

    private fun handleUserSettingsSave(packet: Packet) {
        val value: String? = packet.getOrNull("DATA")
        val key: String? = packet.getOrNull("KEY")
        if (value != null && key != null) {
            val player = session.getPlayer()
            player.setSetting(key, value)

        }
        respondEmpty(packet)
    }

    private fun handleUserSettingsLoadAll(packet: Packet) {
        val player = session.getPlayer()
        channel.respond(packet) {
            map("SMAP", player.createSettingsMap())
        }
    }

    private fun handleSuspendUserPing(packet: Packet) {
        val value: Long? = packet.getOrNull("TVAL")
        if (value != null) {
            if (value == 0x1312D00L) {
                channel.error(packet, 0x12D)
            } else if (value == 0x55D4A80L) {
                channel.error(packet, 0x12E)
            }
        }
    }

    //endregion

}
