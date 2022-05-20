package com.jacobtread.kme.game

import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.tdf.StructTdf
import com.jacobtread.kme.blaze.tdf.UnionTdf
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.database.Player
import com.jacobtread.kme.utils.VarTripple
import com.jacobtread.kme.utils.unixTimeSeconds
import io.netty.channel.Channel

class PlayerSession(
    val id: Int,
) {

    lateinit var channel: Channel

    data class NetData(var address: Long, var port: Int) {
        companion object {
            val DEFAULT = NetData(0, 0)
        }
    }


    private var _player: Player? = null
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
        +struct("DATA") {
            +createAddrUnion("ADDR")
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
        number("USID", if (_player != null) playerId else id)
    }

    fun createSessionDetails(): Packet =
        unique(
            Component.USER_SESSIONS,
            Command.SESSION_DETAILS,
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
                number("AID", player.playerId)
                number("ALOC", 0x64654445)
                blob("EXBB")
                number("EXID", 0)
                number("ID", player.playerId)
                text("NAME", player.displayName)
            }
        }


    @Suppress("SpellCheckingInspection")
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

    @Suppress("SpellCheckingInspection")
    fun createPersonaList(player: Player): StructTdf =
        struct("PDTL" /* Persona Details? */) {
            val lastLoginTime = unixTimeSeconds()
            text("DSNM", player.displayName)
            number("LAST", lastLoginTime)
            number("PID", player.playerId) // Persona ID?
            number("STAS", 0)
            number("XREF", 0)
            number("XTYP", 0)
        }

    @Suppress("SpellCheckingInspection")
    fun createMMSessionDetails(game: Game): Packet = unique(
        Component.USER_SESSIONS,
        Command.SESSION_DETAILS
    ) {
        +struct("DATA") {
            +createAddrUnion("ADDR")
            text("BPS", "rs-lhr")
            text("CTY", "")
            varList("CVAR", listOf())
            map("DMAP", mapOf(0x70001 to 0x291))
            number("HWFG", 0x1)
            list("PSLM", listOf(0xea, 0x9c, 0x5e))
            +struct("QDAT") {
                number("DBPS", 0x0)
                number("NATT", Data.NAT_TYPE)
                number("UBPS", 0x0)
            }
            number("UATT", 0x0)
            list("ULST", listOf(VarTripple(0x4, 0x1, game.id)))
        }
        +struct("USER") {
            number("AID", player.playerId)
            number("ALOC", 0x656e4745)
            blob("EXBB")
            number("EXID", 0x0)
            number("ID", player.playerId)
            text("NAME", player.displayName)
        }
    }
}
