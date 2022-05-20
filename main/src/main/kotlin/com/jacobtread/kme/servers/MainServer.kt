package com.jacobtread.kme.servers

import com.jacobtread.kme.Config
import com.jacobtread.kme.KME_VERSION
import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.Command.*
import com.jacobtread.kme.blaze.Component.*
import com.jacobtread.kme.blaze.tdf.StructTdf
import com.jacobtread.kme.blaze.tdf.UnionTdf
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.database.Player
import com.jacobtread.kme.database.Players
import com.jacobtread.kme.game.GameManager
import com.jacobtread.kme.game.PlayerSession
import com.jacobtread.kme.game.PlayerSession.NetData
import com.jacobtread.kme.utils.*
import com.jacobtread.kme.utils.logging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.IOException
import java.net.SocketException
import java.util.concurrent.atomic.AtomicInteger

fun startMainServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup, config: Config) {
    try {
        val port = config.ports.main
        val clientId = AtomicInteger(0)
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                    val remoteAddress = ch.remoteAddress()
                    Logger.info("Main started new client session with $remoteAddress")
                    val session = PlayerSession(clientId.getAndIncrement())
                    ch.pipeline()
                        // Add handler for decoding packet
                        .addLast(PacketDecoder())
                        // Add handler for processing packets
                        .addLast(MainClient(session, config))
                        .addLast(PacketEncoder())
                }
            })
            // Bind the server to the host and port
            .bind(port)
            // Wait for the channel to bind
            .addListener {
                Logger.info("Started Main Server on port $port")
            }
    } catch (e: IOException) {
        Logger.error("Exception in redirector server", e)
    }
}

