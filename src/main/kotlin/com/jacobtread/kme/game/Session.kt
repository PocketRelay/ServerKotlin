package com.jacobtread.kme.game

import com.jacobtread.blaze.*
import com.jacobtread.blaze.annotations.PacketHandler
import com.jacobtread.blaze.annotations.PacketProcessor
import com.jacobtread.blaze.data.VarTriple
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.tdf.types.GroupTdf
import com.jacobtread.blaze.tdf.types.OptionalTdf
import com.jacobtread.kme.Environment
import com.jacobtread.kme.data.Constants
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.data.LoginError
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.database.data.Player
import com.jacobtread.kme.exceptions.DatabaseException
import com.jacobtread.kme.exceptions.GameException
import com.jacobtread.kme.game.match.MatchRuleSet
import com.jacobtread.kme.game.match.Matchmaking
import com.jacobtread.kme.utils.hashPassword
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.unixTimeSeconds
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Represents an individual clients session connected to the main
 * server. This stores information about the current session which
 * includes client information along with authentication infromation
 *
 * This also has a [PacketProcessor] annotation and is a [ChannelInboundHandlerAdapter]
 * so that it can handle routing and provide routing to the handler functions
 * present.
 *
 * Each session is uniquely identified at runtime using its [sessionId].
 *
 * In order to prevent memory leaks any references to this object must be
 * removed when [dispose] is called.
 *
 * @constructor Creates a new session linked to the provided channel
 *
 * @param channel The underlying channel this session is for
 */
@PacketProcessor
class Session(channel: Channel) : PacketPushable, ChannelInboundHandlerAdapter() {

    /**
     * The unique identifier for this session. Retrieves from the
     * atomic integer value and increases it for the next session
     */
    val sessionId = nextSessionId.getAndIncrement()

    /**
     * The socket channel that this session belongs to
     */
    private val socketChannel: Channel = channel

    /**
     * Encoded external ip address. This is the ip address which is
     * used when connecting from the outer world.
     */
    private var externalAddress: ULong = 0uL

    /**
     * This is the port used when other clients connect to this
     * client from the outer world
     */
    private var externalPort: ULong = 0uL

    /**
     * Encoded internal ip address. This is the ip address which is
     * used when connecting from the same network.
     */
    private var internalAddress: ULong = 0uL

    /**
     * This is the port used when other clients connect to this
     * client from the same network
     */
    private var internalPort: ULong = 0uL

    /**
     * If the internal and external networking information was updated using
     * a [Components.USER_SESSIONS] / [Commands.UPDATE_NETWORK_INFO] packet
     * then this value will be false otherwise it's true. Determines whether
     * [createNetworkingTdf] will return an empty optional or one with a value
     */
    private var isNetworkingUnset: Boolean = true

    /**
     * Usage unknown further investigation needed.
     */
    var dbps: ULong = 0uL
        private set

    /**
     *  The type of Network Address Translation that needs to be used for the client
     *  that this session represents.  0 (Unknown?), 1 (Unknown?), 4 (Appears to be PAT "Port Address Translation")
     */
    var nattType: ULong = 0uL
        private set

    /**
     * Usage unknown further investigation needed.
     */
    var ubps: ULong = 0uL
        private set

    /**
     * Usage unknown further investigation needed.
     */
    private var hardwareFlag: Int = 0

    /**
     * Usage unknown further investigation needed.
     *
     * Possibly the clients' connectivity to the
     * different player sync services?
     */
    private var pslm: ArrayList<ULong> = arrayListOf(0xfff0fffu, 0xfff0fffu, 0xfff0fffu)


    private var location: ULong = 0x64654445uL

    /**
     * The unix timestamp in milliseconds of when the last ping packet was
     * recieved from the client. -1 until the first ping is recieved.
     *
     * TODO: Implement timeout using this
     */
    private var lastPingTime: Long = -1L

    /**
     * References the current game that this session is a part of.
     * Null if the session is not in a game.
     */
    private var game: Game? = null

    /**
     * The slot index that this session is placed at in the current game.
     * Slot 0 is the host slot. This is zero until a game is joined where
     * it is then set to the proper value.
     */
    var gameSlot: Int = 0

    /**
     * Field for safely accessing the ID of the current game.
     * In cases where the game is null this is just 1
     */
    private val gameIdSafe: ULong get() = game?.id ?: 1uL

    /**
     * This variable states whether this session is stored in
     * the matchmaking queue. This helps keep track of the
     * matchmaking state
     */
    private var matchmaking: Boolean = false

    /**
     * This is a unique identifier given to each session that joins the
     * matchmaking queue.
     */
    var matchmakingId: ULong = 1uL

    /**
     * The unix timestamp in miliseconds from when this session entered
     * the matchmaking queue. Used to calcualte whether a session should
     * time out from matchmaking
     */
    var startedMatchmaking: Long = 1L
        private set

    /**
     * References the player entity that this session is currently
     * authenticated as.
     */
    var player: Player? = null

    /**
     * Safe way of retrieving the player ID in the cases where
     * the player could be null 1 is returned instead
     */
    val playerIdSafe: Int get() = player?.playerId ?: 1

    init {
        updateEncoderContext() // Set the initial encoder context
    }

    /**
     * Updates the session matchmaking state and sets
     * the started matchmaking time to the current time
     */
    fun startMatchmaking() {
        matchmaking = true
        startedMatchmaking = System.currentTimeMillis()
    }

    /**
     * Clears the session matchmaking state and resets
     * the matchmaking start time to -1
     *
     */
    fun resetMatchmakingState() {
        matchmaking = false
        startedMatchmaking = -1L
    }

    /**
     * Sets the currently connected game as well as the current
     * game slot. This removes the session from any existing games
     *
     * @param game The game this session is apart of
     * @param gameSlot The slot in the game this session occupies
     */
    fun setGame(game: Game, gameSlot: Int) {
        removeFromGame()
        this.game = game
        this.gameSlot = gameSlot
    }

    /**
     * Clears the currently connect game reference. This is
     * called by the game itself once the player has been
     * removed from the game. This also sets the slot back
     * to zero
     */
    fun clearGame() {
        game = null
        gameSlot = 0
    }


