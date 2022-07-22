package com.jacobtread.kme.game

import com.jacobtread.kme.blaze.Commands
import com.jacobtread.kme.blaze.Components
import com.jacobtread.kme.blaze.group
import com.jacobtread.kme.blaze.packet.Packet
import com.jacobtread.kme.blaze.tdf.GroupTdf
import com.jacobtread.kme.blaze.tdf.ListTdf
import com.jacobtread.kme.blaze.tdf.Tdf
import com.jacobtread.kme.blaze.unique
import com.jacobtread.kme.data.GameStateAttr
import com.jacobtread.kme.exceptions.GameStoppedException
import com.jacobtread.kme.utils.logging.Logger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class Game(
    val id: ULong,
    host: PlayerSession,
) {

    companion object {
        const val MAX_PLAYERS = 4
    }

    var gameState: Int = 0x1
    var gameSetting: Int = 0x11f

    private val attributesLock = ReentrantReadWriteLock()
    private val attributes = HashMap<String, String>()

    fun isGameState(stateAttr: GameStateAttr): Boolean = isAttribute("ME3gameState", stateAttr.value)

    var isActive = true


    private val playersLock = ReentrantReadWriteLock()
    private var playersCount = 1
    private val players = arrayOfNulls<PlayerSession>(MAX_PLAYERS)

    private val activePlayers: List<PlayerSession>
        get() = playersLock.read { players.filterNotNull() }

    val isFull: Boolean get() = playersCount == MAX_PLAYERS
    val isJoinable: Boolean get() = isActive && !isFull

    init {
        players[0] = host
    }

    private fun getHostOrNull(): PlayerSession? {
        var host = players[0]
        if (host == null) {
            updatePlayerSlots()
            host = players[0]
        }
        return host
    }

    fun getHost(): PlayerSession = getHostOrNull() ?: throw GameStoppedException()


    fun join(player: PlayerSession) = playersLock.write {
        // TODO: Unsafe could overflow if too many players. implement limit

        player.gameSlot = playersCount++
        players[player.gameSlot] = player
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

    private fun sendHostPlayerJoin(session: PlayerSession) {

        val host = getHost()

        host.pushPlayerUpdate(session)
        host.push(unique(Components.GAME_MANAGER, Commands.JOIN_GAME_BY_GROUP) {
            number("GID", id)
            +session.createPlayerDataGroup()
        })
        host.push(session.createSetSession())
    }


    private fun updatePlayerSlots() {
        playersCount = 0
        playersLock.write {
            var i = 0
            var backShift = 0
            while (i < MAX_PLAYERS) {
                val playerAt = players[i]
                if (playerAt == null) {
                    backShift++
                } else {
                    playersCount++
                    if (backShift > 0) {
                        val newSlot = i - backShift
                        players[newSlot] = playerAt
                        players[i] = null
                        playerAt.gameSlot = newSlot
                    }
                }
                i++
            }
        }
        if (playersCount < 1) {
            stop()
        }
    }


    fun removePlayer(player: PlayerSession) {
        removeAtIndex(player.gameSlot)
    }


    fun removePlayerById(playerId: Int) {
        val playerIndex: Int = playersLock.read {
            players.indexOfFirst { it != null && it.playerId == playerId }
        }
        if (playerIndex != -1) {
            removeAtIndex(playerIndex)
        }
    }

    private fun removeAtIndex(index: Int) {
        playersLock.read {
            Logger.logIfDebug { "Removing player at id $index" }
            val removedPlayer = players[index]
            if (removedPlayer != null) {
                playersLock.write { players[index] = null }

                removedPlayer.game = null
                removedPlayer.gameSlot = 0

                players.forEach { it?.push(createRemoveNotification(removedPlayer)) }
                Logger.logIfDebug { "Removed player in slot $index ${removedPlayer.displayName} (${removedPlayer.playerId})" }
            } else {
                Logger.logIfDebug { "Tried to remove player that doesn't exist" }
            }
        }
        updatePlayerSlots()
        if (playersCount > 0) {
            updatePlayersList()

            if (index == 0) {
                // TODO: Host migration working state unknown
                val host = getHostOrNull()
                if (host != null) {
                    Logger.logIfDebug { "Migrating host for $id to ${host.displayName} (${host.playerId})" }
                    migrateHost(host)
                }
            }
        }
    }

    private fun migrateHost(newHost: PlayerSession) {
        val startPacket = unique(
            Components.GAME_MANAGER,
            Commands.NOTIFY_HOST_MIGRATION_START
        ) {
            number("GID", id)
            number("HOST", newHost.playerId)
            number("PMIG", 0x2)
            number("SLOT", newHost.gameSlot)
        }
        pushAll(startPacket)

        pushAll(createNotifySetup())

        val finishPacket = unique(
            Components.GAME_MANAGER,
            Commands.NOTIFY_HOST_MIGRATION_FINISHED
        ) {
            number("GID", id)
        }
        pushAll(finishPacket)


    }


    private fun createRemoveNotification(player: PlayerSession): Packet {
        return unique(
            Components.GAME_MANAGER,
            Commands.NOTIFY_PLAYER_REMOVED
        ) {
            number("CNTX", 0x0)
            number("GID", id)
            number("PID", player.playerId)
            number("REAS", 0x6) // Possible remove reason? Investigate further
        }
    }

    private fun updatePlayersList() {
        val host = getHost()
        val hostPacket = unique(Components.USER_SESSIONS, Commands.FETCH_EXTENDED_DATA) { number("BUID", host.playerId) }
        for (i in 1 until MAX_PLAYERS) {
            val player = players[i] ?: continue
            val playerPacket = unique(Components.USER_SESSIONS, Commands.FETCH_EXTENDED_DATA) { number("BUID", player.playerId) }

            player.push(hostPacket)
            host.push(playerPacket)
        }
    }

    private fun stop() {
        GameManager.releaseGame(this)
        isActive = false
        playersLock.write {
            for (i in 0 until MAX_PLAYERS) {
                val player = players[i] ?: continue
                player.game = null
                player.gameSlot = 0
                players[i] = null
            }
        }
    }

    fun broadcastAttributeUpdate() {
        playersLock.read {
            val packet = createNotifyPacket()
            players.forEach { it?.push(packet) }
        }
    }

    private fun pushAll(packet: Packet) {
        playersLock.read { players.forEach { it?.push(packet) } }
    }

    private fun pushAllExcludingHost(packet: Packet) {
        playersLock.read {
            for (i in 1 until MAX_PLAYERS) {
                val player = players[i] ?: continue
                player.push(packet)
            }
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
        val host = getHost()
        val hostPlayer = host.playerEntity
        val hostId = hostPlayer.playerId

        return group("GAME") {
            val playerIds = ArrayList<ULong>()
            players.forEach {
                if (it != null) {
                    playerIds.add(it.playerId.toULong())
                }
            }

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
        val playersList = ArrayList<GroupTdf>()
        players.forEach {
            if (it != null) {
                playersList.add(it.createPlayerDataGroup())
            }
        }
        return ListTdf("PROS", Tdf.GROUP, playersList)
    }

    private fun createPoolPacket(forSession: PlayerSession): Packet =
        unique(
            Components.GAME_MANAGER,
            Commands.RETURN_DEDICATED_SERVER_TO_POOL
        ) {
            +createGameGroup()
            +createPlayersList()
            optional("REAS", 0x3u, group("VALU") {
                number("FIT", 0x3f7a)
                number("MAXF", 0x5460)
                number("MSID", forSession.matchmakingId)
                number("RSLT", 0x2)
                number("USID", forSession.playerId)
            })
        }


    private fun getAttribute(key: String): String? = attributesLock.read { attributes[key] }

    private fun isAttribute(key: String, value: String): Boolean = getAttribute(key) == value

    fun getAttributes(): Map<String, String> = attributesLock.read { attributes }

    fun setAttributes(map: Map<String, String>) {
        attributesLock.write { attributes.putAll(map) }
    }

}