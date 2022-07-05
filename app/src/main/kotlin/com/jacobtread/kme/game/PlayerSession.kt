package com.jacobtread.kme.game

import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.tdf.GroupTdf
import com.jacobtread.kme.blaze.tdf.OptionalTdf
import com.jacobtread.kme.blaze.tdf.VarIntTdf
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.database.entities.PlayerEntity
import com.jacobtread.kme.blaze.NotAuthenticatedException
import com.jacobtread.kme.game.match.Matchmaking
import com.jacobtread.kme.blaze.data.VarTripple
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.tools.unixTimeSeconds
import io.netty.channel.Channel
import java.util.concurrent.atomic.AtomicInteger

/**
 * PlayerSession Stores information about a client that is connected to
 * the Main Server this information includes the authenticated player and
 * networking information that can be provided to the Game
 *
 * @constructor Create empty PlayerSession
 */
class PlayerSession : PacketPushable {

    /**
     * NetData Represents an IP address and a port that belongs to a session
     * however if the session has not updated its networking information
     * then the SHARED_NET_DATA will be used by default
     *
     * @property address The encoded IP address of the network data
     * @property port The encoded port of the network data
     * @constructor Create empty NetData
     */
    data class NetData(var address: ULong, var port: ULong) {
        fun createGroup(label: String): GroupTdf {
            return GroupTdf(
                label, false, listOf(
                    VarIntTdf("IP", address),
                    VarIntTdf("PORT", port)
                )
            )
        }

        fun isDefault(): Boolean = this === SHARED_NET_DATA
    }

    data class OtherNetData(
        val dbps: ULong,
        val natt: ULong,
        val ubps: ULong,
    )

