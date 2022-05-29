package com.jacobtread.kme.game

import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.tdf.GroupTdf
import com.jacobtread.kme.blaze.tdf.OptionalTdf
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.database.Player
import com.jacobtread.kme.exceptions.NotAuthenticatedException
import com.jacobtread.kme.utils.VarTripple
import com.jacobtread.kme.utils.unixTimeSeconds
import io.netty.channel.Channel
import java.util.concurrent.atomic.AtomicInteger

/**
 * PlayerSession Stores information about a client that is connected to
 * the Main Server this information includes the authenticated player and
 * networking information that can be provided to the Game
 *
 * @constructor Create empty PlayerSession
 */
class PlayerSession {

    /**
     * NetData Represents an IP address and a port that belongs to a session
     * however if the session has not updated its networking information
     * then the SHARED_NET_DATA will be used by default
     *
     * @property address The encoded IP address of the network data
     * @property port The encoded port of the network data
     * @constructor Create empty NetData
     */
    data class NetData(var address: Long, var port: Int)

    companion object {
        // Atomic integer for incremental session ID's
        val SESSION_ID = AtomicInteger(0)

        // Shared global net data to prevent unnecessary allocation for initial connections
        val SHARED_NET_DATA = NetData(0, 0)

        /**
         * PLAYER_ID_FLAG The flag key for UPDATE_EXTENDED_DATA_ATTRIBUTE which indicates
         * to set the player id value
         */
        const val PLAYER_ID_FLAG = 3
    }

    // The unique identifier for this session.
    val sessionId = SESSION_ID.getAndIncrement()

    // The client channel that is linked to this session wrapped this is private so
    // that a game can't accidentally access it after its closed
    private var channel: Channel? = null

    // The networking data for this session
    var netData = SHARED_NET_DATA

    // The authenticated player for this session null if the player isn't authenticated
    private var _player: Player? = null
    val player: Player get() = _player ?: throw throw NotAuthenticatedException()
    val playerId: Int get() = player.playerId

    var sendSession = false
    // The time in milliseconds of when the last ping was received from the client
    var lastPingTime = -1L

    // Whether the session is still active or needs to be discarded
    var isActive = true

    var game: Game? = null

    // Whether the player is waiting in a matchmaking queue
    var matchmaking = false

    /**
     * release Handles cleaning up of this session after the session is
     * closed and no longer needed unsets the channel and player and
     * removes the player from any games to prevent memory leaks
     */
    fun release() {
        isActive = false
        _player = null
        channel = null
        game?.removePlayer(this)
        game = null
        if (matchmaking) Matchmaking.removeFromQueue(this)
    }

    /**
     * leaveGame Leaves the current game if we are in one
     */
    fun leaveGame() {
        game?.removePlayer(this)
        game = null
    }

    /**
     * send Sends multiple packets to the channel for this session. Will
     * write all the packets before flushing the channel
     *
     * @param packets The packets to send
     */
    fun send(vararg packets: Packet) {
        val channel = channel ?: return // TODO: Throw closed access exception?
        packets.forEach { channel.write(it) }
        channel.flush()
    }

    /**
     * send Sends a single packet to the channel and flushes straight away
     *
     * @param packet The packet to send
     */
    fun send(packet: Packet) {
        val channel = channel ?: return // TODO: Throw closed access exception?
        channel.write(packet)
        channel.flush()
    }

    /**
     * setAuthenticated Sets the currently authenticated player
     *
     * @param player The authenticated player
     */
    fun setAuthenticated(player: Player?) {
        val existing = _player
        if (player == null && existing != null) {
            game?.removePlayer(this)
        }
        this._player = player
    }

    /**
     * setChannel Sets the underlying channel
     *
     * @param channel The channel to set
     */
    fun setChannel(channel: Channel) {
        this.channel = channel
    }

    /**
     * createSetSession Actual correct name for this is unknown, but I've inferred its
     * name based on its function, and it appears to
     *
     * @return A USER_SESSIONS SET_SESSION packet
     */
    @Suppress("SpellCheckingInspection")
    fun createSetSession(): Packet = unique(Components.USER_SESSIONS, Commands.SET_SESSION) {
        +createSessionDataGroup(0x2e, listOf(0xfff0fff, 0xfff0fff, 0xfff0fff))
        number("USID", if (_player != null) playerId else sessionId)
    }

