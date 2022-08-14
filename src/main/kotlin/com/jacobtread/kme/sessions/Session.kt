package com.jacobtread.kme.sessions

import com.jacobtread.blaze.*
import com.jacobtread.blaze.annotations.PacketProcessor
import com.jacobtread.blaze.data.VarTriple
import com.jacobtread.blaze.handler.PacketNettyHandler
import com.jacobtread.blaze.logging.PacketLogger
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.tdf.types.GroupTdf
import com.jacobtread.blaze.tdf.types.OptionalTdf
import com.jacobtread.kme.data.blaze.LoginError
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.database.data.Player
import com.jacobtread.kme.exceptions.GameException
import com.jacobtread.kme.game.Game
import com.jacobtread.kme.game.match.Matchmaking
import com.jacobtread.kme.utils.getIPv4Encoded
import com.jacobtread.kme.utils.logging.Logger
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.net.Inet4Address
import java.net.InetSocketAddress
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
class Session(
    /**
     * The socket channel that this session belongs to
     */
    override val channel: Channel,
) : PacketNettyHandler() {

    /**
     * The unique identifier for this session. Retrieves from the
     * atomic integer value and increases it for the next session
     */
    val sessionId = nextSessionId.getAndIncrement()

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
    var nattType: Int = 0
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
    private var pslm: ULong = 0xfff0fffu

    var location: ULong = 0x64654445uL

    /**
     * The unix timestamp in milliseconds of when the last ping packet was
     * recieved from the client. -1 until the first ping is recieved.
     *
     * TODO: Implement timeout using this
     */
    var lastPingTime: Long = -1L

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

    private var playerState: Int = 2

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
        val remoteAddress = channel.remoteAddress()

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
        PacketLogger.setContext(channel, builder.toString())
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
    fun setAuthenticatedPlayer(player: Player?) {
        val existing = this.player
        if (existing != player) {
            removeFromGame()
        }
        this.player = player
        // Update the encoder context because player has changed
        updateEncoderContext()
    }

    override fun handlePacket(ctx: ChannelHandlerContext, packet: Packet) {
        routePacket(channel, packet)
    }

    override fun handleConnectionLost(ctx: ChannelHandlerContext) {
        val ipAddress = channel.remoteAddress()
        Logger.debug("Connection to client at $ipAddress lost")
    }

    override fun handleException(ctx: ChannelHandlerContext, cause: Throwable, packet: Packet) {
        when (cause) {
            is NotAuthenticatedException -> {
                push(LoginError.INVALID_ACCOUNT(packet))
                val address = channel.remoteAddress()
                Logger.warn("Client at $address tried to access a authenticated route without authenticating")
            }

            is GameException -> {
                Logger.warn("Client caused game exception", cause)
                push(packet.respond())
            }
            else -> {
                Logger.warn("Failed to handle packet: $packet", cause)
                super.handleException(ctx, cause, packet)
            }
        }
    }

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
                number("RSLT", 4)
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
                list("ASIL", emptyList<GroupTdf>())
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
    private fun createExternalNetGroup(): GroupTdf {
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
    private fun createInternalNetGroup(): GroupTdf {
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
            OptionalTdf(label, 0x2u, group("VALU") {
                +createExternalNetGroup()
                +createInternalNetGroup()
            })
        }
    }

    /**
     * Creates host networking group tdf. This is used for
     * the host connection details in [Game.notifyGameSetup]
     *
     * @return The HNET group
     */
    fun createHNET(): GroupTdf {
        return group(start2 = true) {
            +createExternalNetGroup()
            +createInternalNetGroup()
        }
    }

    /**
     * Sets the current internal and external address
     * and port information from the provided group
     * tdf.
     *
     * @param group The tdf containing the EXIP and INIP groups
     */
    fun setNetworkingFromGroup(group: GroupTdf) {
        val exip = group.group("EXIP")
        externalAddress = exip.ulong("IP")
        externalPort = exip.ulong("PORT")

        val inip = group.group("INIP")
        internalAddress = inip.ulong("IP")
        internalPort = inip.ulong("PORT")

        isNetworkingUnset = false

        val remoteAddress = channel.remoteAddress()
        if (remoteAddress is InetSocketAddress) {
            val addr = remoteAddress.address
            if (addr is Inet4Address) {
                externalAddress = getIPv4Encoded(addr.hostAddress)
                externalPort = internalPort
            }
        }

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
     * Handles setting of the player state variable.
     * After the variable is set a notification packet
     * is sent to the other players in the game.
     *
     * Notification packet won't be sent if the player
     * is not in a game.
     *
     * @param value The new player state value.
     */
    fun setPlayerState(value: Int) {
        playerState = value
        val game = game ?: return
        game.pushAll(
            notify(Components.GAME_MANAGER, Commands.NOTIFY_GAME_PLAYER_STATE_CHANGE) {
                number("GID", game.id)
                number("PID", playerIdSafe)
                number("STAT", value)
            }
        )
    }

    /**
     * Set's the hardware flag value for this session
     * and sends a set session packet to the client
     *
     * @param value The new hardware flag value
     */
    fun setHardwareFlag(value: Int) {
        hardwareFlag = value
        push(createSetSessionPacket())
    }

    /**
     * Sets the networking data for this session
     * and sends a set session packet to the client
     *
     * @param dbps
     * @param nattType
     * @param ubps
     * @param pslm
     */
    fun setNetworkingData(
        addr: GroupTdf,
        dbps: ULong,
        nattType: Int,
        ubps: ULong,
        pslm: ULong,
    ) {
        setNetworkingFromGroup(addr)
        this.dbps = dbps
        this.nattType = nattType
        this.ubps = ubps
        this.pslm = pslm
        push(createSetSessionPacket())
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
            number("USID", playerIdSafe)
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
            number("STAT", playerState)
            number("TIDX", 0xffff)
            number("TIME", 0x0)
            tripple("UGID", 0x0, 0x0, 0x0)
            number("UID", playerId) // Player ID
        }
    }

    /**
     * Creates an authentication response packet which is either
     * for silent authentication (Origin / Token) or visible
     * (Login / Create)
     *
     * @param packet The packet this response is for.
     * @param isSilent Whether the auth response is a result of a silent login (Origin / Token)
     */
    fun doAuthenticate(player: Player, packet: Packet, isSilent: Boolean) {
        setAuthenticatedPlayer(player)
        push(packet.respond {
            if (isSilent) number("AGUP", 0)
            text("LDHT", "")
            number("NTOS", 0)
            text("PCTK", player.getSessionToken())
            if (isSilent) {
                text("PRIV", "")
                +group("SESS") { appendDetailsTo(this) }
            } else {
                list("PLST", listOf(createPersonaGroup()))
                text("PRIV", "")
                text("SKEY", player.getSessionToken())
            }
            number("SPAM", 0)
            text("THST", "")
            text("TSUI", "")
            text("TURI", "")
            if (!isSilent) number("UID", player.playerId) // Player ID
        })
        if (isSilent) updateSessionFor(this)
    }

    /**
     * Appends details about this session to the provided
     * tdf builder.
     *
     * @param builder The builder to append to
     */
    fun appendDetailsTo(builder: TdfBuilder) {
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
     * Returns the string representation of the ip
     * address of the connected client.
     *
     * @return The ip address string
     */
    fun getAddressString(): String {
        return channel.remoteAddress().toString()
    }

    /**
     * Removes the player from the game it is currently in. (If it exists)
     * and then sets the current game to null
     */
    fun removeFromGame() {
        resetMatchmakingState()
        game?.removePlayer(this)
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
