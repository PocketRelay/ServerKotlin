package com.jacobtread.kme.servers

import com.jacobtread.kme.Config
import com.jacobtread.kme.KME_VERSION
import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.Command.*
import com.jacobtread.kme.blaze.Component.*
import com.jacobtread.kme.blaze.tdf.GroupTdf
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.data.LoginError
import com.jacobtread.kme.database.Player
import com.jacobtread.kme.exceptions.NotAuthenticatedException
import com.jacobtread.kme.game.GameManager
import com.jacobtread.kme.game.PlayerSession
import com.jacobtread.kme.game.PlayerSession.NetData
import com.jacobtread.kme.utils.IPAddress
import com.jacobtread.kme.utils.comparePasswordHash
import com.jacobtread.kme.utils.hashPassword
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.logging.Logger.info
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
import java.net.SocketException

/**
 * startMainServer Starts the main server
 *
 * @param bossGroup The boss event loop group to use
 * @param workerGroup The worker event loop group to use
 * @param config The server configuration
 */
fun startMainServer(bossGroup: NioEventLoopGroup, workerGroup: NioEventLoopGroup, config: Config) {
    try {
        val port = config.ports.main
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(MainInitializer(config))
            // Bind the server to the host and port
            .bind(port)
            // Wait for the channel to bind
            .addListener { info("Started Main Server on port $port") }
    } catch (e: IOException) {
        Logger.error("Exception in redirector server", e)
    }
}

/**
 * MainInitializer Channel Initializer for main server clients.
 * Creates sessions for the user as well as adding packet handlers
 * and the MainClient handler
 *
 * @property config
 * @constructor Create empty MainClientInitializer
 */
class MainInitializer(private val config: Config) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        val remoteAddress = ch.remoteAddress() // The remote address of the user
        val session = PlayerSession()
        info("Main started new client session with $remoteAddress given id ${session.sessionId}")
        ch.pipeline()
            // Add handler for decoding packet
            .addLast(PacketDecoder())
            // Add handler for processing packets
            .addLast(MainHandler(session, config))
            .addLast(PacketEncoder())
    }
}

/**
 * MainHandler A handler for clients connected to the main server
 *
 * @property session The session data for this user
 * @property config The server configuration
 * @constructor Create empty MainClient
 */
