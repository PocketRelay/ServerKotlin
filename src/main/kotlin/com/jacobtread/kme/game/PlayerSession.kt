package com.jacobtread.kme.game

import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.tdf.GroupTdf
import com.jacobtread.kme.blaze.tdf.OptionalTdf
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.database.Player
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

    companion object {
        val SESSION_ID = AtomicInteger(0)

        const val PLAYER_ID_FLAG = 3
    }

    lateinit var channel: Channel

    data class NetData(var address: Long, var port: Int) {
        companion object {
            val DEFAULT = NetData(0, 0)
        }
    }

    val sessionId = SESSION_ID.getAndIncrement()

    private var _player: Player? = null
    var game: Game? = null

    val player: Player get() = _player ?: throw throw IllegalStateException("Tried to access player on session without logging in")
    val playerId: Int get() = player.playerId

    var sendSession = false
    var lastPingTime = -1L
    var exip = NetData.DEFAULT
    var inip = NetData.DEFAULT
    var isActive = true
    var waitingForJoin = false

    fun setAuthenticated(player: Player) {
        this._player = player
    }


    @Suppress("SpellCheckingInspection")
    fun createSetSession(): Packet = unique(Component.USER_SESSIONS, Command.SET_SESSION) {
        +group("DATA") {
            +createAddrOptional("ADDR")
            text("BPS", "ea-sjc")
            text("CTY", "")
            varList("CVAR", emptyList())
            map("DMAP", mapOf(0x70001 to 0x2e))
            number("HWFG", 0x0)
            list("PSLM", listOf(0xfff0fff, 0xfff0fff, 0xfff0fff))
            +group("QDAT") {
                number("DBPS", 0x0)
                number("NATT", Data.NAT_TYPE)
                number("UBPS", 0x0)
            }
            number("UATT", 0x0)
            list("ULST", listOf(VarTripple(0x4, 0x1, 0x5dc695)))
        }
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
            Component.USER_SESSIONS,
            Command.UPDATE_EXTENDED_DATA_ATTRIBUTE
        ) {
            number("FLGS", PLAYER_ID_FLAG)
            number("ID", playerId)
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
            Component.USER_SESSIONS,
            Command.SESSION_DETAILS,
        ) {
            // Session Data
            +group("DATA") {
                +createAddrOptional("ADDR")
                text("BPS")
                text("CTY")
                varList("CVAR")
                map("DMAP", mapOf(0x70001 to (if (game != null) 0x291 else 0x22)))
                number("HWFG", 0)
                if (game != null) {
                    list("PSLM", listOf(0xea, 0x9c, 0x5e))
                }
                +group("QDAT") {
                    number("DBPS", 0)
                    number("NATT", Data.NAT_TYPE)
                    number("UBPS", 0)
                }
                number("UATT", 0)
                if (game != null) {
                    list("ULST", listOf(VarTripple(0x4, 0x1, game.id)))
                }
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
                number("IP", exip.address)
                number("PORT", exip.port)
            }
            +group("INIP") {// Internal IP?
                number("IP", inip.address)
                number("PORT", inip.port)
            }
        })

    @Suppress("SpellCheckingInspection")
    fun createPersonaList(player: Player): GroupTdf =
        group("PDTL" /* Persona Details? */) {
            val lastLoginTime = unixTimeSeconds()
            text("DSNM", player.displayName)
            number("LAST", lastLoginTime)
            number("PID", player.playerId) // Persona ID?
            number("STAS", 0)
            number("XREF", 0)
            number("XTYP", 0)
        }

    @Suppress("SpellCheckingInspection")
    fun appendSession(builder: TdfBuilder) {
        builder.apply {
            number("BUID", player.playerId)
            number("FRST", 0)
            text("KEY", Data.SKEY2)
            number("LLOG", unixTimeSeconds())
            text("MAIL", player.email)
            +createPersonaList(player)
            number("UID", player.playerId)
        }
    }

}
