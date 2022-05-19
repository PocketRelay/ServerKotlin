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

    private var player: Player? = null
    var sendOffers = false
    var lastPingTime = -1L
    var exip = NetData.DEFAULT
    var inip = NetData.DEFAULT
    var isActive = true
    var waitingForJoin = false


    val playerId: Int get() = player!!.playerId

    fun getPlayer(): Player = player ?: throw IllegalStateException("Tried to access player on session without logging in")


    fun setPlayer(player: Player) {
        this.player = player
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
    fun createPDTL(player: Player): StructTdf = struct("PDTL") {
        val lastLoginTime = unixTimeSeconds()
        text("DSNM", player.displayName)
        number("LAST", lastLoginTime)
        number("PID", player.id.value)
        number("STAS", 0)
        number("XREF", 0)
        number("XTYP", 0)
    }

    @Suppress("SpellCheckingInspection")
    fun createMMSessionDetails(game: Game): Packet = unique(
        Component.USER_SESSIONS,
        Command.SESSION_DETAILS
    ) {
        val player = getPlayer()
        +struct("DATA") {
            +createAddrUnion("ADDR")
            text("BPS", "rs-lhr")
            text("CTY", "")
            varList("CVAR", listOf())
            map(
                "DMAP", mapOf(0x70001 to 0x291,)
            )
            number("HWFG", 0x1)
            list("PSLM", listOf(0xea, 0x9c, 0x5e))
            +struct("QDAT") {
                number("DBPS", 0x0)
                number("NATT", Data.NAT_TYPE)
                number("UBPS", 0x0)
            }
            number("UATT", 0x0)
            list("ULST", listOf(VarTripple(0x4, 0x1, game.id.toLong())))
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