    /**
     * Updates the encoder context string that is stored as
     * a channel attribute. This context string provides
     * additional information about channel networking and
     * is useful when debugging to see who sent which packets
     * and who recieved what
     */
    private fun updateEncoderContext() {
        val builder = StringBuilder()
        val remoteAddress = socketChannel.remoteAddress()

        builder.append("Session: (ID: ")
            .append(sessionId)
            .append(", ADDRESS: ")
            .append(remoteAddress)
            .appendLine(')')

        val playerEntity = player
        if (playerEntity != null) {
            builder.append("Player: (NAME: ")
                .append(playerEntity.displayName)
                .append(", ID: ")
                .append(playerEntity.playerId)
                .appendLine(')')
        }

        // Update encoder context value
        socketChannel
            .attr(PacketEncoder.ENCODER_CONTEXT_KEY)
            .set(builder.toString())
    }

    /**
     * Handles pushing packets to be written to the socket
     * connection. Checks if the push request was in the
     * event loop and then writes and flushes the packet
     * otherwise it tells the event loop to execute the
     * same thing.
     *
     * @param packet The packet to write to the socket
     */
    override fun push(packet: Packet) {
        val eventLoop = socketChannel.eventLoop()
        if (eventLoop.inEventLoop()) { // If the push was made inside the event loop
            // Write the packet and flush
            socketChannel.write(packet)
            socketChannel.flush()
        } else { // If the push was made outside the event loop
            eventLoop.execute { // Execute write and flush on event loop
                socketChannel.write(packet)
                socketChannel.flush()
            }
        }
    }

    /**
     * Handles pushing multiple packets to be written to the socket
     * connection. Checks to see if the push request was in the event
     * loop and then writes all then packets before flushing. If its
     * called outside the event loop it tells the event loop to execute
     * the same instruction
     *
     * @param packets The packets to write to the socket
     */
    override fun pushAll(vararg packets: Packet) {
        val eventLoop = socketChannel.eventLoop()
        if (eventLoop.inEventLoop()) { // If the push was made inside the event loop
            // Write the packets and flush
            packets.forEach { socketChannel.write(it) }
            socketChannel.flush()
        } else { // If the push was made outside the event loop
            eventLoop.execute { // Execute write and flush on event loop
                packets.forEach { socketChannel.write(it) }
                socketChannel.flush()
            }
        }
    }

    /**
     * Sets the currently authenticated player entity. If there
     * is already an authenticated player that player is removed
     * from any games before setting the new player.
     *
     * Null can be provided to clear the authenticated player
     *
     * Calling this updates the encoder context.
     *
     * @param player The new authenticated player or null to logout
     */
    private fun setAuthenticatedPlayer(player: Player?) {
        val existing = this.player
        if (existing != player) {
            removeFromGame()
        }
        this.player = player
        // Update the encoder context because player has changed
        updateEncoderContext()
    }

