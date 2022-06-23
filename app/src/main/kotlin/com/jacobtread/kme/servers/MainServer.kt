package com.jacobtread.kme.servers

import com.jacobtread.kme.Environment
import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.tdf.GroupTdf
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.data.LoginError
import com.jacobtread.kme.database.Message
import com.jacobtread.kme.database.Player
import com.jacobtread.kme.exceptions.NotAuthenticatedException
import com.jacobtread.kme.game.GameManager
import com.jacobtread.kme.game.PlayerSession
import com.jacobtread.kme.game.PlayerSession.NetData
import com.jacobtread.kme.game.match.MatchRuleSet
import com.jacobtread.kme.game.match.Matchmaking
import com.jacobtread.kme.utils.IPAddress
import com.jacobtread.kme.utils.comparePasswordHash
import com.jacobtread.kme.utils.hashPassword
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.logging.Logger.info
import com.jacobtread.kme.utils.unixTimeSeconds
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.IOException
import java.net.SocketException
import java.time.LocalDate

/**
 * startMainServer Starts the main server
 *
 * @param bossGroup The netty boss event loop group
 * @param workerGroup The netty worker event loop group
 */
fun startMainServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {
    try {
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(MainInitializer())
            // Bind the server to the host and port
            .bind(Environment.Config.ports.main)
            // Wait for the channel to bind
            .addListener { info("Started Main Server on port ${Environment.Config.ports.main}") }
    } catch (e: IOException) {
        Logger.error("Exception in redirector server", e)
    }
}

/**
 * MainInitializer Channel Initializer for main server clients.
 * Creates sessions for the user as well as adding packet handlers
 * and the MainClient handler
 *
 * @constructor Create empty MainClientInitializer
 */
@Sharable
class MainInitializer : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        val remoteAddress = ch.remoteAddress() // The remote address of the user
        val session = PlayerSession()
        info("Main started new client session with $remoteAddress given id ${session.sessionId}")
        ch.pipeline()
            // Add handler for decoding packet
            .addLast(PacketDecoder())
            // Add handler for processing packets
            .addLast(MainHandler(session))
            .addLast(PacketEncoder())
    }
}

/**
 * MainHandler A handler for clients connected to the main server
 *
 * @property session The session data for this user
 * @constructor Create empty MainClient
 */