    /**
     * createIdentityUpdate Creates a packet which updates the ID of the
     * current player session for the client
     *
     * @return The packet which updates the client ID
     */
    @Suppress("SpellCheckingInspection")
    fun createIdentityUpdate(): Packet =
        unique(
            Components.USER_SESSIONS,
            Commands.UPDATE_EXTENDED_DATA_ATTRIBUTE
        ) {
            number("FLGS", PLAYER_ID_FLAG)
            number("ID", playerId)
        }

    /**
     * createSessionDataGroup Creates a GroupTDF containing the data surrounding
     * this session such as the current game ID and networking information
     * (as far as im able to decern from it)
     *
     * @param dmapValue Unknown But Nessicary
     * @param pslm Unknown But Nessicary
     * @return The created group
     */
    @Suppress("SpellCheckingInspection")
    private fun createSessionDataGroup(dmapValue: Int, pslm: List<Long>?): GroupTdf {
        return group("DATA") {
            +createAddrOptional("ADDR")
            text("BPS")
            text("CTY")
            varList("CVAR")
            map("DMAP", mapOf(0x70001 to dmapValue))
            number("HWFG", 0)
            if (pslm != null) {
                list("PSLM", pslm)
            }
            +group("QDAT") {
                number("DBPS", 0)
                number("NATT", Data.NAT_TYPE)
                number("UBPS", 0)
            }
            number("UATT", 0)
            list("ULST", listOf(VarTripple(0x4, 0x1, game?.id ?: Game.MIN_ID)))
        }

    }

    /**
     * createSessionDetails Creates a packet which describes the current
     * session information. This information includes the network information
     * as well as the player user information
     *
     * @return A USER_SESSIONS SESSION_DETAILS packet describing this session
     */
    @Suppress("SpellCheckingInspection")
    fun createSessionDetails(): Packet {
        val player = player
        val game = game
        return unique(
            Components.USER_SESSIONS,
            Commands.SESSION_DETAILS,
        ) {
            // Session Data
            if (game != null) {
                +createSessionDataGroup(0x291, listOf(0xea, 0x9c, 0x5e))
            } else {
                +createSessionDataGroup(0x22, null)
            }
            // Player Data
            +group("USER") {
                number("AID", player.playerId)
                number("ALOC", 0x64654445)
                blob("EXBB")
                number("EXID", 0)
                number("ID", player.playerId)
                text("NAME", player.displayName)
            }
        }
    }

    /**
     * createAddrUnion Creates a union with the network address values
     * presumably internal and external addresses (I assume) and returns it
     *
     * @param label The label to give the union
     * @return The created union
     */
    @Suppress("SpellCheckingInspection")
    fun createAddrOptional(label: String): OptionalTdf =
        OptionalTdf(label, 0x02, group("VALU") {
            +group("EXIP") { // External IP?
                number("IP", netData.address)
                number("PORT", netData.port)
            }
            +group("INIP") {// Internal IP?
                number("IP", netData.address)
                number("PORT", netData.port)
            }
        })

    /**
     * createPersonaList Creates a list of the account "personas" we don't
     * implement this "persona" system so this only ever has one value which
     * is the player account details
     *
     * @return The persona list
     */
    @Suppress("SpellCheckingInspection")
    fun createPersonaList(): GroupTdf {
        val player = player
        return group("PDTL" /* Persona Details? */) {
            val lastLoginTime = unixTimeSeconds()
            text("DSNM", player.displayName)
            number("LAST", lastLoginTime)
            number("PID", player.playerId) // Persona ID?
            number("STAS", 0)
            number("XREF", 0)
            number("XTYP", 0)
        }
    }

    /**
     * appendSession Appends the player session details to the
     * provided tdf builder
     *
     * @param builder The builder to append to
     */
    @Suppress("SpellCheckingInspection")
    fun appendPlayerSession(builder: TdfBuilder) {
        val player = player
        builder.apply {
            number("BUID", player.playerId)
            number("FRST", 0)
            text("KEY", Data.SKEY2)
            number("LLOG", unixTimeSeconds())
            text("MAIL", player.email)
            +createPersonaList()
            number("UID", player.playerId)
        }
    }

    override fun equals(other: Any?): Boolean = other is PlayerSession && sessionId == other.sessionId
    override fun hashCode(): Int = sessionId.hashCode()
}