    /**
     * Handles dealing with messages that have been read from the pipeline.
     * In this case it handles the packets that are recieved and passes them
     * onto the generated routing function.
     *
     * @param ctx The channel context
     * @param msg The recieved message
     */
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is Packet) return
        try { // Automatic routing to the desired function
            routeSession(this, socketChannel, msg)
        } catch (e: NotAuthenticatedException) { // Handle player access with no player
            push(LoginError.INVALID_ACCOUNT(msg))
            val address = ctx.channel().remoteAddress()
            Logger.warn("Client at $address tried to access a authenticated route without authenticating")
        } catch (e: Exception) {
            Logger.warn("Failed to handle packet: $msg", e)
            push(msg.respond())
        } catch (e: GameException) {
            Logger.warn("Client caused game exception", e)
            push(msg.respond())
        }
        ctx.flush()
        Packet.release(msg)
    }


    // region Packet Handlers

    // region Authentication Handlers

    /**
     * Functionality unknown
     *
     * Needs further investigation
     *
     * @param packet
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.GET_LEGAL_DOCS_INFO)
    fun handleGetLegalDocsInfo(packet: Packet) {
        push(packet.respond {
            number("EAMC", 0x0)
            text("LHST", "")
            number("PMC", 0x0)
            text("PPUI", "")
            text("TSUI", "")
        })
    }

    /**
     * Handles serving the terms of service content for displaying on the account login / creation
     * screen. The terms of service are rendered as an HTML markup so HTML tags can be used in this
     *
     * @param packet The packet requesting the terms of service content
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.GET_TERMS_OF_SERVICE_CONTENT)
    fun handleTermsOfServiceContent(packet: Packet) {
        // Terms of service is represented as HTML this is currently a placeholder value
        // in the future Ideally this would be editable from the web control
        val content = """
            <div style="font-family: Calibri; margin: 4px;"><h1>This is a terms of service placeholder</h1></div>
        """.trimIndent()
        push(packet.respond {
            // This is the URL of the page source this is prefixed by https://tos.ea.com/legalapp
            text("LDVC", "webterms/au/en/pc/default/09082020/02042022")
            number("TCOL", 0xdaed)
            text("TCOT", content) // The HTML contents of this legal doc
        })
    }

    /**
     * Handles serving the privacy policy content for displaying on
     * the account login / creation screen. The privacy policy is
     * rendered as an HTML markup so HTML tags can be used in this
     *
     * The current response is just a placeholder
     *
     * @param packet The packet requesting the privacy policy content
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.GET_PRIVACY_POLICY_CONTENT)
    fun handlePrivacyPolicyContent(packet: Packet) {
        // THe privacy policy is represented as HTML this is currently a placeholder value
        // in the future Ideally this would be editable from the web control
        val content = """
            <div style="font-family: Calibri; margin: 4px;"><h1>This is a privacy policy placeholder</h1></div>
        """.trimIndent()
        push(packet.respond {
            // This is the URL of the page source this is prefixed by https://tos.ea.com/legalapp
            text("LDVC", "webprivacy/au/en/pc/default/08202020/02042022")
            number("TCOL", 0xc99c)
            text("TCOT", content) // The HTML contents of this legal doc
        })
    }

    /**
     * In the real EA implementation this handles sending out
     * password reset emails. This could be implemented in the
     * future but at the moment it only prints the email to the
     * output
     *
     * @param packet The packet requesting a forgot password email
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.PASSWORD_FORGOT)
    fun handlePasswordForgot(packet: Packet) {
        val mail = packet.text("MAIL") // The email of the account that wants a reset
        Logger.info("Recieved password reset for $mail")
        push(packet.respond())
    }


    /**
     * Handles logins from clients using the origin system. This generates a
     * unqiue username for the provided origin token.
     *
     * @param packet The packet requesting origin login
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.ORIGIN_LOGIN)
    fun handleOriginLogin(packet: Packet) {
        val auth = packet.text("AUTH")
        val player = Environment.database.getOriginPlayer(auth)
        Logger.info("Authenticated Origin Account ${player.displayName}")

        setAuthenticatedPlayer(player)
        push(createSilentAuthenticatedResponse(packet))
        updateSessionFor(this)
    }

    /**
     * Handles logging out the currently
     * authenticated player
     *
     * @param packet The packet requesting logout
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.LOGOUT)
    fun handleLogout(packet: Packet) {
        push(packet.respond())
        val playerEntity = player ?: return
        Logger.info("Logged out player ${playerEntity.displayName}")
        setAuthenticatedPlayer(null)
    }


    /**
     * Handles retrieving the user entitlements list for
     * the authenticated player
     *
     * @param packet The packet requesting user entitlements
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.LIST_USER_ENTITLEMENTS_2)
    fun handleListUserEntitlements2(packet: Packet) {
        val etag = packet.text("ETAG")
        if (etag.isNotEmpty()) { // Empty responses for packets with ETAG's
            return push(packet.respond())
        }

        // Respond with the entitlements
        push(packet.respond { Data.createUserEntitlements(this) })
    }

    /**
     * Handles getting an authentication token for the
     * Galaxy at war http server. This is currently just
     * using the player id hex value instead of an actual
     * token
     *
     * @param packet The packet requesting the auth token
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.GET_AUTH_TOKEN)
    fun handleGetAuthToken(packet: Packet) {
        val playerEntity = player ?: throw NotAuthenticatedException()
        push(packet.respond {
            text("AUTH", playerEntity.playerId.toString(16).uppercase())
        })
    }

    /**
     * Handles logging into an existing player account using
     * the email and password.
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

        try {
            val database = Environment.database

            // Retrieve the player with this email or send an email not found error
            val player = database.getPlayerByEmail(email) ?: return push(LoginError.EMAIL_NOT_FOUND(packet))

            // Compare the provided password with the hashed password of the player
            if (!player.isMatchingPassword(password)) { // If it's not the same password
                return push(LoginError.WRONG_PASSWORD(packet))
            }

            setAuthenticatedPlayer(player) // Set the authenticated session
            push(createAuthenticatedResponse(packet))
        } catch (e: DatabaseException) {
            Logger.warn("Failed to login player", e)
            push(LoginError.SERVER_UNAVAILABLE(packet))
        }
    }

    /**
     * Handles silent behind the scenes token authentication for clients which
     * have already previously entered their credentials.
     *
     * @param packet The packet requesting silent login
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.SILENT_LOGIN)
    fun handleSilentLogin(packet: Packet) {
        val pid = packet.numberInt("PID")
        val auth = packet.text("AUTH")
        try {
            val database = Environment.database
            // Find the player with a matching ID or send an INVALID_ACCOUNT error
            val player = database.getPlayerById(pid) ?: return push(LoginError.INVALID_ACCOUNT(packet))
            // If the session token's don't match send INVALID_ACCOUNT error
            if (!player.isSessionToken(auth)) return push(LoginError.INVALID_SESSION(packet))
            setAuthenticatedPlayer(player)
            push(createSilentAuthenticatedResponse(packet))
            updateSessionFor(this)
        } catch (e: IOException) {
            Logger.warn("Failed silent login", e)
            push(LoginError.SERVER_UNAVAILABLE(packet))
        }
    }


    /**
     * Handles the creation of a new account using the email
     * and password provided by the client
     *
     * @param packet The packet requesting account creation
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.CREATE_ACCOUNT)
    fun handleCreateAccount(packet: Packet) {
        val email: String = packet.text("MAIL")
        val password: String = packet.text("PASS")
        try {
            val database = Environment.database
            if (database.isEmailTaken(email)) {
                push(LoginError.EMAIL_ALREADY_IN_USE(packet))
                return
            }

            val hashedPassword = hashPassword(password)
            val player = database.createPlayer(email, hashedPassword)
            setAuthenticatedPlayer(player) // Link the player to this session
            push(createAuthenticatedResponse(packet))
        } catch (e: DatabaseException) {
            Logger.warn("Failed to create account", e)
            push(LoginError.SERVER_UNAVAILABLE(packet))
        }
    }

    /**
     * Handles logging into personas. On the official EA servers this
     * would actually be handling with persona data but here it just
     * uses the player data instead
     *
     * @param packet The packet rquesting the login of a persona
     */
    @PacketHandler(Components.AUTHENTICATION, Commands.LOGIN_PERSONA)
    fun handleLoginPersona(packet: Packet) {
        push(packet.respond { appendDetailsTo(this) })
        updateSessionFor(this)
    }

    // endregion

    // region Game Manager Handlers

    /**
     * Handles the creation of a new game using the
     * attributes provided by the client
     *
     * @param packet The packet creating the game
     */
    @PacketHandler(Components.GAME_MANAGER, Commands.CREATE_GAME)
    fun handleCreateGame(packet: Packet) {
        val attributes = packet.mapOrNull<String, String>("ATTR") // Get the provided users attributes
        val game = GameManager.createGame(this) // Create a new game
        val hostNetworking = packet.listOrNull<GroupTdf>("HNET")
        if (hostNetworking != null) {
            val first = hostNetworking.firstOrNull()
            if (first != null) setNetworkingFromGroup(first)
        }

        game.setAttributes(attributes ?: emptyMap(), false) // If the attributes are missing use empty

        push(packet.respond { number("GID", game.id) }) // Send the user session

        game.setupHost()
        Matchmaking.onGameCreated(game)
    }

    /**
     * Handles updating the state of a game. States are not
     * currently documented at this point. Further investation
     * needs to happen to understand what each id means
     *
     * @param packet The packet updating the game state
     */
    @PacketHandler(Components.GAME_MANAGER, Commands.ADVANCE_GAME_STATE)
    fun handleAdvanceGameState(packet: Packet) {
        val gameId = packet.number("GID")
        val gameState = packet.number("GSTA").toInt()
        val game = GameManager.getGameById(gameId)
        if (game != null) {
            game.gameState = gameState
        }
        push(packet.respond())
    }

    /**
     * Handles updating the game setting.
     *
     * Needs further investigation for proper documentation
     *
     * @param packet
     */
    @PacketHandler(Components.GAME_MANAGER, Commands.SET_GAME_SETTINGS)
    fun handleSetGameSettings(packet: Packet) {
        val gameId = packet.number("GID")
        val setting = packet.number("GSET").toInt()
        val game = GameManager.getGameById(gameId)
        if (game != null) {
            game.gameSetting = setting
        }
        pushAll(
            packet.respond(),
            notify(Components.GAME_MANAGER, Commands.NOTIFY_GAME_SETTINGS_CHANGE) {
                number("ATTR", setting)
                number("GID", gameId)
            }
        )
    }

    /**
     * Handles updating a games attributes based on the newly
     * provided attributes from the client. This packet is
     * recieved when things like the enemy type or map change
     *
     * @param packet The packet that is setting the game attributes
     */
    @PacketHandler(Components.GAME_MANAGER, Commands.SET_GAME_ATTRIBUTES)
    fun handleSetGameAttributes(packet: Packet) {
        val gameId = packet.number("GID")
        val attributes = packet.mapOrNull<String, String>("ATTR")
        if (attributes != null) {
            val game = GameManager.getGameById(gameId)
            game?.setAttributes(attributes, true)
        }
        push(packet.respond())
    }

    /**
     * Handles removing a player from a game based on that
     * player's ID and the game ID
     *
     * @param packet The packet requesting the player ID
     */
    @PacketHandler(Components.GAME_MANAGER, Commands.REMOVE_PLAYER)
    fun handleRemovePlayer(packet: Packet) {
        val playerId = packet.number("PID").toInt()
        val gameId = packet.number("GID")
        val game = GameManager.getGameById(gameId)
        game?.removePlayerById(playerId)
        push(packet.respond())
    }

    /**
     * Handles matchmaking for players. Currently, this implementation
     * works if a game already exists but the queue system currently
     * does not work properly
     *
     * @param packet The packet requesting the matchmaking start
     */
    @PacketHandler(Components.GAME_MANAGER, Commands.START_MATCHMAKING)
    fun handleStartMatchmaking(packet: Packet) {
        val playerEntity = player ?: throw NotAuthenticatedException()
        Logger.info("Player ${playerEntity.displayName} started match making")
        val ruleSet = MatchRuleSet(packet)
        val game = Matchmaking.getMatchOrQueue(this, ruleSet)
        push(packet.respond { number("MSID", matchmakingId) })
        if (game != null) {
            Logger.info("Found matching game for player ${playerEntity.displayName}")
            game.join(this)
        }
    }

    /**
     * Handles the player cancelling a matchmaking request. Removes the player
     * from the matchmaking queue along with any games they managed to get into
     * in the process.
     *
     * @param packet The packet requesting the matchmaking cancel
     */
    @PacketHandler(Components.GAME_MANAGER, Commands.CANCEL_MATCHMAKING)
    fun handleCancelMatchmaking(packet: Packet) {
        val playerEntity = player ?: throw NotAuthenticatedException()
        Logger.info("Player ${playerEntity.displayName} cancelled match making")
        Matchmaking.removeFromQueue(this)
        removeFromGame()
        push(packet.respond())
    }


    /**
     * Handles updating a connection state across the mesh network between players
     * and hosts of a game.
     *
     * @param packet The packet requesting the mesh update
     */
    @PacketHandler(Components.GAME_MANAGER, Commands.UPDATE_MESH_CONNECTION)
    fun handleUpdateMeshConnection(packet: Packet) {
        val gameId = packet.number("GID")
        push(packet.respond())
        val game = GameManager.getGameById(gameId) ?: return
        game.updateMeshConnection(this)
    }

    // endregion

    // region Stats Handlers

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
     * Handle leaderboard group
     *
     * TODO: NOT IMPLEMENTED PROPERLY
     *
     * @param packet
     */
    @PacketHandler(Components.STATS, Commands.GET_LEADERBOARD_GROUP)
    fun handleLeaderboardGroup(packet: Packet) {
        val name: String = packet.text("NAME")
        val isN7 = name.startsWith("N7Rating")
        if (isN7 || name.startsWith("ChallengePoints")) {
            val locale: String = name.substring(if (isN7) 8 else 15)
            val localeName = getLocaleName(locale)
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
            push(packet.respond {
                number("ACSD", 0x0)
                text("BNAM", name)
                text("DESC", desc)
                pair("ETYP", 0x7802, 0x1)
                map("KSUM", mapOf(
                    "accountcountry" to group {
                        map("KSVL", mapOf(0x0 to 0x0))
                    }
                ))
                number("LBSZ", 0x7270e0)
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
            })
        } else {
            push(packet.respond())
        }
    }

    /**
     * Handles a filtered leaderboard
     *
     * TODO: Currently not implemented
     *
     * @param packet
     */
    @PacketHandler(Components.STATS, Commands.GET_FILTERED_LEADERBOARD)
    fun handleFilteredLeaderboard(packet: Packet) {
        push(packet.respond {
            list("LDLS", emptyList<GroupTdf>())
        })
    }

    /**
     * Handles retrieving the number of entities on the leaderboard
     *
     * TODO: Currently not implemented
     *
     * @param packet
     */
    @PacketHandler(Components.STATS, Commands.GET_LEADERBOARD_ENTITY_COUNT)
    fun handleLeaderboardEntityCount(packet: Packet) {
        val entityCount = 1 // The number of leaderboard entities
        push(packet.respond { number("CNT", entityCount) })
    }

    /**
     * Handles retrieving the contents of the centered leaderboard
     *
     * TODO: Currently not implemented
     *
     * @param packet
     */
    @PacketHandler(Components.STATS, Commands.GET_CENTERED_LEADERBOARD)
    fun handleCenteredLeadboard(packet: Packet) {
        // TODO: Currenlty not implemented
        push(packet.respond {
            list("LDLS", emptyList<GroupTdf>())
        })
    }

    // endregion

    // region Messaging Handlers

    /**
     * Handles sending messages to the client when the client
     * requests them. Currently, this only includes the main menu
     * message. But once further investigation is complete I hope
     * to have this include all messaging types.
     *
     * TODO: Investigate sending of other message types
     *
     * @param packet The packet requesting messages
     */
    @PacketHandler(Components.MESSAGING, Commands.FETCH_MESSAGES)
    fun handleFetchMessages(packet: Packet) {
        val playerEntity = player

        if (playerEntity == null) { // If not authenticate display no messages
            push(packet.respond { number("MCNT", 0) })
            return
        }

        val ip = socketChannel.remoteAddress().toString()
        val menuMessage = Environment.menuMessage
            .replace("{v}", Constants.KME_VERSION)
            .replace("{n}", playerEntity.displayName)
            .replace("{ip}", ip) + 0xA.toChar()

        pushAll(
            packet.respond { number("MCNT", 1) }, // Number of messages

            notify(Components.MESSAGING, Commands.SEND_MESSAGE) {
                number("FLAG", 0x1)
                number("MGID", 0x1)
                text("NAME", menuMessage)
                +group("PYLD") {
                    map("ATTR", mapOf("B0000" to "160"))
                    // Flag types
                    // 0x1 = Default message
                    // 0x2 = Unlocked acomplishment

                    number("FLAG", 0x1)
                    number("STAT", 0x0)
                    number("TAG", 0x0)
                    tripple(
                        "TARG",
                        Components.USER_SESSIONS,
                        Commands.SET_SESSION,
                        playerEntity.playerId
                    )
                    number("TYPE", 0x0)
                }
                tripple(
                    "SRCE",
                    Components.USER_SESSIONS,
                    Commands.SET_SESSION,
                    playerEntity.playerId
                )
                number("TIME", unixTimeSeconds())
            }
        )
    }


    // endregion

    // region Association Lists Handlers

    /**
     * Needs further investigation for proper documentation.
     *
     * @param packet
     */
    @PacketHandler(Components.ASSOCIATION_LISTS, Commands.GET_LISTS)
    fun handleAssociationListGetLists(packet: Packet) {
        push(packet.respond {
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
        })
    }

    // endregion

    // region Game Reporting Handlers

    /**
     * Handles the submission of an offline game report.
     *
     * Needs further investigation for proper documentation.
     *
     * @param packet The game reporting packet
     */
    @PacketHandler(Components.GAME_REPORTING, Commands.SUBMIT_OFFLINE_GAME_REPORT)
    fun handleSubmitOfflineReport(packet: Packet) {
        push(packet.respond())
        push(notify(Components.GAME_REPORTING, Commands.NOTIFY_GAME_REPORT_SUBMITTED) {
            varList("DATA")
            number("EROR", 0)
            number("FNL", 0)
            number("GHID", 0)
            number("GRID", 0) // Game Report ID
        })
    }

    // endregion

    // region User Sessions Handlers

    /**
     * Handles resuming a session that was present on a previous run or
     * that was logged out. This is done using the session key that was
     * provided to that session upon authenticating. The session key
     * provided by this packet is looked up in the database and if a
     * player is found with a matching one they become authenticated
     *
     * @param packet The packet requesting the session resumption
     */
    @PacketHandler(Components.USER_SESSIONS, Commands.RESUME_SESSION)
    fun handleResumeSession(packet: Packet) {
        try {
            val database = Environment.database
            val sessionToken = packet.text("SKEY")
            val player = database.getPlayerBySessionToken(sessionToken)
            if (player == null) {
                push(LoginError.INVALID_INFORMATION(packet))
                return
            }
            setAuthenticatedPlayer(player)
            push(packet.respond())
        } catch (e: DatabaseException) {
            Logger.warn("Failed to resume session", e)
            push(packet.respond())
        }
    }

    /**
     * The packet recieved from the client contains networking information
     * including the external and internal ip addresses and ports along with
     * the natt type. All of this information is stored. This handler responds
     * with a set session packet to update the clients view of its session
     *
     * @param packet The packet containing the network update information
     */
    @PacketHandler(Components.USER_SESSIONS, Commands.UPDATE_NETWORK_INFO)
    fun updateNetworkInfo(packet: Packet) {
        val addr = packet.unionValue("ADDR") as GroupTdf
        setNetworkingFromGroup(addr)

        val nqos = packet.group("NQOS")
        dbps = nqos.number("DBPS")
        nattType = nqos.number("NATT")
        ubps = nqos.number("UBPS")

        val nlmp = packet.map<String, ULong>("NLMP")
        pslm[0] = nlmp.getOrDefault("ea-sjc", 0xfff0fffu)
        pslm[1] = nlmp.getOrDefault("rs-iad", 0xfff0fffu)
        pslm[2] = nlmp.getOrDefault("rs-lhr", 0xfff0fffu)

        pushAll(packet.respond(), createSetSessionPacket())
    }

    /**
     * Handles updating the hardware flag using the value provided by the
     * client using this packet. This handler responds with a set session
     * packet to update the clients view of its session
     *
     * @param packet The packet containing the hardware flag
     */
    @PacketHandler(Components.USER_SESSIONS, Commands.UPDATE_HARDWARE_FLAGS)
    fun updateHardwareFlag(packet: Packet) {
        hardwareFlag = packet.number("HWFG").toInt()
        push(packet.respond())
        push(createSetSessionPacket())
    }

    // endregion

    // region Util Handlers

    /**
     * Handles retrieving / creating of conf files and returning them
     * to the client. This includes talk files as well as other data
     * about the server
     *
     * - ME3_LIVE_TLK_PC_LANGUAGE: Talk files for the game
     * - ME3_DATA: Configurations and http server locations
     * - ME3_MSG: The current message this can be on the menu or in multiplayer
     * - ME3_ENT: Map of user entitlements (includes online access entitlement)
     * - ME3_DIME: Shop contents / Currency definition
     * - ME3_BINI_VERSION: BINI version information
     * - ME3_BINI_PC_COMPRESSED: ME3 BINI
     *
     * @param packet
     */
    @PacketHandler(Components.UTIL, Commands.FETCH_CLIENT_CONFIG)
    fun handleFetchClientConfig(packet: Packet) {
        val type = packet.text("CFID")
        val conf: Map<String, String> = if (type.startsWith("ME3_LIVE_TLK_PC_")) {
            val lang = type.substring(16)
            Data.getTalkFileConfig(lang)
        } else {
            when (type) {
                "ME3_DATA" -> Data.createDataConfig() // Configurations for GAW, images and others
                "ME3_MSG" -> emptyMap() // Custom multiplayer messages
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
        push(packet.respond {
            map("CONF", conf)
        })
    }

    /**
     * Handles responding to pings from the client. Responsd with the
     * server time in the response body.
     *
     * Currently this does nothing but update the last ping time
     * variable
     *
     * TODO: Implement actual ping timeout
     *
     * @param packet The ping packet
     */
    @PacketHandler(Components.UTIL, Commands.PING)
    fun handlePing(packet: Packet) {
        lastPingTime = System.currentTimeMillis()
        push(packet.respond {
            number("STIM", unixTimeSeconds())
        })
    }

    /**
     * Handles the pre authentication packet this includes information about the
     * client such as location, version, platform, etc. This response with information
     * about the current server configuration. This function updates [location]
     *
     * Other CINF Fields:
     *
     * - PLAT: The platform the game is running on (e.g. Windows)
     * - MAC: The mac address of the computer
     * - BSDK: The Blaze SDK version used in the client
     * - CVER: The mass effect client version
     * - ENV: The client environment type
     *
     * @param packet
     */
    @PacketHandler(Components.UTIL, Commands.PRE_AUTH)
    fun handlePreAuth(packet: Packet) {

        val infoGroup = packet.group("CINF")
        location = infoGroup.number("LOC")

        push(packet.respond {
            number("ANON", 0x0)
            text("ASRC", "303107")
            list(
                // Component IDS? (They match up so assumptions...)
                "CIDS", listOf(
                    Components.AUTHENTICATION,
                    Components.ASSOCIATION_LISTS,
                    Components.GAME_MANAGER,
                    Components.GAME_REPORTING,
                    Components.STATS,
                    Components.UTIL,
                    63490,
                    30720,
                    Components.MESSAGING,
                    30721,
                    Components.USER_SESSIONS,
                    30723,
                    30725,
                    30726,
                    Components.DYNAMIC_FILTER
                )
            )
            text("CNGN", "")
            +group("CONF") {
                map(
                    "CONF", mapOf(
                        "pingPeriod" to "15s", // The delay between each ping to the server
                        "voipHeadsetUpdateRate" to "1000", // The rate at which headsets are updated
                        "xlspConnectionIdleTimeout" to "300" // The xlsp connection idle timeout
                    )
                )
            }
            text("INST", "masseffect-3-pc") // The type of server?
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
            text("SVER", "Blaze 3.15.08.0 (CL# 1629389)") // Blaze Server Version
        })
    }

    /**
     * Handles the post authentication packet which responds with
     * information about the ticker and telemetry servers
     *
     * @param packet The packet requesting the post auth information
     */
    @PacketHandler(Components.UTIL, Commands.POST_AUTH)
    fun handlePostAuth(packet: Packet) {
        push(packet.respond {
            +group("PSS") { // Player Sync Service
                text("ADRS", "playersyncservice.ea.com") // Host / Address
                blob("CSIG")
                text("PJID", "303107")
                number("PORT", 443) // Port
                number("RPRT", 0xF)
                number("TIID", 0x0)
            }

            //  telemetryAddress = "reports.tools.gos.ea.com:9988"
            //  tickerAddress = "waleu2.tools.gos.ea.com:8999"

            +group("TELE") {
                text("ADRS", "127.0.0.1") // Server Address
                number("ANON", 0)
                text("DISA", "**")
                text("FILT", "-UION/****") // Telemetry filter?
                number("LOC", 1701725253)
                text("NOOK", "US,CA,MX")
                number("PORT", 9988)
                number("SDLY", 15000)
                text("SESS", "JMhnT9dXSED")
                text("SKEY", "")
                number("SPCT", 0x4B)
                text("STIM", "")
            }

            +group("TICK") {
                text("ADRS", "127.0.0.1")
                number("port", 9988)
                text("SKEY", "823287263,10.23.15.2:8999,masseffect-3-pc,10,50,50,50,50,0,12")
            }

            +group("UROP") {
                number("TMOP", 0x1)
                number("UID", sessionId)
            }
        })
    }

    /**
     * Handles updating an individual setting provided by the client in the
     * form of a key value pair named KEY and DATA. This is for updating any
     * data stored on the player such as inventory, characters, classes etc
     *
     * @param packet The packet updating the setting
     */
    @PacketHandler(Components.UTIL, Commands.USER_SETTINGS_SAVE)
    fun handleUserSettingsSave(packet: Packet) {
        val playerEntity = player ?: throw NotAuthenticatedException()
        val value = packet.textOrNull("DATA")
        val key = packet.textOrNull("KEY")
        if (value != null && key != null) {
            playerEntity.setPlayerData(key, value)
        }
        push(packet.respond())
    }

    /**
     * Handles loading all the user settings for the authenticated users. This
     * loads all the player entity settings from the database and puts them
     * as key value pairs into a map which is sent to the client.
     *
     * @param packet The packet requesting all the settings
     */
    @PacketHandler(Components.UTIL, Commands.USER_SETTINGS_LOAD_ALL)
    fun handleUserSettingsLoadAll(packet: Packet) {
        val playerEntity = player ?: throw NotAuthenticatedException()
        push(packet.respond {
            map("SMAP", playerEntity.createSettingsMap())
        })
    }

    /**
     * Handles suspend user pings. The purpose of this is not yet understood,
     * and it requires further investigation before it can be documented
     *
     * @param packet The packet for suspend user ping
     */
    @PacketHandler(Components.UTIL, Commands.SUSPEND_USER_PING)
    fun handleSuspendUserPing(packet: Packet) {
        push(
            when (packet.numberOrNull("TVAL")) {
                0x1312D00uL -> packet.error(0x12D)
                0x55D4A80uL -> packet.error(0x12E)
                else -> packet.respond()
            }
        )
    }

    // endregion

    // endregion

    // region Packet Generators

    /**
     * Notifies the client for this session that it failed to complete
     * matchmaking. This also makes a call to [resetMatchmakingState]
     * to reset the matchmaking state.
     */
    fun notifyMatchmakingFailed() {
        resetMatchmakingState()
        val playerEntity = player ?: return
        push(
            notify(Components.GAME_MANAGER, Commands.NOTIFY_MATCHMAKING_FAILED) {
                number("MAXF", 0x5460)
                number("MSID", matchmakingId)
                number("RSLT", 0x4)
                number("USID", playerEntity.playerId)
            }
        )
    }

    /**
     * Notifies the client for this session of its current matchmaking status
     * the fields for this packet need to be investigated further as it doesn't
     * entirely work properly at the moment
     */
    fun notifyMatchmakingStatus() {
        val playerEntity = player ?: return
        push(
            notify(
                Components.GAME_MANAGER,
                Commands.NOTIFY_MATCHMAKING_ASYNC_STATUS
            ) {
                list("ASIL", listOf(
                    group {
                        +group("CGS") {
                            number("EVST", if (matchmaking) 0x6 else 0x0)
                            number("MMSN", 0x1)
                            number("NOMP", 0x0)
                        }
                        +group("CUST") {
                        }
                        +group("DNFS") {
                            number("MDNF", 0x0)
                            number("XDNF", 0x0)
                        }
                        +group("FGS") {
                            number("GNUM", 0x0)
                        }
                        +group("GEOS") {
                            number("DIST", 0x0)
                        }
                        map(
                            "GRDA", mapOf(
                                "ME3_gameDifficultyRule" to group {
                                    text("NAME", "ME3_gameDifficultyRule")
                                    list("VALU", listOf("difficulty3"))
                                },
                                "ME3_gameEnemyTypeRule" to group {
                                    text("NAME", "ME3_gameEnemyTypeRule")
                                    list("VALU", listOf("enemy4"))
                                },
                                "ME3_gameMapMatchRule" to group {
                                    text("NAME", "ME3_gameMapMatchRule")
                                    list(
                                        "VALU",
                                        listOf(
                                            "map0", "map1", "map2", "map3", "map4", "map5", "map6",
                                            "map7", "map8", "map9", "map10", "map11", "map12", "map13",
                                            "map14", "map15", "map16", "map17", "map18", "map19", "map20",
                                            "map21", "map22", "map23", "map24", "map25", "map26", "map27",
                                            "map28", "map29", "random", "abstain"
                                        )
                                    )
                                },
                                "ME3_gameStateMatchRule" to group {
                                    text("NAME", "ME3_gameStateMatchRule")
                                    list("VALU", listOf("IN_LOBBY", "IN_LOBBY_LONGTIME", "IN_GAME_STARTING", "abstain"))
                                },
                                "ME3_rule_dlc2300" to group {
                                    text("NAME", "ME3_rule_dlc2300")
                                    list("VALU", listOf("required", "preferred"))
                                },
                                "ME3_rule_dlc2500" to group {
                                    text("NAME", "ME3_rule_dlc2500")
                                    list("VALU", listOf("required", "preferred"))
                                },
                                "ME3_rule_dlc2700" to group {
                                    text("NAME", "ME3_rule_dlc2700")
                                    list("VALU", listOf("required", "preferred"))
                                },
                                "ME3_rule_dlc3050" to group {
                                    text("NAME", "ME3_rule_dlc3050")
                                    list("VALU", listOf("required", "preferred"))
                                },
                                "ME3_rule_dlc3225" to group {
                                    text("NAME", "ME3_rule_dlc3225")
                                    list("VALU", listOf("required", "preferred"))
                                },
                            )
                        )
                        +group("GSRD") {
                            number("PMAX", 0x4)
                            number("PMIN", 0x2)
                        }
                        +group("HBRD") {
                            number("BVAL", 0x1)
                        }
                        +group("HVRD") {
                            number("VVAL", 0x0)
                        }
                        +group("PSRS") {
                        }
                        +group("RRDA") {
                            number("RVAL", 0x0)
                        }
                        +group("TSRS") {
                            number("TMAX", 0x0)
                            number("TMIN", 0x0)
                        }
                        map(
                            "UEDS", mapOf(
                                "ME3_characterSkill_Rule" to group {
                                    number("AMAX", 0x1f4)
                                    number("AMIN", 0x0)
                                    number("MUED", 0x1f4)
                                    text("NAME", "ME3_characterSkill_Rule")
                                },
                            )
                        )
                        +group("VGRS") {
                            number("VVAL", 0x0)
                        }
                    }
                ))
                number("MSID", matchmakingId)
                number("USID", playerEntity.playerId)
            }
        )
    }

    /**
     * Creates the group TDF that stores the external
     * networking information (ip and port)
     *
     * @return The created group tdf
     */
    fun createExternalNetGroup(): GroupTdf {
        return group("EXIP") {
            number("IP", externalAddress)
            number("PORT", externalPort)
        }
    }


    /**
     * Creates the group TDF that stores the internal
     * networking information (ip and port)
     *
     * @return The created group tdf
     */
    fun createInternalNetGroup(): GroupTdf {
        return group("INIP") {
            number("IP", internalAddress)
            number("PORT", internalPort)
        }
    }

    /**
     * Creates the optional tdf value that stores the networking
     * information for this session. If the networking information
     * is unset ([isNetworkingUnset]) then this will return an empty
     * optional.
     *
     * @param label The label to give the created tdf
     * @return The created optional tdf
     */
    private fun createNetworkingTdf(label: String): OptionalTdf {
        return if (isNetworkingUnset) { // If networking information hasn't been provided
            OptionalTdf(label)
        } else {
            OptionalTdf(label, 0x02u, group("VALU") {
                +createExternalNetGroup()
                +createInternalNetGroup()
            })
        }
    }

    /**
     * Sets the current internal and external address
     * and port information from the provided group
     * tdf.
     *
     * @param group The tdf containing the EXIP and INIP groups
     */
    private fun setNetworkingFromGroup(group: GroupTdf) {
        val exip = group.group("EXIP")
        externalAddress = exip.number("IP")
        externalPort = exip.number("PORT")

        val inip = group.group("INIP")
        internalAddress = inip.number("IP")
        internalPort = inip.number("PORT")

        isNetworkingUnset = false
    }

    /**
     * Pushes the packets that update the information about this session
     * to which ever [session] is provided. This is used to update the user
     * information as well as session information for each session.
     *
     * @param session The session to send the update to
     */
    fun updateSessionFor(session: Session) {
        val playerEntity = player ?: return
        val sessionDetailsPacket = notify(
            Components.USER_SESSIONS,
            Commands.SESSION_DETAILS
        ) {
            +createSessionDataGroup()
            +group("USER") {
                number("AID", playerEntity.playerId)
                number("ALOC", location)
                blob("EXBB")
                number("EXID", 0)
                number("ID", playerEntity.playerId)
                text("NAME", playerEntity.displayName)
            }
        }

        val identityPacket = notify(
            Components.USER_SESSIONS,
            Commands.UPDATE_EXTENDED_DATA_ATTRIBUTE
        ) {
            number("FLGS", 0x3uL)
            number("ID", playerEntity.playerId)
        }

        session.pushAll(sessionDetailsPacket, identityPacket)
    }

    /**
     * Creates a tdf group with the session data information.
     * This is used by [Commands.SESSION_DETAILS] and
     * [Commands.SET_SESSION]
     *
     * @return The created tdf group
     */
    private fun createSessionDataGroup(): GroupTdf {
        return group("DATA") {
            +createNetworkingTdf("ADDR")
            text("BPS", "ea-sjc")
            text("CTY")
            varList("CVAR")
            map("DMAP", mapOf(0x70001 to 0x409a))
            number("HWFG", hardwareFlag)
            list("PSLM", pslm)
            +group("QDAT") {
                number("DBPS", dbps)
                number("NATT", nattType)
                number("UBPS", ubps)
            }
            number("UATT", 0)
            list("ULST", listOf(VarTriple(4u, 1u, gameIdSafe)))
        }
    }

    /**
     * Creates a packet which sets the session details for this
     * session.
     *
     * @return The created packet
     */
    fun createSetSessionPacket(): Packet {
        return notify(
            Components.USER_SESSIONS,
            Commands.SET_SESSION
        ) {
            +createSessionDataGroup()
            number("USID", player?.playerId ?: 1)
        }
    }

    /**
     * Creates player data group this is used by games and
     * contains information about the player and the session
     * this includes networking information
     *
     * @return The created group tdf
     */
    fun createPlayerDataGroup(): GroupTdf {
        val playerId = playerIdSafe
        val displayName = player?.displayName ?: ""
        return group("PDAT") {
            blob("BLOB")
            number("EXID", 0x0)
            number("GID", gameIdSafe) // Current game ID
            number("LOC", location) // Encoded Location
            text("NAME", displayName) // Player Display Name
            number("PID", playerId)  // Player ID
            +createNetworkingTdf("PNET") // Player Network Information
            number("SID", gameSlot) // Player Slot Index/ID
            number("SLOT", 0x0)
            number("STAT", 0x2)
            number("TIDX", 0xffff)
            number("TIME", 0x0)
            tripple("UGID", 0x0, 0x0, 0x0)
            number("UID", playerId) // Player ID
        }
    }

    /**
     * Creates an authenticated response message for the provided
     * packet and returns the created message
     *
     * @param packet The packet to create the response for
     * @return The created response
     *
     * @throws NotAuthenticatedException If the session is not authenticated
     */
    private fun createAuthenticatedResponse(packet: Packet): Packet {
        val playerEntity = player ?: throw NotAuthenticatedException()
        return packet.respond {
            text("LDHT", "")
            number("NTOS", 0)
            text("PCTK", playerEntity.getSessionToken()) // PC Session Token
            list("PLST", listOf(createPersonaGroup())) // Persona List
            text("PRIV", "")
            text("SKEY", playerEntity.getSessionToken())
            number("SPAM", 0)
            text("THST", "")
            text("TSUI", "")
            text("TURI", "")
            number("UID", playerEntity.playerId) // Player ID
        }
    }

    private fun createSilentAuthenticatedResponse(packet: Packet): Packet {
        val player = player ?: throw NotAuthenticatedException()
        setAuthenticatedPlayer(player)
        // We don't store last login time so this is just computed here
        return packet.respond {
            number("AGUP", 0)
            text("LDHT", "")
            number("NTOS", 0)
            text("PCTK", player.getSessionToken())
            text("PRIV", "")
            +group("SESS") { appendDetailsTo(this) }
            number("SPAM", 0)
            text("THST", "")
            text("TSUI", "")
            text("TURI", "")
        }
    }

    /**
     * Appends details about this session to the provided
     * tdf builder.
     *
     * @param builder The builder to append to
     */
    private fun appendDetailsTo(builder: TdfBuilder) {
        val playerEntity = player ?: throw NotAuthenticatedException()
        with(builder) {
            number("BUID", playerEntity.playerId)
            number("FRST", 0)
            text("KEY", playerEntity.getSessionToken())
            number("LLOG", 0)
            text("MAIL", playerEntity.email) // Player Email
            +createPersonaGroup()
            number("UID", playerEntity.playerId) // Player ID
        }
    }

    /**
     * Create's a "persona" group tdf value for this session. The EA system has a
     * whole "persona" system but there's no need for that system to be implemented
     * in this project so instead the details are just filled with the player details
     *
     * @return The created persona group tdf
     */
    private fun createPersonaGroup(): GroupTdf {
        val playerEntity = player ?: throw NotAuthenticatedException()
        return group("PDTL") {
            text("DSNM", playerEntity.displayName) // Player Display Name
            number("LAST", 0) // Last login time (Ignored)
            number("PID", playerEntity.playerId) // Player ID
            number("STAS", 0)
            number("XREF", 0)
            number("XTYP", 0)
        }
    }

    // endregion

    /**
     * Removes the player from the game it is currently in. (If it exists)
     * and then sets the current game to null
     */
    private fun removeFromGame() {
        resetMatchmakingState()
        game?.removeAtIndex(gameSlot)
        clearGame()
    }

    /**
     * Handles removing all references to this session. This will allow
     * it to be garbage collected preventing memory leaks.
     */
    private fun dispose() {
        setAuthenticatedPlayer(null)
        if (matchmaking) Matchmaking.removeFromQueue(this)
        // TODO: REMOVE ALL REFERENCES TO THIS OBJECT SO IT CAN BE GARBAGE COLLECTED
    }

    /**
     * Handles disposing of any references to this session when
     * the underlying channel becomes inactive / disconnected
     *
     * @param ctx The channel context
     */
    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)
        dispose()

        val channel = ctx.channel()
        channel.pipeline()
            .remove(this)
    }

    /**
     * Equality for sessions is only checked by reference
     * and the actual session ID itself as those will always
     * be unique.
     *
     * @param other The other object to check equality with
     * @return Whether the two objects are equal
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Session) return false
        if (sessionId != other.sessionId) return false
        return true
    }

    /**
     * The hash code for sessions is just the hashcode
     * of the session ID
     *
     * @return The hash code value
     */
    override fun hashCode(): Int {
        return sessionId.hashCode()
    }


    companion object {

        /**
         * The integer value which is used as the ID of the
         * next session. This is incremented as each session
         * takes their ID
         */
        private val nextSessionId = AtomicInteger(0)
    }
}
