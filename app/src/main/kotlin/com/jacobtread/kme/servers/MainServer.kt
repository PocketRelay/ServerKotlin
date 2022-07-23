package com.jacobtread.kme.servers

import com.jacobtread.kme.Environment
import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.annotations.PacketHandler
import com.jacobtread.kme.blaze.annotations.PacketProcessor
import com.jacobtread.kme.blaze.packet.Packet
import com.jacobtread.kme.blaze.tdf.GroupTdf
import com.jacobtread.kme.data.Constants
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.database.byId
import com.jacobtread.kme.database.entities.MessageEntity
import com.jacobtread.kme.database.entities.PlayerEntity
import com.jacobtread.kme.exceptions.GameException
import com.jacobtread.kme.game.GameManager
import com.jacobtread.kme.game.PlayerSession
import com.jacobtread.kme.game.PlayerSession.NetData
import com.jacobtread.kme.game.match.MatchRuleSet
import com.jacobtread.kme.game.match.Matchmaking
import com.jacobtread.kme.tools.comparePasswordHash
import com.jacobtread.kme.tools.unixTimeSeconds
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.logging.Logger.info
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.io.IOException
import java.net.InetSocketAddress

/**
 * startMainServer Starts the main server
 *
 * @param bossGroup The netty boss event loop group
 * @param workerGroup The netty worker event loop group
 */
fun startMainServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup) {

    if (Environment.mitmEnabled) { // If MITM is enabled
        startMITMServer(bossGroup, workerGroup)
        return // Don't create the normal main server
    }
    try {
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(MainInitializer())
            // Bind the server to the host and port
            .bind(Environment.mainPort)
            // Wait for the channel to bind
            .addListener { info("Started Main Server on port ${Environment.mainPort}") }
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
            .addLast(MainProcessor(session))
            .addLast(PacketEncoder)
    }
}


/**
 * MainProcessor The packet processor for the main server.
 *
 * @property session The session data for this user
 * @constructor Create empty MainClient
 */