@Suppress("SpellCheckingInspection")
private class MainHandler(
    private val session: PlayerSession,
    private val config: Config,
) : SimpleChannelInboundHandler<Packet>() {

    /**
     * respondEmpty Used for sending a response that has no content
     *
     * @param packet The packet to respond to
     */
    fun respondEmpty(packet: Packet) = channel.respond(packet)

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
        } catch (e: NotAuthenticatedException) { // Handle player access with no player
            val address = channel.remoteAddress()
            channel.send(LoginError.INVALID_ACCOUNT(msg))
            Logger.warn("Client at $address tried to access a authenticated route without authenticating")
        } catch (e: Exception) {
            Logger.warn("Failed to handle packet: $msg", e)
            respondEmpty(msg)
        }
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
            LIST_USER_ENTITLEMENTS_2 -> handleListUserEntitlements2(packet)
            GET_AUTH_TOKEN -> handleGetAuthToken(packet)
            LOGIN -> handleLogin(packet)
            SILENT_LOGIN -> handleSilentLogin(packet)
            LOGIN_PERSONA -> handleLoginPersona(packet)
            ORIGIN_LOGIN -> handleOriginLogin(packet)
            CREATE_ACCOUNT -> handleCreateAccount(packet)
            LOGOUT -> handleLogout(packet)
            else -> respondEmpty(packet)
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
        respondEmpty(packet)
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
        respondEmpty(packet)
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
            respondEmpty(packet)
            return
        }

        // Respond with the already generated entitlements
        // (This is a lazy value that's the same for everyone)
        channel.respond(packet, Data.USER_ENTITLEMENTS)

        // If the player hasn't yet recieved their session info send them it
        if (!session.sendSession) {
            session.sendSession = true
            // Send a set session packet for the current player
            channel.send(session.createSetSession())
        }
    }

    /**
     * handleGetAuthToken Returns the auth token used when making the GAW
     * authentication request. TODO: Replace this with a proper auth token
     *
     * @param packet The packet requesting the auth token
     */
    private fun handleGetAuthToken(packet: Packet) {
        channel.respond(packet) {
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
    private fun handleLogin(packet: Packet) {
        val email: String = packet.text("MAIL")
        val password: String = packet.text("PASS")
        if (email.isBlank() || password.isBlank()) { // If we are missing email or password
            return channel.send(LoginError.INVALID_INFORMATION(packet))
        }

        // Regex for matching emails
        val emailRegex = Regex("^[\\p{L}\\p{N}._%+-]+@[\\p{L}\\p{N}.\\-]+\\.\\p{L}{2,}$")
        if (!email.matches(emailRegex)) { // If the email is not a valid email
            return channel.send(LoginError.INVALID_EMAIL(packet))
        }

        // Retrieve the player with this email or send an email not found error
        val player = Player.getByEmail(email) ?: return channel.send(LoginError.EMAIL_NOT_FOUND(packet))

        // Compare the provided password with the hashed password of the player
        if (!comparePasswordHash(password, player.password)) { // If it's not the same password
            return channel.send(LoginError.WRONG_PASSWORD(packet))
        }

        session.setAuthenticated(player) // Set the authenticated session
        // Send the authenticated response with a persona list
        channel.respond(packet) {
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
        val pid: Long = packet.number("PID")
        val auth: String = packet.text("AUTH")
        // Find the player with a matching ID or send an INVALID_ACCOUNT error
        val player = Player.getById(pid) ?: return channel.send(LoginError.INVALID_ACCOUNT(packet))
        // If the session token's don't match send INVALID_ACCOUNT error
        if (!player.isSessionToken(auth)) return channel.send(LoginError.INVALID_ACCOUNT(packet))
        val sessionToken = player.sessionToken // Session token grabbed after auth as to not generate new one
        session.setAuthenticated(player)
        // We don't store last login time so this is just computed here
        channel.respond(packet) {
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
        channel.send(session.createSessionDetails())
        channel.send(session.createIdentityUpdate())
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
            channel.send(LoginError.EMAIL_ALREADY_IN_USE(packet)) // Send email in use error
            return
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
        respondEmpty(packet)
    }

    /**
     * handleLoginPersona Handles the logging in to a persona this ignores the persona name
     * field and just sends the session details of the player
     *
     * @param packet The packet requesting persona login
     */
    private fun handleLoginPersona(packet: Packet) {
        channel.respond(packet) { session.appendPlayerSession(this) }
        channel.send(session.createSessionDetails())
        channel.send(session.createIdentityUpdate())
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

    /**
     * handleCreateGame Handles creating a game based on the provided attributes
     * and then tells the client the details of the created game
     *
     * @param packet The packet requesting creation of a game
     */
    private fun handleCreateGame(packet: Packet) {
        val attributes = packet.mapKVOrNull("ATTR") // Get the provided users attributes
        val game = GameManager.createGame(session) // Create a new game
        game.attributes.setValues(attributes ?: emptyMap()) // If the attributes are missing use empty
        channel.respond(packet) { number("GID", game.id) } // Respond with the game ID
        channel.send(game.createPoolPacket(true)) // Send the game pool details
        channel.send(session.createSetSession()) // Send the user session
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
        respondEmpty(packet)
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
        respondEmpty(packet)
        channel.unique(
            GAME_MANAGER,
            MIGRATE_ADMIN_PLAYER,
        ) {
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
        val attributes = packet.mapKVOrNull("ATTR")
        if (attributes == null) {
            respondEmpty(packet)
            return
        }
        val game = GameManager.getGameById(gameId)
        if (game != null) {
            game.attributes.setValues(attributes)
            game.broadcastAttributeUpdate()
        }
        respondEmpty(packet)
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
        respondEmpty(packet)
    }

    /**
     * handleStartMatchmaking Handles finding a match for the current player session
     * this doesn't search based on the provided attributes isntead just finding the
     * first game with an open slot and connecting to that
     *
     * @param packet The packet requesting to start matchmaking
     */
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

        val creator = game.host.createSessionDetails()
        channel.send(creator)
        game.getActivePlayers().forEach {
            val sessionDetails = it.createSessionDetails()
            channel.send(sessionDetails)
        }
        channel.send(game.createPoolPacket(false))
    }

    /**
     * handleCancelMatchmaking Handles canceling matchmaking this just removes
     * the player from any existing games and sets waiting for join to false
     *
     * @param packet The packet requesting to cancel matchmaking
     */
    private fun handleCancelMatchmaking(packet: Packet) {
        session.waitingForJoin = false
        session.leaveGame()
        respondEmpty(packet)
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
        if (game != null && session.waitingForJoin) {
            val player = session.player
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

            host.send(a, b, c)
        } else {
            respondEmpty(packet)
        }
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
            GET_LEADERBOARD_GROUP -> handleLeaderboardGroup(packet)
            GET_FILTERED_LEADERBOARD -> handleFilteredLeaderboard(packet)
            GET_LEADERBOARD_ENTITY_COUNT -> handleLeaderboardEntityCount(packet)
            GET_CENTERED_LEADERBOARD -> handleCenteredLeadboard(packet)
            else -> respondEmpty(packet)
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
            channel.respond(packet) {
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
            respondEmpty(packet)
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
                channel.respond(packet) {
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
                channel.respond(packet) {
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
            else -> respondEmpty(packet)
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
        transaction {
            val playerCount = Player.count()
            channel.respond(packet) {
                number("CNT", playerCount)
            }
        }
    }

    /**
     * handleCenteredLeadboard Returns a centered leaderboard TODO: NOT IMPLEMENTED
     *
     * @param packet The packet requesting a centered leaderboard
     */
    private fun handleCenteredLeadboard(packet: Packet) {
        // TODO: Currenlty not implemented
        channel.respond(packet) {
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
            FETCH_MESSAGES -> handleFetchMessages(packet)
            else -> respondEmpty(packet)
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
        channel.respond(packet, flush = false) { number("MCNT", 0x1) }
        val ip = channel.remoteAddress().toString()
        val player = session.player
        val menuMessage = config.menuMessage
            .replace("{v}", KME_VERSION)
            .replace("{n}", player.displayName)
            .replace("{ip}", ip) + 0xA.toChar()
        channel.unique(
            MESSAGING,
            SEND_MESSAGE,
        ) {
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
            GET_LISTS -> handleAssociationListGetLists(packet)
            else -> respondEmpty(packet)
        }
    }

    /**
     * handleAssociationListGetLists Handles getting associated lists.
     * functionality appears to be for friends lists?
     *
     * @param packet The packet requesting the list
     */
    private fun handleAssociationListGetLists(packet: Packet) {
        channel.respond(packet) {
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
        when (packet.command) {
            SUBMIT_OFFLINE_GAME_REPORT -> handleOfflineGameReport(packet)
            else -> respondEmpty(packet)
        }
    }

    /**
     * handleOfflineGameReport Handles offline report submission
     *
     * @param packet The packet requesting submission of offline game report
     */
    private fun handleOfflineGameReport(packet: Packet) {
        respondEmpty(packet)
        channel.unique(GAME_REPORTING, GAME_REPORT_RESULT_72) {
            varList("DATA")
            number("EROR", 0)
            number("FNL", 0)
            number("GHID", 0)
            number("GRID", 0)
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
            UPDATE_HARDWARE_FLAGS,
            UPDATE_NETWORK_INFO,
            -> updateSessionNetworkInfo(packet)
            else -> respondEmpty(packet)
        }
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
            val port: Int = inip.numberInt("PORT")
            val remoteAddress = channel.remoteAddress()
            val addressEncoded = IPAddress.asLong(remoteAddress)
            session.netData = NetData(addressEncoded, port)
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
                "ME3_DATA" -> Data.createDataConfig(config) // Configurations for GAW, images and others
                "ME3_MSG" -> Data.createServerMessage() // Custom multiplayer messages
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
        channel.respond(packet) { map("CONF", conf) } // Respond with the config
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
        channel.respond(packet) {
            number("STIM", unixTimeSeconds())
        }
    }

    /**
     * handlePreAuth Handles pre authentication sends the user
     * configurations for the other servers
     *
     * @param packet The packet requesting pre-auth information
     */
    private fun handlePreAuth(packet: Packet) {
        channel.respond(packet) {
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
        channel.respond(packet) {
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

            +group("TELE") {
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

            +group("TICK") {
                text("ADRS", config.address)
                number("port", config.ports.ticker)
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
        respondEmpty(packet)
    }

    /**
     * handleUserSettingsLoadAll Handles setting all the user settings to
     * the client. Makes a database request and returns it as a map
     *
     * @param packet The packet requesting the user settings
     */
    private fun handleUserSettingsLoadAll(packet: Packet) {
        channel.respond(packet) {
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
            0x1312D00L -> channel.error(packet, 0x12D)
            0x55D4A80L -> channel.error(packet, 0x12E)
            else -> respondEmpty(packet)
        }
    }

    //endregion
}