private class MainHandler(
    private val session: PlayerSession,
) : SimpleChannelInboundHandler<Packet>(), PacketPushable {

    inline fun Packet.pushResponse(init: ContentInitializer) = push(respond(init))
    fun Packet.pushEmptyResponse() = push(respond())
    fun Packet.pushEmptyError(error: Int) = push(error(error))
    override fun push(packet: Packet) {
        channel.write(packet)
    }

    lateinit var channel: Channel

    /**
     * channelActive Handles when the channel becomes active. Sets
     * the local channel and sets it on the player session aswell
     *
     * @param ctx The channel context
     */
    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        this.channel = ctx.channel()
        session.setChannel(channel)
    }

    /**
     * exceptionCaught Exception handling for unhandled exceptions in the
     * channel pipline. Messages that aren't connection resets are logged
     *
     * @param ctx The channel context
     * @param cause The exception cause
     */
    @Deprecated("Deprecated in Java")
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        if (cause is SocketException) {
            if (cause.message?.startsWith("Connection reset") == true) {
                return
            }
        }
        Logger.warn("Exception in MainServer", cause)
    }

    /**
     * channelInactive Handles when the channel becomes inactive. Used
     * to release and cleanup the session so that it can be released
     * by the garbage collector
     *
     * @param ctx The channel context
     */
    override fun channelInactive(ctx: ChannelHandlerContext) {
        session.release()
        super.channelInactive(ctx)
    }

    /**
     * channelRead0 Handles routing recieved packets to their desired
     * handler functions
     *
     * @param ctx The channel context
     * @param msg The reieved packet
     */
    override fun channelRead0(ctx: ChannelHandlerContext, msg: Packet) {
        try {
            when (msg.component) {
                Components.AUTHENTICATION -> handleAuthentication(msg)
                Components.GAME_MANAGER -> handleGameManager(msg)
                Components.STATS -> handleStats(msg)
                Components.MESSAGING -> handleMessaging(msg)
                Components.ASSOCIATION_LISTS -> handleAssociationLists(msg)
                Components.GAME_REPORTING -> handleGameReporting(msg)
                Components.USER_SESSIONS -> handleUserSessions(msg)
                Components.UTIL -> handleUtil(msg)
                else -> pushEmptyResponse(msg)
            }
        } catch (e: NotAuthenticatedException) { // Handle player access with no player
            val address = channel.remoteAddress()
            push(LoginError.INVALID_ACCOUNT(msg))
            Logger.warn("Client at $address tried to access a authenticated route without authenticating")
        } catch (e: Exception) {
            Logger.warn("Failed to handle packet: $msg", e)
            msg.pushEmptyResponse()
        }
        ctx.flush()
        msg.release() // Release content from message at end of handling
    }

    //region Authentication Component Region

    /**
     * handleAuthentication Handles the authentication component and passes
     * the call onto the other functions that handle the commmands
     *
     * @param packet The packet with an authentication component
     */
    private fun handleAuthentication(packet: Packet) {
        when (packet.command) {
            Commands.LIST_USER_ENTITLEMENTS_2 -> handleListUserEntitlements2(packet)
            Commands.GET_AUTH_TOKEN -> handleGetAuthToken(packet)
            Commands.LOGIN -> handleLogin(packet)
            Commands.SILENT_LOGIN -> handleSilentLogin(packet)
            Commands.LOGIN_PERSONA -> handleLoginPersona(packet)
            Commands.ORIGIN_LOGIN -> handleOriginLogin(packet)
            Commands.CREATE_ACCOUNT -> handleCreateAccount(packet)
            Commands.LOGOUT -> handleLogout(packet)
            else -> packet.pushEmptyResponse()
        }
    }

    /**
     * handleOriginLogin This would be the functionality for logging in Via origin
     * however this is currently not supported because I have no way of testing it,
     * so it's just going to send an empty response
     *
     * @param packet The packet requesting the origin login
     */
    private fun handleOriginLogin(packet: Packet) {
        info("Recieved unsupported request for Origin Login")
        packet.pushEmptyResponse()
    }

    /**
     * handleLogout Handles logging out a player which clears the references
     * to the player and removes them from any games they are in
     *
     * @param packet The packet requesting the logout
     */
    private fun handleLogout(packet: Packet) {
        val player = session.player
        info("Logged out player ${player.displayName}")
        session.setAuthenticated(null)
        packet.pushEmptyResponse()
    }

    /**
     * handleListUserEntitlements2 Sends back the player a list of "entitlements"
     * (content,offers,etc) that the player has access to these values are currently
     * static and stored in Data.
     *
     * @param packet The packet requesting the user entitlements
     */
    private fun handleListUserEntitlements2(packet: Packet) {
        val etag = packet.text("ETAG")
        if (etag.isNotEmpty()) { // Empty responses for packets with ETAG's
            return packet.pushEmptyResponse()
        }

        // Respond with the entitlements
        packet.pushResponse { Data.createUserEntitlements(this) }

        // If the player hasn't yet recieved their session info send them it
        if (!session.sendSession) {
            session.sendSession = true
            // Send a set session packet for the current player
            push(session.createSetSession())
        }
    }

    /**
     * handleGetAuthToken Returns the auth token used when making the GAW
     * authentication request. TODO: Replace this with a proper auth token
     *
     * @param packet The packet requesting the auth token
     */
    private fun handleGetAuthToken(packet: Packet) {
        val response = packet.respond {
            text("AUTH", session.playerId.toString(16).uppercase())
        }
        channel.write(response)
    }

    /**
     * handleLogin Handles email + password authentication from the login prompt
     * in the menu. Simple as that. NOTE: It would be possible to allow using a
     * username instead of an email by simply removing the email validation
     *
     * @param packet The packet requesting login
     */
    private fun handleLogin(packet: Packet) {
        val email: String = packet.text("MAIL")
        val password: String = packet.text("PASS")
        if (email.isBlank() || password.isBlank()) { // If we are missing email or password
            return push(LoginError.INVALID_ACCOUNT(packet))
        }

        // Regex for matching emails
        val emailRegex = Regex("^[\\p{L}\\p{N}._%+-]+@[\\p{L}\\p{N}.\\-]+\\.\\p{L}{2,}$")
        if (!email.matches(emailRegex)) { // If the email is not a valid email
            return push(LoginError.INVALID_EMAIL(packet))
        }

        // Retrieve the player with this email or send an email not found error
        val player = Player.getByEmail(email) ?: return push(LoginError.EMAIL_NOT_FOUND(packet))

        // Compare the provided password with the hashed password of the player
        if (!comparePasswordHash(password, player.password)) { // If it's not the same password
            return push(LoginError.WRONG_PASSWORD(packet))
        }

        session.setAuthenticated(player) // Set the authenticated session

        // Send the authenticated response with a persona list
        packet.pushResponse {
            text("LDHT", "")
            number("NTOS", 0)
            text("PCTK", player.sessionToken)
            list("PLST", listOf(session.createPersonaList()))
            text("PRIV", "")
            text("SKEY", Data.SKEY2)
            number("SPAM", 0)
            text("THST", "")
            text("TSUI", "")
            text("TURI", "")
            number("UID", player.id.value)
        }
    }

    /**
     * handleSilentLogin Handles behind the scene token logins this is what the client does when
     * it attempts to reconnect with a previously used token or the token provided by the last auth.
     *
     * For accounts that don't exist the INVALID_ACCOUNT error is used to kick the stored
     * credentials out of the session to give the user a change to log in to a new account
     * (This is the best option I could come up with that stil included session token validation)
     *
     * @param packet The packet requesting silent login
     */
    private fun handleSilentLogin(packet: Packet) {
        val pid: ULong = packet.number("PID")
        val auth: String = packet.text("AUTH")
        // Find the player with a matching ID or send an INVALID_ACCOUNT error
        val player = Player.getById(pid) ?: return push(LoginError.INVALID_ACCOUNT(packet))
        // If the session token's don't match send INVALID_ACCOUNT error
        if (!player.isSessionToken(auth)) return push(LoginError.INVALID_ACCOUNT(packet))
        val sessionToken = player.sessionToken // Session token grabbed after auth as to not generate new one
        session.setAuthenticated(player)
        // We don't store last login time so this is just computed here
        packet.pushResponse {
            number("AGUP", 0)
            text("LDHT", "")
            number("NTOS", 0)
            text("PCTK", sessionToken)
            text("PRIV", "")
            +group("SESS") { session.appendPlayerSession(this) }
            number("SPAM", 0)
            text("THST", "")
            text("TSUI", "")
            text("TURI", "")
        }
        push(session.createSessionDetails())
        push(session.createIdentityUpdate())
    }

    /**
     * handleCreateAccount Handles the creation of accounts from the in game account greation tool
     * most of the data provided is discarded only the email and password are used
     *
     * @param packet The packet requesting account creation
     */
    private fun handleCreateAccount(packet: Packet) {
        val email: String = packet.text("MAIL")
        val password: String = packet.text("PASS")
        if (Player.isEmailTaken(email)) { // Check if the email is already in use
            return push(LoginError.EMAIL_ALREADY_IN_USE(packet))
        }
        val hashedPassword = hashPassword(password) // Hash the password
        // Create the new player account
        val player = transaction {
            Player.new {
                this.email = email
                this.displayName = email
                this.password = hashedPassword
            }
        }
        session.setAuthenticated(player) // Link the player to this session
        packet.pushResponse {
            text("LDHT", "")
            number("NTOS", 0)
            text("PCTK", player.sessionToken)
            list("PLST", listOf(session.createPersonaList()))
            text("PRIV", "")
            text("SKEY", Data.SKEY2)
            number("SPAM", 0)
            text("THST", "")
            text("TSUI", "")
            text("TURI", "")
            number("UID", player.id.value)
        }
    }

    /**
     * handleLoginPersona Handles the logging in to a persona this ignores the persona name
     * field and just sends the session details of the player
     *
     * @param packet The packet requesting persona login
     */
    private fun handleLoginPersona(packet: Packet) {
        packet.pushResponse {
            session.appendPlayerSession(this)
        }
        push(session.createSessionDetails())
        push(session.createIdentityUpdate())
    }

    //endregion

    //region Game Manager Component Region

    /**
     * handleGameManager Handles commands under the GAME_MANAGER component this handles
     * things such as game creation, managements, and matchmaking
     *
     * @param packet The packet with a GAME_MANAGER component
     */
    private fun handleGameManager(packet: Packet) {
        when (packet.command) {
            Commands.CREATE_GAME -> handleCreateGame(packet)
            Commands.ADVANCE_GAME_STATE -> handleAdvanceGameState(packet)
            Commands.SET_GAME_SETTINGS -> handleSetGameSettings(packet)
            Commands.SET_GAME_ATTRIBUTES -> handleSetGameAttributes(packet)
            Commands.REMOVE_PLAYER -> handleRemovePlayer(packet)
            Commands.START_MATCHMAKING -> handleStartMatchmaking(packet)
            Commands.CANCEL_MATCHMAKING -> handleCancelMatchmaking(packet)
            Commands.UPDATE_MESH_CONNECTION -> handleUpdateMeshConnection(packet)
            else -> packet.pushEmptyResponse()
        }
    }

    /**
     * handleCreateGame Handles creating a game based on the provided attributes
     * and then tells the client the details of the created game
     *
     * @param packet The packet requesting creation of a game
     */
    private fun handleCreateGame(packet: Packet) {
        val attributes = packet.mapOrNull<String, String>("ATTR") // Get the provided users attributes
        val game = GameManager.createGame(session) // Create a new game

        val hostNetworking = packet.listOrNull<GroupTdf>("HNET")
        if (hostNetworking != null) {
            val first = hostNetworking.firstOrNull()
            if (first != null) session.setNetworkingFromHNet(first)
        }

        game.setAttributes(attributes ?: emptyMap()) // If the attributes are missing use empty
        packet.pushResponse { number("GID", game.id) }
        push(game.createPoolPacket(true)) // Send the game pool details
        push(session.createSetSession()) // Send the user session
        channel.flush()
        Matchmaking.onGameCreated(game)
    }

    /**
     * handleAdvanceGameState Handles updating the state of a game from the host
     * TODO: Validate that this packet is actually coming from the host?
     * Updates the game state of a the game with the id matching GID to the GSTA value
     *
     * @param packet The packet requesting game state change
     */
    private fun handleAdvanceGameState(packet: Packet) {
        val gameId = packet.number("GID")
        val gameState = packet.number("GSTA").toInt()
        val game = GameManager.getGameById(gameId)
        if (game != null) {
            game.gameState = gameState
        }
        packet.pushEmptyResponse()
    }

    /**
     * handleSetGameSettings Handles updating the game setting... from the packet command
     * used it appears that this could be having a different functionality?
     *
     * @param packet The packet requesting set game settings
     */
    private fun handleSetGameSettings(packet: Packet) {
        val gameId = packet.number("GID")
        val setting = packet.number("GSET").toInt()
        val game = GameManager.getGameById(gameId)
        if (game != null) {
            game.gameSetting = setting
        }
        packet.pushEmptyResponse()
        pushUnique(Components.GAME_MANAGER, Commands.MIGRATE_ADMIN_PLAYER) {
            number("ATTR", setting)
            number("GID", gameId)
        }
    }

    /**
     * handleSetGameAttributes Handles changing the attributes of the provided
     * game that matches GID the newly changed attributes are then broadcasted
     * to all the game players.
     *
     * @param packet The packet requesting attribute changes
     */
    private fun handleSetGameAttributes(packet: Packet) {
        val gameId = packet.number("GID")
        val attributes = packet.mapOrNull<String, String>("ATTR") ?: return packet.pushEmptyResponse()
        val game = GameManager.getGameById(gameId)
        if (game != null) {
            game.setAttributes(attributes)
            game.broadcastAttributeUpdate()
        }
        packet.pushEmptyResponse()
    }

    /**
     * handleRemovePlayer Handles removing the player with the
     * provided PID from the game
     *
     * @param packet The packet requesting the player removal
     */
    private fun handleRemovePlayer(packet: Packet) {
        val playerId = packet.number("PID").toInt()
        val gameId = packet.number("GID")
        val game = GameManager.getGameById(gameId)
        game?.removePlayer(playerId)
        packet.pushEmptyResponse()
    }

    /**
     * handleStartMatchmaking Handles finding a match for the current player session
     * this doesn't search based on the provided attributes isntead just finding the
     * first game with an open slot and connecting to that
     *
     * @param packet The packet requesting to start matchmaking
     */
    private fun handleStartMatchmaking(packet: Packet) {
        session.matchmaking = true
        val player = session.player
        info("Player ${player.displayName} started match making")

        val ruleSet = MatchRuleSet(packet)
        val game = Matchmaking.getMatchOrQueue(session, ruleSet) ?: return packet.pushEmptyResponse()
        info("Found matching game for player ${player.displayName}")
        game.join(session)
        packet.pushResponse {
            number("MSID", game.mid)
            number("GID", game.id)
        }
        game.getActivePlayers().forEach {
            if (it.sessionId != session.sessionId) {
                push(it.createSessionDetails())
            }
        }
        push(game.createPoolPacket(false))
    }

    /**
     * handleCancelMatchmaking Handles canceling matchmaking this just removes
     * the player from any existing games and sets waiting for join to false
     *
     * @param packet The packet requesting to cancel matchmaking
     */
    private fun handleCancelMatchmaking(packet: Packet) {
        val player = session.player
        info("Player ${player.displayName} cancelled match making")
        Matchmaking.removeFromQueue(session)
        session.leaveGame()
        packet.pushEmptyResponse()
    }

    /**
     * handleUpdateMeshConnection Functionality unknown but appears to tell
     * the host and players about games and player linking?
     *
     * @param packet The packet requesting the update mesh connection
     */
    private fun handleUpdateMeshConnection(packet: Packet) {
        val gameId = packet.number("GID")
        val game = GameManager.getGameById(gameId)
        if (game == null || !session.matchmaking) {
            return packet.pushEmptyResponse()
        }
        val player = session.player
        val host = game.host
        session.matchmaking = false
        val a = unique(Components.GAME_MANAGER, Commands.GAME_MANAGER_74) {
            number("GID", gameId)
            number("PID", player.playerId)
            number("STAT", 4)
        }
        val b = unique(Components.GAME_MANAGER, Commands.GAME_MANAGER_1E) {
            number("GID", gameId)
            number("PID", player.playerId)
        }
        val c = unique(Components.GAME_MANAGER, Commands.GAME_MANAGER_CA) {
            number("ALST", player.playerId)
            number("GID", gameId)
            number("OPER", 0)
            number("UID", host.playerId)
        }

        // Retain all the content buffers so they can be sent to the host aswell
        a.contentBuffer.retain()
        b.contentBuffer.retain()
        c.contentBuffer.retain()

        pushAll(a, b, c)
        host.pushAll(a, b, c)
        packet.pushEmptyResponse()
    }

    //endregion

    //region Stats Component Region

    /**
     * handleStats Handles commands under the STATS component handles
     * functionality for the leaderboard logic
     *
     * @param packet The packet with the STATS component
     */
    private fun handleStats(packet: Packet) {
        when (packet.command) {
            Commands.GET_LEADERBOARD_GROUP -> handleLeaderboardGroup(packet)
            Commands.GET_FILTERED_LEADERBOARD -> handleFilteredLeaderboard(packet)
            Commands.GET_LEADERBOARD_ENTITY_COUNT -> handleLeaderboardEntityCount(packet)
            Commands.GET_CENTERED_LEADERBOARD -> handleCenteredLeadboard(packet)
            else -> packet.pushEmptyResponse()
        }
    }

    /**
     * getLocaleName Translates the provided locale name
     * to the user readable name
     *
     * @param code The shorthand code for the locale name
     * @return The human readable locale name
     */
    private fun getLocaleName(code: String): String = when (code.lowercase()) {
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

    /**
     * handleLeaderboardGroup TODO: NOT IMPLEMENTED PROPERLY
     *
     * @param packet Packet requesting a leaderboard group
     */
    private fun handleLeaderboardGroup(packet: Packet) {
        val name: String = packet.text("NAME")
        val isN7 = name.startsWith("N7Rating")
        if (isN7 || name.startsWith("ChallengePoints")) {
            val locale: String = name.substring(if (isN7) 8 else 15)
            val isGlobal = locale == "Global"
            val localeName = getLocaleName(locale)
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
            packet.pushResponse {
                number("ACSD", 0x0)
                text("BNAM", name)
                text("DESC", desc)
                pair("ETYP", 0x7802, 0x1)
                map("KSUM", mapOf(
                    "accountcountry" to group {
                        map("KSVL", ksvl)
                    }
                ))
                number("LBSZ", lbsz)
                list("LIST", listOf(
                    group {
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
            packet.pushEmptyResponse()
        }
    }

    /**
     * handleFilteredLeaderboard TODO: NOT IMPLEMENTED PROPERLY
     *
     * @param packet The packet requesting a filtered leaderboard
     */
    private fun handleFilteredLeaderboard(packet: Packet) {
        val name: String = packet.text("NAME")
        val player = session.player
        when (name) {
            "N7RatingGlobal" -> {
                val rating = player.getN7Rating().toString()
                packet.pushResponse {
                    list("LDLS", listOf(
                        group {
                            text("ENAM", player.displayName)
                            number("ENID", player.id.value)
                            number("RANK", 0x58c86)
                            text("RSTA", rating)
                            number("RWFG", 0x0)
                            optional("RWST")
                            list("STAT", listOf(rating))
                            number("UATT", 0x0)
                        }
                    ))
                }
            }
            "ChallengePointsGlobal" -> {
                val challengePoints = "0"
                packet.pushResponse {
                    list("LDLS", listOf(
                        group {
                            text("ENAM", player.displayName)
                            number("ENID", player.id.value)
                            number("RANK", 0x48f8c)
                            text("RSTA", challengePoints)
                            number("RWFG", 0x0)
                            optional("RWST")
                            list("STAT", listOf(challengePoints))
                            number("UATT", 0x0)
                        }
                    ))
                }
            }
            else -> packet.pushEmptyResponse()
        }
    }

    /**
     * handleLeaderboardEntityCount Handles telling the client how many
     * "entites" (players) are on the leaderboard in this case it's just
     * the total number of players
     *
     * @param packet The packet requesting the leadboard entity count
     */
    private fun handleLeaderboardEntityCount(packet: Packet) {
        val playerCount = transaction { Player.count() }
        packet.pushResponse {
            number("CNT", playerCount)
        }
    }

    /**
     * handleCenteredLeadboard Returns a centered leaderboard TODO: NOT IMPLEMENTED
     *
     * @param packet The packet requesting a centered leaderboard
     */
    private fun handleCenteredLeadboard(packet: Packet) {
        // TODO: Currenlty not implemented
        packet.pushResponse {
            list("LDLS", emptyList<GroupTdf>())
        }
    }

    //endregion

    //region Messaging Component Region

    /**
     * handleMessaging Handles the ingame message retrieval in this case its
     * only used for getting the message that's displayed on the main menu
     *
     * @param packet The packet with the MESSAGING component
     */
    private fun handleMessaging(packet: Packet) {
        when (packet.command) {
            Commands.FETCH_MESSAGES -> handleFetchMessages(packet)
            else -> packet.pushEmptyResponse()
        }
    }

    /**
     * handleFetchMessages Handles fetch messages requests from the client this
     * will send a MESSAGING SEND_MESSAGE packet to the client containing the
     * generated main menu message
     *
     * @param packet The packet requesting the messages
     */
    private fun handleFetchMessages(packet: Packet) {
        packet.pushResponse { number("MCNT", 0x1) } // Number of messages
        val ip = channel.remoteAddress().toString()
        val player = session.player
        val menuMessage = Environment.Config.menuMessage
            .replace("{v}", Environment.KME_VERSION)
            .replace("{n}", player.displayName)
            .replace("{ip}", ip) + 0xA.toChar()
        pushUnique(Components.MESSAGING, Commands.SEND_MESSAGE) {
            number("FLAG", 0x01)
            number("MGID", 0x01)
            text("NAME", menuMessage)
            +group("PYLD") {
                map("ATTR", mapOf("B0000" to "160"))
                number("FLAG", 0x01)
                number("STAT", 0x00)
                number("TAG", 0x00)
                tripple("TARG", 0x7802, 0x01, player.playerId.toLong())
                number("TYPE", 0x0)
            }
            tripple("SRCE", 0x7802, 0x01, player.playerId.toLong())
            number("TIME", unixTimeSeconds())
        }
    }


    //endregion

    //region Association Lists Component Region

    /**
     * handleAssociationLists Handles getting assocation lists in this case
     * only the friends list will be returned
     *
     * @param packet The packet requesting ASSOCIATION_LISTS
     */
    private fun handleAssociationLists(packet: Packet) {
        when (packet.command) {
            Commands.GET_LISTS -> handleAssociationListGetLists(packet)
            else -> packet.pushEmptyResponse()
        }
    }

    /**
     * handleAssociationListGetLists Handles getting associated lists.
     * functionality appears to be for friends lists?
     *
     * @param packet The packet requesting the list
     */
    private fun handleAssociationListGetLists(packet: Packet) {
        packet.pushResponse {
            list("LMAP", listOf(
                group {
                    +group("INFO") {
                        tripple("BOID", 0x19, 0x1, 0x28557f3)
                        number("FLGS", 4)
                        +group("LID") {
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

    //endregion

    //region Game Reporting Component Region

    /**
     * handleGameReporting Handles logic for game reporting this appears to only
     * handle the offline report submission
     *
     * @param packet The packet requesting the GAME_REPORTING
     */
    private fun handleGameReporting(packet: Packet) {
        packet.pushEmptyResponse()
        if (packet.command == Commands.SUBMIT_OFFLINE_GAME_REPORT) {
            pushUnique(Components.GAME_REPORTING, Commands.GAME_REPORT_RESULT_72) {
                varList("DATA")
                number("EROR", 0)
                number("FNL", 0)
                number("GHID", 0)
                number("GRID", 0)
            }
        }
    }

    //endregion

    //region User Sessions Component Region

    /**
     * handleUserSessions Handles updating the session of the user in this
     * case we only update the networking information from the client
     *
     * @param packet The packet requesting a session change
     */
    private fun handleUserSessions(packet: Packet) {
        when (packet.command) {
            Commands.UPDATE_HARDWARE_FLAGS,
            Commands.UPDATE_NETWORK_INFO,
            -> updateSessionNetworkInfo(packet)
            Commands.RESUME_SESSION -> handleResumeSession(packet)
            else -> packet.pushEmptyResponse()
        }
    }

    /**
     * handleResumeSession Handles resuming a previously existing client
     * session using a session token provided by the client. Checks the
     * database for any players with the provided session token and if one
     * is found that is set as the authenticated session
     *
     * @param packet The packet requesting a session resumption
     */
    private fun handleResumeSession(packet: Packet) {
        val sessionKey = packet.text("SKEY");
        val player = Player.getBySessionKey(sessionKey) ?: return push(LoginError.INVALID_INFORMATION(packet))
        session.setAuthenticated(player) // Set the authenticated session
        packet.pushEmptyResponse()
    }

    /**
     * updateSessionNetworkInfo Updates the session networking information
     * provided from the client
     *
     * @param packet The packet requesting the updated networking info
     */
    private fun updateSessionNetworkInfo(packet: Packet) {
        val addr: GroupTdf? = packet.unionValueOrNull("ADDR") as GroupTdf?
        if (addr != null) {
            val inip: GroupTdf = addr.group("INIP")
            val port: ULong = inip.number("PORT")
            val remoteAddress = channel.remoteAddress()
            val addressEncoded = IPAddress.asLong(remoteAddress)
            info("Encoded address $addressEncoded (${remoteAddress.toString()}) for ${session.player.displayName}")
            session.intNetData = NetData(addressEncoded, port)
            session.extNetData = NetData(addressEncoded, port)
        }
        packet.pushEmptyResponse()
    }

    //endregion

    //region Util Component Region

    private fun handleUtil(packet: Packet) {
        when (packet.command) {
            Commands.FETCH_CLIENT_CONFIG -> handleFetchClientConfig(packet)
            Commands.PING -> handlePing(packet)
            Commands.PRE_AUTH -> handlePreAuth(packet)
            Commands.POST_AUTH -> handlePostAuth(packet)
            Commands.USER_SETTINGS_SAVE -> handleUserSettingsSave(packet)
            Commands.USER_SETTINGS_LOAD_ALL -> handleUserSettingsLoadAll(packet)
            Commands.SUSPEND_USER_PING -> handleSuspendUserPing(packet)
            else -> packet.pushEmptyResponse()
        }
    }

    /**
     * handleFetchClientConfig Retrieves configurations for the client from the
     * server most of this data is pre chunked or generated data
     *
     * @param packet The packet requesting a client config
     */
    private fun handleFetchClientConfig(packet: Packet) {
        val type = packet.text("CFID")
        val conf: Map<String, String>
        if (type.startsWith("ME3_LIVE_TLK_PC_")) { // Filter TLK files
            val lang = type.substring(16)
            conf = Data.loadTLK(lang) // Load the tlk file
        } else {
            // Matching different configs
            conf = when (type) {
                "ME3_DATA" -> Data.createDataConfig() // Configurations for GAW, images and others
                "ME3_MSG" -> getServerMessages() // Custom multiplayer messages
                "ME3_ENT" -> Data.createEntitlementMap() // Entitlements
                "ME3_DIME" -> Data.createDimeResponse() // Shop contents?
                "ME3_BINI_VERSION" -> mapOf(
                    "SECTION" to "BINI_PC_COMPRESSED",
                    "VERSION" to "40128"
                )
                "ME3_BINI_PC_COMPRESSED" -> Data.loadBiniCompressed() // Loads the chunked + compressed bini
                else -> emptyMap()
            }
        }
        packet.pushResponse { map("CONF", conf) }
    }

    private fun getServerMessages(): LinkedHashMap<String, String> {
        return transaction {
            val out = LinkedHashMap<String, String>()
            val messages = Message.all()
            val locales = arrayOf("de", "es", "fr", "it", "ja", "pl", "ru")
            messages.forEachIndexed { i, message ->
                val index = i + 1
                out["MSG_${index}_endDate"] = Message.DATE_FORMAT.format(LocalDate.ofEpochDay(message.endDate))
                out["MSG_${index}_image"] = message.image
                out["MSG_${index}_message"] = message.message
                locales.forEach { locale ->
                    out["MSG_${index}_message_$locale"] = message.message
                }
                out["MSG_${index}_priority"] = message.priority.toString()
                out["MSG_${index}_title"] = message.title
                locales.forEach { locale ->
                    out["MSG_${index}_title_$locale"] = message.title
                }
                out["MSG_${index}_trackingId"] = message.id.value.toString()
                out["MSG_${index}_type"] = message.type.toString()
            }
            out
        }
    }

    /**
     * handlePing Handles user ping updates. stores the current time in
     * the session. Then responds with a ping response containing the time
     *
     * @param packet The packet requesting a ping update
     */
    private fun handlePing(packet: Packet) {
        Logger.logIfDebug { "Received ping update from client: ${session.sessionId}" }
        session.lastPingTime = System.currentTimeMillis()
        packet.pushResponse { number("STIM", unixTimeSeconds()) }
    }

    /**
     * handlePreAuth Handles pre authentication sends the user
     * configurations for the other servers
     *
     * @param packet The packet requesting pre-auth information
     */
    private fun handlePreAuth(packet: Packet) {
        packet.pushResponse {
            number("ANON", 0x0)
            number("ASRC", 303107)
            list("CIDS", Data.CIDS)
            text("CNGN", "")
            +group("CONF") {
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
            +group("QOSS") {
                +group("BWPS") {

                    // was gossjcprod-qos01.ea.com
                    text("PSA", "127.0.0.1")
                    number("PSP", 17502)
                    text("SNA", "prod-sjc")
                }

                number("LNP", 0xA)
                map("LTPS", mapOf(
                    "ea-sjc" to group {
                        // was gossjcprod-qos01.ea.com
                        text("PSA", "127.0.0.1")
                        number("PSP", 17502)
                        text("SNA", "prod-sjc")
                    },
                    "rs-iad" to group {
                        // was gosiadprod-qos01.ea.com
                        text("PSA", "127.0.0.1")
                        number("PSP", 17502)
                        text("SNA", "rs-prod-iad")
                    },
                    "rs-lhr" to group {
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

    /**
     * handlePostAuth Handles post authentication sends the configuration
     * for the telemetry, player sync and ticker server informaiton
     *
     * @param packet The packet requesting post-auth information
     */
    private fun handlePostAuth(packet: Packet) {
        packet.pushResponse {
            +group("PSS") {
                text("ADRS", "playersyncservice.ea.com")
                blob("CSIG")
                text("PJID", "303107")
                number("PORT", 443)
                number("RPRT", 0xF)
                number("TIID", 0x0)
            }

            //  telemetryAddress = "reports.tools.gos.ea.com:9988"
            //  tickerAddress = "waleu2.tools.gos.ea.com:8999"
            val config = Environment.Config
            val address = config.externalAddress
            val port = config.ports.discard

            +group("TELE") {
                text("ADRS", address) // Server Address
                number("ANON", 0)
                text("DISA", Data.TELE_DISA)
                text("FILT", "-UION/****") // Telemetry filter?
                number("LOC", 1701725253)
                text("NOOK", "US,CA,MX")
                number("PORT", port)
                number("SDLY", 15000)
                text("SESS", "JMhnT9dXSED")
                text("SKEY", Data.SKEY)
                number("SPCT", 0x4B)
                text("STIM", "")
            }

            +group("TICK") {
                text("ADRS", address)
                number("port", port)
                text("SKEY", "823287263,10.23.15.2:8999,masseffect-3-pc,10,50,50,50,50,0,12")
            }

            +group("UROP") {
                number("TMOP", 0x1)
                number("UID", session.sessionId)
            }
        }
    }

    /**
     * handleUserSettingsSave Handles updating user settings from the client
     * the settings are parsed then stored in the database
     *
     * @param packet The packet requesting a setting update
     */
    private fun handleUserSettingsSave(packet: Packet) {
        val value = packet.textOrNull("DATA")
        val key = packet.textOrNull("KEY")
        if (value != null && key != null) {
            session.player.setSetting(key, value)
        }
        packet.pushEmptyResponse()
    }

    /**
     * handleUserSettingsLoadAll Handles setting all the user settings to
     * the client. Makes a database request and returns it as a map
     *
     * @param packet The packet requesting the user settings
     */
    private fun handleUserSettingsLoadAll(packet: Packet) {
        packet.pushResponse {
            map("SMAP", session.player.createSettingsMap())
        }
    }

    /**
     * handleSuspendUserPing Functionality unknown
     *
     * @param packet The packet requesting suspend user ping
     */
    private fun handleSuspendUserPing(packet: Packet) {
        when (packet.numberOrNull("TVAL")) {
            0x1312D00uL -> packet.pushEmptyError(0x12D)
            0x55D4A80uL -> packet.pushEmptyError(0x12E)
            else -> packet.pushEmptyResponse()
        }
    }

    //endregion
}
