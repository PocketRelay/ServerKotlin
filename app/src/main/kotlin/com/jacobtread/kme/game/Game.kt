package com.jacobtread.kme.game

import com.jacobtread.kme.blaze.Commands
import com.jacobtread.kme.blaze.Components
import com.jacobtread.kme.blaze.group
import com.jacobtread.kme.blaze.packet.Packet
import com.jacobtread.kme.blaze.tdf.GroupTdf
import com.jacobtread.kme.blaze.tdf.ListTdf
import com.jacobtread.kme.blaze.tdf.Tdf
import com.jacobtread.kme.blaze.unique
import com.jacobtread.kme.utils.logging.Logger
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

    private val activePlayers: List<PlayerSession>
        get() = playersLock.read {
            players.filter { it.isActive }
                .toList()
        }

    init {
        players.add(host)
    }

    fun isInActive(): Boolean = !isActive || !host.isActive

    fun isFull(): Boolean = playersLock.read { players.size >= MAX_PLAYERS }
    fun isJoinable(): Boolean = isActive && !isFull()


    fun join(player: PlayerSession) = playersLock.write {
        players.add(player)
        player.game = this
        sendHostPlayerJoin(player)

        activePlayers.forEach {
            if (it.sessionId != player.sessionId) {
                player.pushPlayerUpdate(it)
            }
        }

        player.push(createPoolPacket(player))
        player.push(player.createSetSession())
    }

    fun getSlotIndex(player: PlayerSession): Int = playersLock.read { players.indexOf(player) }

    private fun sendHostPlayerJoin(session: PlayerSession) {
        host.pushPlayerUpdate(session)
        host.push(unique(Components.GAME_MANAGER, Commands.JOIN_GAME_BY_GROUP) {
            number("GID", id)
            +session.createPlayerDataGroup(getSlotIndex(session))
        })
        host.push(session.createSetSession())
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
            players.forEach {
                if (it != host) {
                    val userPacket = unique(Components.USER_SESSIONS, Commands.FETCH_EXTENDED_DATA) { number("BUID", it.playerId) }
                    it.push(hostPacket)
                    host.push(userPacket)
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
                true
            }
        }
    }

    fun broadcastAttributeUpdate() {
        playersLock.read {
            val packet = createNotifyPacket()
            players.forEach { it.push(packet) }
        }
    }

    fun createNotifySetup(): Packet =
        unique(
            Components.GAME_MANAGER,
            Commands.NOTIFY_GAME_SETUP
        ) {
            +createGameGroup()
            +createPlayersList()
            optional("REAS", group("VALU") {
                number("DCTX", 0x0)
            })
        }

    private fun createNotifyPacket(): Packet =
        unique(
            Components.GAME_MANAGER,
            Commands.NOTIFY_GAME_UPDATED
        ) {
            map("ATTR", getAttributes())
            number("GID", id)
        }

    private fun createGameGroup(): GroupTdf {
        return group("GAME") {
            val hostPlayer = host.playerEntity
            val hostId = hostPlayer.playerId
            val playerIds = players.map { it.playerId.toULong() }

            // Game Admins
            list("ADMN", playerIds)
            map("ATTR", getAttributes())
            list("CAP", listOf(0x4, 0x0))
            number("GID", id)
            text("GNAM", hostPlayer.displayName)
            number("GPVH", 0x5a4f2b378b715c6)
            number("GSET", gameSetting)
            number("GSID", 0x4000000a76b645)
            number("GSTA", gameState)
            text("GTYP", "")
            // Host network information
            list("HNET", listOf(
                group(start2 = true) {
                    +host.extNetData.createGroup("EXIP")
                    +host.intNetData.createGroup("INIP")
                }
            ))
            number("HSES", host.playerId)
            number("IGNO", 0x0)
            number("MCAP", 0x4)
            +group("NQOS") {
                val otherNetData = host.otherNetData
                number("DBPS", otherNetData.dbps)
                number("NATT", otherNetData.natt)
                number("UBPS", otherNetData.ubps)
            }
            number("NRES", 0x0)
            number("NTOP", 0x0)
            text("PGID", "")
            blob("PGSR")
            +group("PHST") {
                number("HPID", hostId)
                number("HSLT", 0x0)
            }
            number("PRES", 0x1)
            text("PSAS", "")
            number("QCAP", 0x0)
            number("SEED", 0x4cbc8585) // Seed? Could be used for game randomness?
            number("TCAP", 0x0)
            +group("THST") {
                number("HPID", hostId)
                number("HSLT", 0x0)
            }
            text("UUID", "286a2373-3e6e-46b9-8294-3ef05e479503")
            number("VOIP", 0x2)
            text("VSTR", "ME3-295976325-179181965240128") // Mass effect version string
            blob("XNNC")
            blob("XSES")
        }
    }

    private fun createPlayersList(): ListTdf {
        return ListTdf("PROS", Tdf.GROUP, players.mapIndexed { index, playerSession ->
            playerSession.createPlayerDataGroup(index)
        })
    }

    fun createPoolPacket(forSession: PlayerSession): Packet =
        unique(
            Components.GAME_MANAGER,
            Commands.RETURN_DEDICATED_SERVER_TO_POOL
        ) {
            +createGameGroup()
            +createPlayersList()
            optional("REAS", 0x3u, group("VALU") {
                number("FIT", 0x3f7a)
                number("MAXF", 0x5460)
                number("MSID", mid)
                number("RSLT", 0x2)
                number("USID", forSession.playerId)
            })
        }

    fun getAttributes(): Map<String, String> = attributesLock.read { attributes }

    fun setAttributes(map: Map<String, String>) {
        attributesLock.write { attributes.putAll(map) }
    }

}