@Suppress("SpellCheckingInspection")
private class MainClient(private val session: PlayerSession, private val config: Config) : SimpleChannelInboundHandler<Packet>() {

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
        session.channel = channel
    }

    @Deprecated("Deprecated in Java")
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        if (cause is SocketException) {
            if (cause.message?.startsWith("Connection reset") == true) {
                return
            }
        }
        cause.printStackTrace()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        session.isActive = false
        super.channelInactive(ctx)
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
        } catch (e: Exception) {
            Logger.warn("Failed to handle packet: $msg", e)
            respondEmpty(msg)
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
            ORIGIN_LOGIN -> {
                Logger.info("Recieved unsupported request for Origin Login")
                respondEmpty(packet)
            }
            CREATE_ACCOUNT -> handleCreateAccount(packet)
            else -> respondEmpty(packet)
        }
    }

    private fun handleListUserEntitlements2(packet: Packet) {
        val etag = packet.text("ETAG")
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
                    number("USID", session.id)
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
        val email: String = packet.text("MAIL")
        val password: String = packet.text("PASS")
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
        val pid: Long = packet.number("PID")
        val auth: String = packet.text("AUTH")
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
        val email: String = packet.text("MAIL")
        val password: String = packet.text("PASS")
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
        val sessionToken = getSessionToken()
        channel.respond(packet) {
            text("PNAM", player.displayName)
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
        sessionDetailsPackets()
    }

    private fun handleLoginPersona(packet: Packet) {
        val playerName: String = packet.text("PNAM")
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
        when (packet.command) {
            CREATE_GAME -> handleCreateGame(packet)
            ADVANCE_GAME_STATE -> handleAdvanceGameState(packet)
            SET_GAME_SETTINGS -> handleSetGameSettings(packet)
            SET_GAME_ATTRIBUTES -> handleSetGameAttributes(packet)
            REMOVE_PLAYER -> handleRemovePlayer(packet)
            START_MATCHMAKING -> handleStartMatchmaking(packet)
            CANCEL_MATCHMAKING -> handleCancelMatchmaking(packet)
            UPDATE_MESH_CONNECTION -> handleUpdateMeshConnection(packet)
            else -> respondEmpty(packet)
        }
    }

    private fun handleCreateGame(packet: Packet) {
        val player = session.getPlayer()
        val attributes = packet.mapKVOrNull("ATTR") ?: return
        val game = GameManager.createGame(session)
        game.attributes.setBulk(attributes)
        channel.respond(packet) { number("GID", game.id) }
        channel.send(game.createPoolPacket(true))
        channel.unique(USER_SESSIONS, START_SESSION) {
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
                list("ULST", listOf(VarTripple(0x4, 0x1, 0x5dc695)))
            }
            number("USID", player.playerId)
        }
    }

    private fun handleAdvanceGameState(packet: Packet) {
        val gameId = packet.number("GID")
        val gameState = packet.number("GSTA").toInt()
        val game = GameManager.getGameById(gameId)
        if (game != null) {
            game.gameState = gameState
        }
        respondEmpty(packet)
    }

    private fun handleSetGameSettings(packet: Packet) {
        val gameId = packet.number("GID")
        val setting = packet.number("GSET").toInt()
        val game = GameManager.getGameById(gameId)
        if (game != null) {
            game.gameSetting = setting
        }
        respondEmpty(packet)
        channel.unique(
            GAME_MANAGER,
            MIGRATE_ADMIN_PLAYER,
        ) {
            number("ATTR", setting)
            number("GID", gameId)
        }
    }

    private fun handleSetGameAttributes(packet: Packet) {
        val gameId = packet.number("GID")
        val attributes = packet.mapKVOrNull("ATTR")
        if (attributes == null) {
            respondEmpty(packet)
            return
        }
        val game = GameManager.getGameById(gameId)
        if (game != null) {
            game.attributes.setBulk(attributes)
            game.broadcastAttributeUpdate()
        }
        respondEmpty(packet)
    }

    private fun handleRemovePlayer(packet: Packet) {
        val playerId = packet.number("PID").toInt()
        val gameId = packet.number("GID")
        val game = GameManager.getGameById(gameId)
        game?.removePlayer(playerId)
        respondEmpty(packet)
    }

    private fun handleStartMatchmaking(packet: Packet) {
        session.waitingForJoin = true
        // TODO: Implement Proper Searching
        val game = GameManager.getFreeGame()
        if (game == null) {
            respondEmpty(packet)
            return
        }
        game.join(session)
        channel.respond(packet) {
            number("MSID", game.mid)
        }

        val creator = game.host.createMMSessionDetails(game)
        channel.send(creator)
        game.getActivePlayers().forEach {
            val sessionDetails = it.createMMSessionDetails(game)
            channel.send(sessionDetails)
        }
        channel.send(game.createPoolPacket(false))
    }

    private fun handleCancelMatchmaking(packet: Packet) {
        session.waitingForJoin = false
        respondEmpty(packet)
    }

    private fun handleUpdateMeshConnection(packet: Packet) {
        val gameId = packet.number("GID")
        val game = GameManager.getGameById(gameId)
        val player = session.getPlayer()
        if (game != null && session.waitingForJoin) {
            val host = game.host
            session.waitingForJoin = false
            val a = unique(GAME_MANAGER, GAME_MANAGER_74) {
                number("GID", gameId)
                number("PID", player.playerId)
                number("STAT", 4)
            }
            val b = unique(GAME_MANAGER, GAME_MANAGER_1E) {
                number("GID", gameId)
                number("PID", player.playerId)
            }
            val c = unique(GAME_MANAGER, GAME_MANAGER_CA) {
                number("ALST", player.playerId)
                number("GID", gameId)
                number("OPER", 0)
                number("UID", host.playerId)
            }
            channel.send(a, flush = false)
            channel.send(b, flush = false)
            channel.send(c)

            host.channel.send(a, flush = false)
            host.channel.send(b, flush = false)
            host.channel.send(c)
        } else {
            respondEmpty(packet)
        }
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
        val name: String = packet.text("NAME")
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
        val name: String = packet.text("NAME")
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
        transaction {
            val playerCount = Player.count()
            channel.respond(packet) {
                number("CNT", playerCount)
            }
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
                val menuMessage = config.menuMessage
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
        when (packet.command) {
            SUBMIT_OFFLINE_GAME_REPORT -> {
                respondEmpty(packet)
                channel.unique(GAME_REPORTING, GAME_REPORT_RESULT_72) {
                    varList("DATA", emptyList())
                    number("EROR", 0)
                    number("FNL", 0)
                    number("GHID", 0)
                    number("GRID", 0)
                }
            }
            else -> {}
        }
    }

    //endregion

    //region User Sessions Component Region

    private fun handleUserSessions(packet: Packet) {
        when (packet.command) {
            UPDATE_HARDWARE_FLAGS,
            UPDATE_NETWORK_INFO,
            -> {
                val addr: UnionTdf? = packet.unionOrNull("ADDR")
                if (addr != null) {
                    val value = addr.value as StructTdf
                    val inip: StructTdf = value.struct("INIP")
                    val port: Int = inip.numberInt("PORT")
                    val remoteAddress = channel.remoteAddress()
                    val addressEncoded = IPAddress.asLong(remoteAddress)
                    session.inip = NetData(addressEncoded, port)
                    session.exip = NetData(addressEncoded, port)
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
        val type = packet.text("CFID")
        if (type.startsWith("ME3_LIVE_TLK_PC_")) {
            val lang = type.substring(16)
            val tlk = Data.loadTLK(lang)
            channel.respond(packet) {
                map("CONF", tlk)
            }
        } else {
            val conf: Map<String, String> = when (type) {
                "ME3_DATA" -> Data.makeME3Data(config)
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
            // The following addresses have all been redirected to localhost to be ignored
            +struct("QOSS") {
                +struct("BWPS") {

                    // was gossjcprod-qos01.ea.com
                    text("PSA", "127.0.0.1")
                    number("PSP", 17502)
                    text("SNA", "prod-sjc")
                }

                number("LNP", 0xA)
                map("LTPS", mapOf(
                    "ea-sjc" to struct {
                        // was gossjcprod-qos01.ea.com
                        text("PSA", "127.0.0.1")
                        number("PSP", 17502)
                        text("SNA", "prod-sjc")
                    },
                    "rs-iad" to struct {
                        // was gosiadprod-qos01.ea.com
                        text("PSA", "127.0.0.1")
                        number("PSP", 17502)
                        text("SNA", "rs-prod-iad")
                    },
                    "rs-lhr" to struct {
                        // was gosgvaprod-qos01.ea.com
                        text("PSA", "127.0.0.1")
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

            //  telemetryAddress = "reports.tools.gos.ea.com:9988"
            //  tickerAddress = "waleu2.tools.gos.ea.com:8999"

            +struct("TELE") {
                text("ADRS", config.address) // Server Address
                number("ANON", 0)
                text("DISA", Data.TELE_DISA)
                text("FILT", "-UION/****") // Telemetry filter?
                number("LOC", 1701725253)
                text("NOOK", "US,CA,MX")
                number("PORT", config.ports.telemetry)
                number("SDLY", 15000)
                text("SESS", "JMhnT9dXSED")
                text("SKEY", Data.SKEY)
                number("SPCT", 0x4B)
                text("STIM", "")
            }

            +struct("TICK") {
                text("ADRS", config.address)
                number("port", config.ports.ticker)
                text("SKEY", "823287263,10.23.15.2:8999,masseffect-3-pc,10,50,50,50,50,0,12")
            }

            +struct("UROP") {
                number("TMOP", 0x1)
                number("UID", session.id)
            }
        }
    }

    private fun handleUserSettingsSave(packet: Packet) {
        val value = packet.textOrNull("DATA")
        val key = packet.textOrNull("KEY")
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
        when (packet.numberOrNull("TVAL")) {
            0x1312D00L -> channel.error(packet, 0x12D)
            0x55D4A80L -> channel.error(packet, 0x12E)
            else -> respondEmpty(packet)
        }
    }

    //endregion

}