    companion object {
        // Atomic integer for incremental session ID's
        val SESSION_ID = AtomicInteger(0)

        // Shared global net data to prevent unnecessary allocation for initial connections
        val SHARED_NET_DATA = NetData(0u, 0u)
        val SHARED_OTHER_NET_DATA = OtherNetData(0u, 4u, 0u)

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
    var extNetData = SHARED_NET_DATA
        set(value) {
            field = value
            Logger.logIfDebug { "Updated ext net data for user with session ID $sessionId" }
        }
    var intNetData = SHARED_NET_DATA
        set(value) {
            field = value
            Logger.logIfDebug { "Updated int net data for user with session ID $sessionId" }
        }

    var otherNetData = SHARED_OTHER_NET_DATA

    // The authenticated player for this session null if the player isn't authenticated
    private var _playerEntity: PlayerEntity? = null
    val playerEntity: PlayerEntity get() = _playerEntity ?: throw throw NotAuthenticatedException()
    val playerId: Int get() = playerEntity.playerId

    val displayName: String get() = playerEntity.displayName

    var hardwareFlag: Int = 0

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
        _playerEntity = null
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
     * push Sends a single packet to the channel for this
     * session then flushes
     *
     * @param packet
     */
    override fun push(packet: Packet) {
        val channel = channel ?: return // TODO: Throw closed access exception?
        channel.write(packet)
        channel.flush()
    }

    /**
     * setAuthenticated Sets the currently authenticated player
     *
     * @param playerEntity The authenticated player
     */
    fun setAuthenticated(playerEntity: PlayerEntity?) {
        val existing = _playerEntity
        if (playerEntity == null && existing != null) {
            game?.removePlayer(this)
        }
        this._playerEntity = playerEntity
    }

    /**
     * setChannel Sets the underlying channel
     *
     * @param channel The channel to set
     */
    fun setChannel(channel: Channel) {
        this.channel = channel
    }

    fun setNetworkingFromHNet(group: GroupTdf) {
        val exip = group.group("EXIP")
        val exipIp = exip.number("IP")
        val exipPort = exip.number("PORT")
        extNetData = NetData(exipIp, exipPort)

        val inip = group.group("INIP")
        val inipIp = inip.number("IP")
        val inipPort = inip.number("PORT")
        extNetData = NetData(inipIp, inipPort)
    }

    /**
     * createSetSession Actual correct name for this is unknown, but I've inferred its
     * name based on its function, and it appears to
     *
     * @return A USER_SESSIONS SET_SESSION packet
     */
    fun createSetSession(): Packet = unique(Components.USER_SESSIONS, Commands.SET_SESSION) {
        +createSessionDataGroup(0x2e, false)
        number("USID", playerId)
    }

    fun authResponse(packet: Packet) = packet.respond {
        val player = playerEntity
        text("LDHT", "")
        number("NTOS", 0)
        text("PCTK", player.sessionToken)
        list("PLST", listOf(createPersonaList()))
        text("PRIV", "")
        text("SKEY", "11229301_9b171d92cc562b293e602ee8325612e7")
        number("SPAM", 0)
        text("THST", "")
        text("TSUI", "")
        text("TURI", "")
        number("UID", player.playerId)
    }

    /**
     * createIdentityUpdate Creates a packet which updates the ID of the
     * current player session for the client
     *
     * @return The packet which updates the client ID
     */
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
    private fun createSessionDataGroup(dmapValue: Int, details: Boolean): GroupTdf {
        return group("DATA") {
            +createAddrOptional("ADDR")
            text("BPS", "ea-sjc")
            text("CTY")
            varList("CVAR")
            map("DMAP", mapOf(0x70001 to dmapValue))
            number("HWFG", hardwareFlag)
            if (details) {
                list("PSLM", listOf(if (matchmaking) 0x4e else 0x2f, 0xfff0fff, 0xfff0fff))
            } else {
                list("PSLM", listOf(0x9c, 0xfff0fff, 0xfff0fff))
            }
            +group("QDAT") {
                number("DBPS", otherNetData.dbps)
                number("NATT", otherNetData.natt)
                number("UBPS", otherNetData.ubps)
            }
            number("UATT", 0)
            list("ULST", listOf(VarTripple(4u, 1u, (game?.id ?: Game.MIN_ID))))
        }

    }

    /**
     * createSessionDetails Creates a packet which describes the current
     * session information. This information includes the network information
     * as well as the player user information
     *
     * @return A USER_SESSIONS SESSION_DETAILS packet describing this session
     */
    fun createSessionDetails(): Packet {
        val player = playerEntity
        val game = game
        return unique(
            Components.USER_SESSIONS,
            Commands.SESSION_DETAILS,
        ) {
            // Session Data
            if (game != null) {
                +createSessionDataGroup(0x291, true)
            } else {
                +createSessionDataGroup(0x22, true)
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
    fun createAddrOptional(label: String): OptionalTdf {
        return if (extNetData.isDefault() && intNetData.isDefault()) {
            OptionalTdf(label)
        } else {
            OptionalTdf(label, 0x02, group("VALU") {
                +extNetData.createGroup("EXIP")
                +intNetData.createGroup("INIP")
            })
        }
    }


    /**
     * createPersonaList Creates a list of the account "personas" we don't
     * implement this "persona" system so this only ever has one value which
     * is the player account details
     *
     * @return The persona list
     */
    fun createPersonaList(): GroupTdf {
        val player = playerEntity
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
    fun appendPlayerSession(builder: TdfBuilder) {
        val player = playerEntity
        builder.apply {
            number("BUID", player.playerId)
            number("FRST", 0)
            text("KEY", "11229301_9b171d92cc562b293e602ee8325612e7")
            number("LLOG", 0)
            text("MAIL", player.email)
            +createPersonaList()
            number("UID", player.playerId)
        }
    }

    /**
     * equals Equals checking only checks that the unique
     * session ID matches the other player session
     *
     * @param other The other object to check
     * @return Whether both sessions are equal
     */
    override fun equals(other: Any?): Boolean = other is PlayerSession && sessionId == other.sessionId

    /**
     * hashCode Uniqiue hash code for this object this
     * uses the player session ID as the hash code
     *
     * @return The hash code
     */
    override fun hashCode(): Int = sessionId.hashCode()
}
