package com.jacobtread.kme.game

import com.jacobtread.kme.blaze.*
import com.jacobtread.kme.data.Data
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class Game(
    val id: ULong,
    val mid: ULong,
    var host: PlayerSession,
) {

    companion object {
        const val MAX_PLAYERS = 4
        const val MIN_ID = 0x5DC695uL
        const val MIN_MID = 0x1129DA20uL
    }

    var gameState: Int = 0x1
    var gameSetting: Int = 0x11f

    private val attributesLock = ReentrantReadWriteLock()
    private val attributes = HashMap<String, String>()

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
        player.game = this
        sendHostPlayerJoin(player)
    }

    private fun sendHostPlayerJoin(session: PlayerSession) {
        val player = session.player
        val sessionDetails = session.createSessionDetails()
        host.send(
            sessionDetails,
            unique(Components.GAME_MANAGER, Commands.JOIN_GAME_BY_GROUP) {
                number("GID", id)
                +group("PDAT") {
                    blob("BLOB")
                    number("EXID", 0x0)
                    number("GID", id)
                    number("LOC", 0x64654445)
                    text("NAME", player.displayName)
                    number("PID", player.playerId)
                    +session.createAddrOptional("PNET")
                    number("SID", players.size)
                    number("SLOT", 0x0)
                    number("STAT", 0x2)
                    number("TIDX", 0xffff)
                    number("TIME", 0x0)
                    tripple("UGID", 0x0, 0x0, 0x0)
                    number("UID", player.playerId)
                }
            },
            session.createSetSession()
        )
    }

    fun removePlayer(player: PlayerSession) {
        playersLock.write { players.remove(player) }
    }

    fun removePlayer(playerId: Int) {
        playersLock.read {
            val index = players.indexOfFirst { it.playerId == playerId }
            if (index != -1) {
                val player: PlayerSession = players[index]
                player.game = null
                player.matchmaking = false
                playersLock.write {
                    players.removeAt(index)
                }
            }
            if (playerId == host.playerId) {
                if (players.isEmpty()) {
                    return stop()
                } else {
                    val first = players.firstOrNull()
                    if (first != null) {
                        host = first
                    } else {
                        return stop()
                    }
                }
            }
            val hostPacket = unique(Components.USER_SESSIONS, Commands.FETCH_EXTENDED_DATA) { number("BUID", host.playerId) }
            val hostContent = hostPacket.contentBuffer
            hostContent.retain(players.size - 1)
            players.forEach {
                if (it != host) {
                    val userPacket = unique(Components.USER_SESSIONS, Commands.FETCH_EXTENDED_DATA) { number("BUID", it.playerId) }
                    it.send(hostPacket)
                    host.send(userPacket)
                }
            }

        }
    }

    private fun stop() {
        GameManager.releaseGame(this)
        isActive = false
        playersLock.write {
            players.removeIf {
                it.game = null
                it.matchmaking = false
                true
            }
        }
    }

    fun broadcastAttributeUpdate() {
        playersLock.read {
            val packet = createNotifyPacket()
            players.forEach { it.send(packet) }
        }
    }

    fun createNotifyPacket(): Packet =
        unique(
            Components.GAME_MANAGER,
            Commands.NOTIFY_GAME_UPDATED
        ) {
            map("ATTR", getAttributes())
            number("GID", id)
        }

    fun createPoolPacket(init: Boolean): Packet =
        unique(
            Components.GAME_MANAGER,
            Commands.RETURN_DEDICATED_SERVER_TO_POOL
        ) {
            val hostPlayer = host.player
            +group("GAME") {
                // Game Admins
                list("ADMN", listOf(hostPlayer.playerId))
                map("ATTR", getAttributes())
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
                    group(start2 = true) {
                        +host.extNetData.createGroup("EXIP")
                        +host.intNetData.createGroup("INIP")
                    }
                ))
                number("HSES", 0x112888c1)
                number("IGNO", 0x0)
                number("MCAP", 0x4)
                +group("NQOS") {
                    number("DBPS", if (init) 0x0 else 0x5b8d800)
                    number("NATT", Data.NAT_TYPE)
                    number("UBPS", if (init) 0x0 else 0x4016400)
                }
                number("NRES", 0x0)
                number("NTOP", 0x0)
                text("PGID", "")
                blob("PGSR")
                +group("PHST") {
                    number("HPID", hostPlayer.playerId)
                    number("HSLT", 0x0)
                }
                number("PRES", 0x1)
                text("PSAS", "")
                number("QCAP", 0x0)
                number("SEED", 0x2cf2048f)
                number("TCAP", 0x0)
                +group("THST") {
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
                val player = playerSession.player
                group {
                    blob("BLOB")
                    number("EXID", 0x0)
                    number("GID", this@Game.id) // Game ID
                    number("LOC", 0x64654445) // Location
                    text("NAME", player.displayName) // Player name
                    number("PID", player.playerId) // Player id
                    +host.createAddrOptional("PNET") // Player net info
                    number("SID", index) // Slot ID
                    number("SLOT", 0x0)
                    number("STAT", if (host.playerId == player.playerId) 0x2 else 0x4)
                    number("TIDX", 0xffff)
                    number("TIME", 0x4fd4ce4f6a036)
                    tripple("UGID", 0x0, 0x0, 0x0)
                    number("UID", player.playerId)
                }
            }

            list("PROS", pros)
            if (init) {
                optional("REAS", group("VALU") {
                    number("DCTX", 0x0)
                })
            } else {
                optional("REAS", 0x3, group("VALU") {
                    number("FIT", 0x53fc)
                    number("MAXF", 0x5460)
                    number("MSID", mid)
                    number("RSLT", 0x2)
                    number("USID", hostPlayer.playerId)
                })
            }
        }

    fun getAttributes(): Map<String, String> = attributesLock.read { attributes }

    fun setAttributes(map: Map<String, String>) {
        attributesLock.write { attributes.putAll(map) }
    }

}