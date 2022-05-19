package com.jacobtread.kme.game

import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.blaze.tdf.MapTdf
import com.jacobtread.kme.data.Data
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class Game(
    val id: Int,
    val mid: Int,
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

    var gameState: Int = 0;
    var gameSetting: Int = 0;
    val attributes = GameAttributes()
    private var isActive = true

    private val players = ArrayList<PlayerSession>(MAX_PLAYERS)
    private val playersLock = ReentrantReadWriteLock()

    init {
        players.add(host)
    }


    fun isFull(): Boolean = playersLock.read { players.size >= MAX_PLAYERS }
    fun isJoinable(): Boolean = isActive && !isFull()
    fun getActivePlayers(): List<PlayerSession> = playersLock.read {
        players.filter { it.isActive }
            .toMutableList()
    }

    fun addPlayer(player: PlayerSession) = playersLock.write { players.add(player) }

    fun removePlayer(playerId: Int) {
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
    fun createPoolPacket(): Packet =
        unique(
            Component.GAME_MANAGER,
            Command.RETURN_DEDICATED_SERVER_TO_POOL
        ) {
            val hostPlayer = host.getPlayer()
            +struct("GAME") {
                // Game Admins
                list("ADMN", listOf(hostPlayer.playerId))
                map("ATTR", attributes.getMap())
                list("CAP", listOf(0x4, 0x0))
                number("GID", id)
                text("GNAM", hostPlayer.displayName)
                number("GPVH", 0x5a4f2b378b715c6)
                number("GSET", 0x11f)
                number("GSTA", 0x1)
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
                    number("DBPS", 0x0)
                    number("NATT", Data.NAT_TYPE)
                    number("UBPS", 0x0)
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
            list("PROS", listOf(
                struct {
                    blob("BLOB")
                    number("EXID", 0x0)
                    number("GID", id)
                    number("LOC", 0x64654445)
                    text("NAME", hostPlayer.displayName)
                    number("PID", hostPlayer.playerId)
                    +host.createAddrUnion("PNET")
                    number("SID", 0x0)
                    number("SLOT", 0x0)
                    number("STAT", 0x4)
                    number("TIDX", 0xffff)
                    number("TIME", 0x4fd4ce4f6a036)
                    tripple("UGID", 0x0, 0x0, 0x0)
                    number("UID", hostPlayer.playerId)
                }
            ))
            union("REAS", struct("VALU") {
                number("DCTX", 0x0)
            })
        }

}