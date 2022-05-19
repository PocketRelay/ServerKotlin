package com.jacobtread.kme.game

import com.jacobtread.kme.blaze.struct
import com.jacobtread.kme.blaze.tdf.StructTdf
import com.jacobtread.kme.blaze.tdf.UnionTdf
import com.jacobtread.kme.database.Player
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
    var sendOffers: Boolean = false
    var lastPingTime: Long = -1L
    var exip: NetData = NetData.DEFAULT
    var inip: NetData = NetData.DEFAULT
    var isActive: Boolean = true

    val playerId: Int get() = player!!.playerId

    fun getPlayer(): Player = player ?: throw IllegalStateException("Tried to access player on session without logging in")


    fun setPlayer(player: Player) {
        this.player = player
    }

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

    fun createPDTL(player: Player): StructTdf = struct("PDTL") {
        val lastLoginTime = unixTimeSeconds()
        text("DSNM", player.displayName)
        number("LAST", lastLoginTime)
        number("PID", player.id.value)
        number("STAS", 0)
        number("XREF", 0)
        number("XTYP", 0)
    }

}
