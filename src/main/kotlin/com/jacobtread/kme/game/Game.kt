package com.jacobtread.kme.game

import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.tdf.MapTdf
import com.jacobtread.kme.data.Data
import com.jacobtread.kme.utils.VarTripple
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class Game(
    val id: Long,
    val mid: Long,
    val host: PlayerSession,
) {

    companion object {
        private const val MAX_PLAYERS = 4
    }

    class GameAttributes {
        private val values = HashMap<String, String>()
        private var isDirty = false
        private var tdfMap: MapTdf? = null
        private val lock = ReentrantReadWriteLock()

        fun setAttribute(key: String, value: String) {
            lock.write {
                isDirty = true
                values[key] = value
            }
        }

        fun setBulk(values: Map<String, String>) {
            lock.write {
                isDirty = true
                this.values.putAll(values)
            }
        }

        fun getMap(): HashMap<String, String> {
            return lock.read { values }
        }
    }

    var gameState: Int = 0x1;
    var gameSetting: Int = 0x11f;
    val attributes = GameAttributes()
    private var isActive = true

    private val players = ArrayList<PlayerSession>(MAX_PLAYERS)
    private val playersLock = ReentrantReadWriteLock()

    init {
        players.add(host)
    }

    fun isInActive(): Boolean = !isActive || !host.isActive

    fun isFull(): Boolean = playersLock.read { players.size >= MAX_PLAYERS }
    fun isJoinable(): Boolean = isActive && !isFull()
    fun getActivePlayers(): List<PlayerSession> = playersLock.read {
        players.filter { it.isActive }
            .toMutableList()
    }

    fun join(player: PlayerSession) = playersLock.write {
        players.add(player)
        sendHostPlayerJoin(player)
    }

    @Suppress("SpellCheckingInspection")
    private fun sendHostPlayerJoin(session: PlayerSession) {
        val player = session.player
        val sessionDetails = session.createMMSessionDetails(this)
        host.channel.send(sessionDetails)
        host.channel.unique(Component.GAME_MANAGER, Command.JOIN_GAME_BY_GROUP) {
            number("GID", id)
            +struct("PDAT") {
                blob("BLOB")
                number("EXID", 0x0)
                number("GID", id)
                number("LOC", 0x64654445)
                text("NAME", player.displayName)
                number("PID", player.playerId)
                +session.createAddrUnion("PNET")
                number("SID", players.size)
                number("SLOT", 0x0)
                number("STAT", 0x2)
                number("TIDX", 0xffff)
                number("TIME", 0x0)
                tripple("UGID", 0x0, 0x0, 0x0)
                number("UID", player.playerId)
            }
        }

        host.channel.send(session.createSetSession())
    }

    fun removePlayer(playerId: Int) {
        if (playerId == host.playerId) {
            if (players.size < 1) {
                GameManager.releaseGame(this)
                isActive = false
                players.clear()
                return
            }
        }
        playersLock.write {
            players.removeIf { it.playerId == playerId }
        }
        playersLock.read {
            val hostPacket = unique(Component.USER_SESSIONS, Command.FETCH_EXTENDED_DATA) { number("BUID", host.playerId) }
            players.forEach {
                val userPacket = unique(Component.USER_SESSIONS, Command.FETCH_EXTENDED_DATA) { number("BUID", it.playerId) }
                it.channel.send(hostPacket)
                host.channel.send(userPacket)
            }
        }
    }

    fun broadcastAttributeUpdate() {
        playersLock.read {
            val packet = createNotifyPacket()
            players.forEach {
                it.channel.send(packet)
            }
        }
    }

    @Suppress("SpellCheckingInspection")
    fun createNotifyPacket(): Packet =
        unique(
            Component.GAME_MANAGER,
            Command.NOTIFY_GAME_UPDATED
        ) {
            map("ATTR", attributes.getMap())
            number("GID", id)
        }

    @Suppress("SpellCheckingInspection")
    fun createPoolPacket(init: Boolean): Packet =
        unique(
            Component.GAME_MANAGER,
            Command.RETURN_DEDICATED_SERVER_TO_POOL
        ) {
            val hostPlayer = host.player
            +struct("GAME") {
                // Game Admins
                list("ADMN", listOf(hostPlayer.playerId))
                map("ATTR", attributes.getMap())
                list("CAP", listOf(0x4, 0x0))
                number("GID", id)
                text("GNAM", hostPlayer.displayName)
                number("GPVH", 0x5a4f2b378b715c6)
                number("GSET", gameSetting)
                number("GSID", 0x4000000618E41C)
                number("GSTA", gameState)
                text("GTYP", "")
                // Host network information
                list("HNET", listOf(
                    struct(start2 = true) {
                        +struct("EXIP") {
                            number("IP", host.exip.address)
                            number("PORT", host.exip.port)
                        }
                        +struct("INIP") {
                            number("IP", host.inip.address)
                            number("PORT", host.inip.port)
                        }
                    }
                ))
                number("HSES", 0x112888c1)
                number("IGNO", 0x0)
                number("MCAP", 0x4)
                +struct("NQOS") {
                    number("DBPS", if (init) 0x0 else 0x5b8d800)
                    number("NATT", Data.NAT_TYPE)
                    number("UBPS", if (init) 0x0 else 0x4016400)
                }
                number("NRES", 0x0)
                number("NTOP", 0x0)
                text("PGID", "")
                blob("PGSR")
                +struct("PHST") {
                    number("HPID", hostPlayer.playerId)
                    number("HSLT", 0x0)
                }
                number("PRES", 0x1)
                text("PSAS", "")
                number("QCAP", 0x0)
                number("SEED", 0x2cf2048f)
                number("TCAP", 0x0)
                +struct("THST") {
                    number("HPID", hostPlayer.playerId)
                    number("HSLT", 0x0)
                }
                text("UUID", "f5193367-c991-4429-aee4-8d5f3adab938")
                number("VOIP", 0x2)
                text("VSTR", "ME3-295976325-179181965240128")
                blob("XNNC")
                blob("XSES")
            }

            val pros = players.mapIndexed { index, playerSession ->
                val tmppl = when (index) {
                    0 -> host
                    else -> if ((index - 1) < players.size) players[index - 1] else playerSession
                }
                val player = playerSession.player
                struct {
                    blob("BLOB")
                    number("EXID", 0x0)
                    number("GID", this@Game.id)
                    number("LOC", 0x64654445)
                    text("NAME", player.displayName)
                    number("PID", player.playerId)
                    +host.createAddrUnion("PNET")
                    number("SID", index)
                    number("SLOT", 0x0)
                    number("STAT", if (tmppl.playerId == player.playerId) 0x2 else 0x4)
                    number("TIDX", 0xffff)
                    number("TIME", 0x4fd4ce4f6a036)
                    tripple("UGID", 0x0, 0x0, 0x0)
                    number("UID", player.playerId)
                }
            }

            list("PROS", pros)
            if (init) {
                union("REAS", struct("VALU") {
                    number("DCTX", 0x0)
                })
            } else {
                union("REAS", 0x3, struct("VALU") {
                    number("FIT", 0x53fc)
                    number("MAXF", 0x5460)
                    number("MSID", mid)
                    number("RSLT", 0x2)
                    number("USID", hostPlayer.playerId)
                })
            }
        }

}