@PacketProcessor
class MainProcessor(
    private val session: PlayerSession,
) : ChannelInboundHandlerAdapter(), PacketPushable {

    private inline fun Packet.pushResponse(init: ContentInitializer) = push(respond(init))
    private fun Packet.pushEmptyResponse() = push(respond())
    private fun Packet.pushEmptyError(error: Int) = push(error(error))

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

    private fun createEncoderContext(): String {
        val builder = StringBuilder()
        val remoteAddress = channel.remoteAddress()
        builder.append("Session: ")
            .append(session.sessionId)
            .append(" (")
            .append(remoteAddress.toString())
            .appendLine(')')
        if (session.isAuthenticated) {
            val player = session.playerEntity
            builder.append("Player: ")
                .append(player.displayName)
                .append(" (")
                .append(player.playerId)
                .appendLine(')')
        }
        return builder.toString()
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

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is Packet) return
        try { // Automatic routing to the desired function
            routeMainProcessor(this, channel, msg)
        } catch (e: NotAuthenticatedException) { // Handle player access with no player
            push(LoginError.INVALID_ACCOUNT(msg))
            val address = ctx.channel().remoteAddress()
            Logger.warn(
                "Client at {} tried to access a authenticated route without authenticating",
                address
            )
        } catch (e: Exception) {
            Logger.warn("Failed to handle packet: {}", msg, e)
            msg.pushEmptyResponse()
        } catch (e: GameException) {
            Logger.warn("Client caused game exception", e)
            msg.pushEmptyResponse()
        }
        ctx.flush()
        Packet.release(msg)
    }

    //region Authentication Component Region

    /**
     * handleGetLegalDocsInfo Retrieves info about the legal documents
     * the contents of this packet need further research and are currently
     * not documented
     *
     * @param packet The packet requesting the legal doc info
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.GET_LEGAL_DOCS_INFO)
    fun handleGetLegalDocsInfo(packet: Packet) {
        packet.pushResponse {
            number("EAMC", 0x0)
            text("LHST", "")
            number("PMC", 0x0)
            text("PPUI", "")
            text("TSUI", "")
        }
    }

    /**
     * handleTermsOfServiceContent Handles serving the contents of the
     * terms of service to the clients this is displayed if the user
     * pushes the terms of service button in the login screen
     *
     * @param packet The packet requesting the TOS contents
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.GET_TERMS_OF_SERVICE_CONTENT)
    fun handleTermsOfServiceContent(packet: Packet) {
        // Terms of service is represented as HTML this is currently a placeholder value
        // in the future Ideally this would be editable from the web control
        val content = """
            <div style="font-family: Calibri; margin: 4px;"><h1>This is a terms of service placeholder</h1></div>
        """.trimIndent()
        packet.pushResponse {
            // This is the URL of the page source this is prefixed by https://tos.ea.com/legalapp
            text("LDVC", "webterms/au/en/pc/default/09082020/02042022")
            number("TCOL", 0xdaed)
            text("TCOT", content) // The HTML contents of this legal doc
        }
    }

    /**
     * handlePrivacyPolicyContent Handles serving the contents of the
     * privacy policy to the clients this is displayed if the user
     * pushes the privacy policy button in the login screen
     *
     * @param packet The packet requesting the TOS contents
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.GET_PRIVACY_POLICY_CONTENT)
    fun handlePrivacyPolicyContent(packet: Packet) {
        // THe privacy policy is represented as HTML this is currently a placeholder value
        // in the future Ideally this would be editable from the web control
        val content = """
            <div style="font-family: Calibri; margin: 4px;"><h1>This is a privacy policy placeholder</h1></div>
        """.trimIndent()
        packet.pushResponse {
            // This is the URL of the page source this is prefixed by https://tos.ea.com/legalapp
            text("LDVC", "webprivacy/au/en/pc/default/08202020/02042022")
            number("TCOL", 0xc99c)
            text("TCOT", content) // The HTML contents of this legal doc
        }
    }

    /**
     * handlePasswordForgot This would handle email password reset emails for
     * users who have forgotten passwords but this system doesn't support that yet
     * so instead this just doesn't get handled
     *
     * @param packet The packet requesting the password reset
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.PASSWORD_FORGOT)
    fun handlePasswordForgot(packet: Packet) {
        val mail = packet.text("MAIL") // The email of the account that wants a reset
        info("Recieved password reset for $mail")
        packet.pushEmptyResponse()
    }

    /**
     * handleOriginLogin This would be the functionality for logging in Via origin
     * however this is currently not supported because I have no way of testing it,
     * so it's just going to send an empty response
     *
     * @param packet The packet requesting the origin login
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.ORIGIN_LOGIN)
    fun handleOriginLogin(packet: Packet) {
        info("Recieved unsupported request for Origin Login")
        packet.pushEmptyResponse()
    }

    /**
     * handleLogout Handles logging out a player which clears the references
     * to the player and removes them from any games they are in
     *
     * @param packet The packet requesting the logout
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.LOGOUT)
    fun handleLogout(packet: Packet) {
        val player = session.playerEntity
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
    @PacketHandler(Components.AUTHENTICATION, Commands.LIST_USER_ENTITLEMENTS_2)
    fun handleListUserEntitlements2(packet: Packet) {
        val etag = packet.text("ETAG")
        if (etag.isNotEmpty()) { // Empty responses for packets with ETAG's
            return packet.pushEmptyResponse()
        }

        // Respond with the entitlements
        packet.pushResponse { Data.createUserEntitlements(this) }
    }

    /**
     * handleGetAuthToken Returns the auth token used when making the GAW
     * authentication request. TODO: Replace this with a proper auth token
     *
     * @param packet The packet requesting the auth token
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.GET_AUTH_TOKEN)
    fun handleGetAuthToken(packet: Packet) {
        packet.pushResponse {
            text("AUTH", session.playerId.toString(16).uppercase())
        }
    }

    /**
     * handleLogin Handles email + password authentication from the login prompt
     * in the menu. Simple as that. NOTE: It would be possible to allow using a
     * username instead of an email by simply removing the email validation
     *
     * @param packet The packet requesting login
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.LOGIN)
    fun handleLogin(packet: Packet) {
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
        val playerEntity = PlayerEntity.byEmail(email) ?: return push(LoginError.EMAIL_NOT_FOUND(packet))

        // Compare the provided password with the hashed password of the player
        if (!comparePasswordHash(password, playerEntity.password)) { // If it's not the same password
            return push(LoginError.WRONG_PASSWORD(packet))
        }

        session.setAuthenticated(playerEntity) // Set the authenticated session
        push(session.authResponse(packet))
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
    @PacketHandler(Components.AUTHENTICATION, Commands.SILENT_LOGIN)
    fun handleSilentLogin(packet: Packet) {
        val pid = packet.numberInt("PID")
        val auth = packet.text("AUTH")
        // Find the player with a matching ID or send an INVALID_ACCOUNT error
        val playerEntity = PlayerEntity.byId(pid) ?: return push(LoginError.INVALID_SESSION(packet))
        // If the session token's don't match send INVALID_ACCOUNT error
        if (!playerEntity.isSessionToken(auth)) return push(LoginError.INVALID_SESSION(packet))
        val sessionToken = playerEntity.sessionToken // Session token grabbed after auth as to not generate new one
        session.setAuthenticated(playerEntity)
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
        session.pushPlayerUpdate(session)
    }

    /**
     * handleCreateAccount Handles the creation of accounts from the in game account greation tool
     * most of the data provided is discarded only the email and password are used
     *
     * @param packet The packet requesting account creation
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.CREATE_ACCOUNT)
    fun handleCreateAccount(packet: Packet) {
        val email: String = packet.text("MAIL")
        val password: String = packet.text("PASS")
        if (PlayerEntity.isEmailTaken(email)) { // Check if the email is already in use
            return push(LoginError.EMAIL_ALREADY_IN_USE(packet))
        }
        // Create a new player entity
        val playerEntity = PlayerEntity.create(email, password)
        session.setAuthenticated(playerEntity) // Link the player to this session
        push(session.authResponse(packet))
    }

    /**
     * handleLoginPersona Handles the logging in to a persona this ignores the persona name
     * field and just sends the session details of the player
     *
     * @param packet The packet requesting persona login
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.LOGIN_PERSONA)
    fun handleLoginPersona(packet: Packet) {
        packet.pushResponse { session.appendPlayerSession(this) }
        push(session.createSessionDetails()) // Send session details
        push(session.createIdentityUpdate()) // Ask for networking info
    }

    //endregion

    //region Game Manager Component Region

    /**
     * handleCreateGame Handles creating a game based on the provided attributes
     * and then tells the client the details of the created game
     *
     * @param packet The packet requesting creation of a game
     */
    @PacketHandler(Components.GAME_MANAGER, Commands.CREATE_GAME)
    fun handleCreateGame(packet: Packet) {
        val attributes = packet.mapOrNull<String, String>("ATTR") // Get the provided users attributes
        val game = GameManager.createGame(session) // Create a new game

        val hostNetworking = packet.listOrNull<GroupTdf>("HNET")
        if (hostNetworking != null) {
            val first = hostNetworking.firstOrNull()
            if (first != null) session.setNetworkingFromHNet(first)
        }

        game.setAttributes(attributes ?: emptyMap()) // If the attributes are missing use empty
        packet.pushResponse { number("GID", game.id) }

        push(game.createNotifySetup())


//        push(game.createPoolPacket(true, session)) // Send the game pool details
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
    @PacketHandler(Components.GAME_MANAGER, Commands.ADVANCE_GAME_STATE)
    fun handleAdvanceGameState(packet: Packet) {
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
    @PacketHandler(Components.GAME_MANAGER, Commands.SET_GAME_SETTINGS)
    fun handleSetGameSettings(packet: Packet) {
        val gameId = packet.number("GID")
        val setting = packet.number("GSET").toInt()
        val game = GameManager.getGameById(gameId)
        if (game != null) {
            game.gameSetting = setting
        }
        packet.pushEmptyResponse()
        push(unique(Components.GAME_MANAGER, Commands.MIGRATE_ADMIN_PLAYER) {
            number("ATTR", setting)
            number("GID", gameId)
        })
    }

    /**
     * handleSetGameAttributes Handles changing the attributes of the provided
     * game that matches GID the newly changed attributes are then broadcasted
     * to all the game players.
     *
     * @param packet The packet requesting attribute changes
     */
    @PacketHandler(Components.GAME_MANAGER, Commands.SET_GAME_ATTRIBUTES)
    fun handleSetGameAttributes(packet: Packet) {
        val gameId = packet.number("GID")
        val attributes = packet.mapOrNull<String, String>("ATTR") ?: return packet.pushEmptyResponse()
        var game = GameManager.getGameById(gameId)
        if (game != null) {
            game.setAttributes(attributes)
            game.broadcastAttributeUpdate()
        } else {
            info("Recreating game with ID $gameId")
            game = GameManager.createGameWithID(session, gameId) // Create a new game
            game.setAttributes(attributes) // If the attributes are missing use empty
            game.join(session)
        }
        packet.pushEmptyResponse()
    }

    /**
     * handleRemovePlayer Handles removing the player with the
     * provided PID from the game
     *
     * @param packet The packet requesting the player removal
     */
    @PacketHandler(Components.GAME_MANAGER, Commands.REMOVE_PLAYER)
    fun handleRemovePlayer(packet: Packet) {
        val playerId = packet.number("PID").toInt()
        val gameId = packet.number("GID")
        val game = GameManager.getGameById(gameId)
        game?.removePlayerById(playerId)
        packet.pushEmptyResponse()
    }

    /**
     * handleStartMatchmaking Handles finding a match for the current player session
     * this doesn't search based on the provided attributes isntead just finding the
     * first game with an open slot and connecting to that
     *
     * @param packet The packet requesting to start matchmaking
     */
    @PacketHandler(Components.GAME_MANAGER, Commands.START_MATCHMAKING)
    fun handleStartMatchmaking(packet: Packet) {
        val player = session.playerEntity
        info("Player ${player.displayName} started match making")
        val ruleSet = MatchRuleSet(packet)
        val game = Matchmaking.getMatchOrQueue(session, ruleSet)
        packet.pushResponse { number("MSID", session.matchmakingId) }
        if (game != null) {
            info("Found matching game for player ${player.displayName}")
            game.join(session)
        }
    }

    /**
     * handleCancelMatchmaking Handles canceling matchmaking this just removes
     * the player from any existing games and sets waiting for join to false
     *
     * @param packet The packet requesting to cancel matchmaking
     */
    @PacketHandler(Components.GAME_MANAGER, Commands.CANCEL_MATCHMAKING)
    fun handleCancelMatchmaking(packet: Packet) {
        val player = session.playerEntity
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
    @PacketHandler(Components.GAME_MANAGER, Commands.UPDATE_MESH_CONNECTION)
    fun handleUpdateMeshConnection(packet: Packet) {
        val gameId = packet.number("GID")
        packet.pushEmptyResponse()

        val game = GameManager.getGameById(gameId) ?: return
        val player = session.playerEntity
        val host = game.getHost()

        val a = unique(Components.GAME_MANAGER, Commands.NOTIFY_GAME_PLAYER_STATE_CHANGE) {
            number("GID", gameId)
            number("PID", player.playerId)
            number("STAT", 4)
        }
        val b = unique(Components.GAME_MANAGER, Commands.NOTIFY_PLAYER_JOIN_COMPLETED) {
            number("GID", gameId)
            number("PID", player.playerId)
        }

        val c = unique(Components.GAME_MANAGER, Commands.NOTIFY_ADMIN_LIST_CHANGE) {
            number("ALST", player.playerId)
            number("GID", gameId)
            number("OPER", 0) // 0 = add 1 = remove
            number("UID", host.playerId)
        }

        pushAll(a, b, c)
        host.pushAll(a, b, c)
    }

    //endregion

    //region Stats Component Region

    /**
     * getLocaleName Translates the provided locale name
     * to the user readable name
     *
     * @param code The shorthand code for the locale name
     * @return The human-readable locale name
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
    @PacketHandler(Components.STATS, Commands.GET_LEADERBOARD_GROUP)
    fun handleLeaderboardGroup(packet: Packet) {
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
    @PacketHandler(Components.STATS, Commands.GET_FILTERED_LEADERBOARD)
    fun handleFilteredLeaderboard(packet: Packet) {
        val name: String = packet.text("NAME")
        val player = session.playerEntity
        when (name) {
            "N7RatingGlobal" -> {
                val rating = player.n7Rating.toString()
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
    @PacketHandler(Components.STATS, Commands.GET_LEADERBOARD_ENTITY_COUNT)
    fun handleLeaderboardEntityCount(packet: Packet) {
        val entityCount = 1 // The number of leaderboard entities
        packet.pushResponse { number("CNT", entityCount) }
    }

    /**
     * handleCenteredLeadboard Returns a centered leaderboard TODO: NOT IMPLEMENTED
     *
     * @param packet The packet requesting a centered leaderboard
     */
    @PacketHandler(Components.STATS, Commands.GET_CENTERED_LEADERBOARD)
    fun handleCenteredLeadboard(packet: Packet) {
        // TODO: Currenlty not implemented
        packet.pushResponse {
            list("LDLS", emptyList<GroupTdf>())
        }
    }

    //endregion

    //region Messaging Component Region

    /**
     * handleFetchMessages Handles fetch messages requests from the client this
     * will send a MESSAGING SEND_MESSAGE packet to the client containing the
     * generated main menu message
     *
     * @param packet The packet requesting the messages
     */
    @PacketHandler(Components.MESSAGING, Commands.FETCH_MESSAGES)
    fun handleFetchMessages(packet: Packet) {
        packet.pushResponse { number("MCNT", 0x1) } // Number of messages
        val ip = channel.remoteAddress().toString()
        val player = session.playerEntity
        val menuMessage = Environment.menuMessage
            .replace("{v}", Constants.KME_VERSION)
            .replace("{n}", player.displayName)
            .replace("{ip}", ip) + 0xA.toChar()
        push(unique(Components.MESSAGING, Commands.SEND_MESSAGE) {
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
        })
    }


    //endregion

    //region Association Lists Component Region

    /**
     * handleAssociationListGetLists Handles getting associated lists.
     * functionality appears to be for friends lists?
     *
     * @param packet The packet requesting the list
     */
    @PacketHandler(Components.ASSOCIATION_LISTS, Commands.GET_LISTS)
    fun handleAssociationListGetLists(packet: Packet) {
        packet.pushResponse {
            list("LMAP", listOf(
                group {
                    +group("INFO") {
                        tripple("BOID", 0x19, 0x1, 0x74b09c4)
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
    @PacketHandler(Components.GAME_REPORTING, Commands.SUBMIT_OFFLINE_GAME_REPORT)
    fun handleSubmitOfflineReport(packet: Packet) {
        packet.pushEmptyResponse()
        push(unique(Components.GAME_REPORTING, Commands.GAME_REPORT_RESULT_72) {
            varList("DATA")
            number("EROR", 0)
            number("FNL", 0)
            number("GHID", 0)
            number("GRID", 0)
        })
    }

    //endregion

    //region User Sessions Component Region

    /**
     * handleResumeSession Handles resuming a previously existing client
     * session using a session token provided by the client. Checks the
     * database for any players with the provided session token and if one
     * is found that is set as the authenticated session
     *
     * @param packet The packet requesting a session resumption
     */
    @PacketHandler(Components.USER_SESSIONS, Commands.RESUME_SESSION)
    fun handleResumeSession(packet: Packet) {
        val sessionToken = packet.text("SKEY")
        val playerEntity = PlayerEntity.bySessionToken(sessionToken) ?: return push(LoginError.INVALID_INFORMATION(packet))
        session.setAuthenticated(playerEntity) // Set the authenticated session
        packet.pushEmptyResponse()
    }

    @PacketHandler(Components.USER_SESSIONS, Commands.UPDATE_HARDWARE_FLAGS)
    fun updateHardwareFlag(packet: Packet) {
        val value = packet.number("HWFG")
        session.hardwareFlag = value.toInt()
        packet.pushEmptyResponse()
        push(session.createSetSession()) // Send the user session
    }

    /**
     * updateSessionNetworkInfo Updates the session networking information
     * provided from the client
     *
     * @param packet The packet requesting the updated networking info
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    @PacketHandler(Components.USER_SESSIONS, Commands.UPDATE_NETWORK_INFO)
    fun updateSessionNetworkInfo(packet: Packet) {
        val displayName = session.playerEntity.displayName
        val addr: GroupTdf = packet.unionValue("ADDR") as GroupTdf
        val inip: GroupTdf = addr.group("INIP")
        val port: ULong = inip.number("PORT")
        val remoteAddress = channel.remoteAddress()

        require(remoteAddress is InetSocketAddress) { "Remote address was not a socket address" }
        val addressBytes = remoteAddress.address.address.toUByteArray()
        val addressEncoded = (addressBytes[0].toULong() shl 24)
            .or(addressBytes[1].toULong() shl 16)
            .or(addressBytes[2].toULong() shl 8)
            .or(addressBytes[3].toULong())

        info("Updated player network info ($remoteAddress) for $displayName")
        session.intNetData = NetData(addressEncoded, port)
        session.extNetData = NetData(addressEncoded, port)

        val nqos: GroupTdf = packet.group("NQOS")
        val dbps = nqos.number("DBPS")
        val natt = nqos.number("NATT")
        val ubps = nqos.number("UBPS")
        info("Updated player other network info ($dbps, $natt, $ubps) for $displayName")
        session.otherNetData = PlayerSession.OtherNetData(dbps, natt, ubps)

        val nlmp: Map<String, ULong> = packet.map("NLMP")
        val a = nlmp.getOrDefault("ea-sjc", 0xfff0fffu)
        val b = nlmp.getOrDefault("rs-iad", 0xfff0fffu)
        val c = nlmp.getOrDefault("rs-lhr", 0xfff0fffu)
        session.pslm = listOf(a, b, c)

        packet.pushEmptyResponse()
        push(session.createSetSession()) // Send the user session
    }

    //endregion

    //region Util Component Region

    /**
     * handleClientMetrics Handles logging of client metrics this handler
     * will always respond with empty but will log the details for the client
     * if DEBUG logging is enabled
     *
     * @param packet The packet updating client metrics
     */
    @PacketHandler(Components.UTIL, Commands.SET_CLIENT_METRICS)
    fun handleClientMetrics(packet: Packet) {
        if (Logger.debugEnabled) {
            val ubfl = packet.number("UBFL")
            val udev = packet.text("UDEV")
            val uflg = packet.number("UFLG")
            val unat = packet.number("UNAT")
            val usta = packet.number("USTA")
            val uwan = packet.number("UWAN")
            Logger.debug("Recieved client metrics")
            Logger.debug("UBFL: $ubfl")
            Logger.debug("UDEV: $udev")
            Logger.debug("UFLG: $uflg")
            Logger.debug("UNAT: $unat")
            Logger.debug("USTA: $usta")
            Logger.debug("UWAN: $uwan")
        }
        packet.pushEmptyResponse()
    }

    /**
     * handleFetchClientConfig Retrieves configurations for the client from the
     * server most of this data is pre chunked or generated data
     *
     * @param packet The packet requesting a client config
     */
    @PacketHandler(Components.UTIL, Commands.FETCH_CLIENT_CONFIG)
    fun handleFetchClientConfig(packet: Packet) {
        val type = packet.text("CFID")
        val conf: Map<String, String>
        if (type.startsWith("ME3_LIVE_TLK_PC_")) { // Filter TLK files
            val lang = type.substring(16)
            conf = try {
                Data.loadChunkedFile("data/tlk/$lang.tlk.chunked")
            } catch (e: IOException) {
                Data.loadChunkedFile("data/tlk/default.tlk.chunked")
            }
        } else {
            // Matching different configs
            conf = when (type) {
                "ME3_DATA" -> Data.createDataConfig() // Configurations for GAW, images and others
                "ME3_MSG" -> MessageEntity.createMessageMap() // Custom multiplayer messages
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

    /**
     * handlePing Handles user ping updates. stores the current time in
     * the session. Then responds with a ping response containing the time
     *
     * @param packet The packet requesting a ping update
     */
    @PacketHandler(Components.UTIL, Commands.PING)
    fun handlePing(packet: Packet) {
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
    @PacketHandler(Components.UTIL, Commands.PRE_AUTH)
    fun handlePreAuth(packet: Packet) {
        packet.pushResponse {
            number("ANON", 0x0)
            text("ASRC", "303107")
            list("CIDS", listOf(1, 25, 4, 28, 7, 9, 63490, 30720, 15, 30721, 30722, 30723, 30725, 30726, 2000))
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
                    text("PSA", "127.0.0.1")   // Server Address (formerly gossjcprod-qos01.ea.com)
                    number("PSP", 17502)  // Server Port
                    text("SNA", "prod-sjc")  // Server name?
                }

                number("LNP", 0xA)
                map("LTPS", mapOf(
                    "ea-sjc" to group {
                        text("PSA", "127.0.0.1")  // Server Address (formerly gossjcprod-qos01.ea.com)
                        number("PSP", 17502)  // Server Port
                        text("SNA", "prod-sjc") // Server name?
                    },
                    "rs-iad" to group {
                        text("PSA", "127.0.0.1") // Server Address (formerly gosiadprod-qos01.ea.com)
                        number("PSP", 17502)  // Server Port
                        text("SNA", "bio-iad-prod-shared") // Server name?
                    },
                    "rs-lhr" to group {
                        text("PSA", "127.0.0.1") // Server Address (formerly gosgvaprod-qos01.ea.com)
                        number("PSP", 17502) // Server Port
                        text("SNA", "bio-dub-prod-shared") // Server name?
                    }
                ))
                number("SVID", 0x45410805)
            }
            text("RSRC", "303107")
            text("SVER", "Blaze 3.15.08.0 (CL# 1629389)") // Server Version
        }
    }

    /**
     * handlePostAuth Handles post authentication sends the configuration
     * for the telemetry, player sync and ticker server informaiton
     *
     * @param packet The packet requesting post-auth information
     */
    @PacketHandler(Components.UTIL, Commands.POST_AUTH)
    fun handlePostAuth(packet: Packet) {
        packet.pushResponse {
            +group("PSS") { // Player Sync Service?
                text("ADRS", "playersyncservice.ea.com") // Host / Address
                blob("CSIG")
                text("PJID", "303107")
                number("PORT", 443) // Port
                number("RPRT", 0xF)
                number("TIID", 0x0)
            }

            //  telemetryAddress = "reports.tools.gos.ea.com:9988"
            //  tickerAddress = "waleu2.tools.gos.ea.com:8999"
            val address = Environment.externalAddress
            val port = Environment.discardPort

            +group("TELE") {
                text("ADRS", address) // Server Address
                number("ANON", 0)
                text("DISA", "**")
                text("FILT", "-UION/****") // Telemetry filter?
                number("LOC", 1701725253)
                text("NOOK", "US,CA,MX")
                number("PORT", port)
                number("SDLY", 15000)
                text("SESS", "JMhnT9dXSED")
                text("SKEY", "")
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
    @PacketHandler(Components.UTIL, Commands.USER_SETTINGS_SAVE)
    fun handleUserSettingsSave(packet: Packet) {
        val value = packet.textOrNull("DATA")
        val key = packet.textOrNull("KEY")
        if (value != null && key != null) {
            session.playerEntity.setSetting(key, value)
        }
        packet.pushEmptyResponse()
    }

    /**
     * handleUserSettingsLoadAll Handles setting all the user settings to
     * the client. Makes a database request and returns it as a map
     *
     * @param packet The packet requesting the user settings
     */
    @PacketHandler(Components.UTIL, Commands.USER_SETTINGS_LOAD_ALL)
    fun handleUserSettingsLoadAll(packet: Packet) {
        packet.pushResponse {
            map("SMAP", session.playerEntity.createSettingsMap())
        }
    }

    /**
     * handleSuspendUserPing Functionality unknown
     *
     * @param packet The packet requesting suspend user ping
     */
    @PacketHandler(Components.UTIL, Commands.SUSPEND_USER_PING)
    fun handleSuspendUserPing(packet: Packet) {
        when (packet.numberOrNull("TVAL")) {
            0x1312D00uL -> packet.pushEmptyError(0x12D)
            0x55D4A80uL -> packet.pushEmptyError(0x12E)
            else -> packet.pushEmptyResponse()
        }
    }

    //endregion
}
