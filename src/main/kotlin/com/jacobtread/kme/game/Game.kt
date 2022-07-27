package com.jacobtread.kme.game

import com.jacobtread.blaze.group
import com.jacobtread.blaze.notify
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.tdf.GroupTdf
import com.jacobtread.blaze.tdf.ListTdf
import com.jacobtread.blaze.tdf.Tdf
import com.jacobtread.kme.data.Commands
import com.jacobtread.kme.data.Components
import com.jacobtread.kme.data.GameStateAttr
import com.jacobtread.kme.exceptions.GameStoppedException
import com.jacobtread.kme.logging.Logger
import com.jacobtread.kme.servers.main.Session
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class Game(
    val id: ULong,
    host: Session,
) {

    companion object {
        const val MAX_PLAYERS = 4
    }

    var gameState: Int = 0x1
    var gameSetting: Int = 0x11f

    private val attributesLock = ReentrantReadWriteLock()
    private val attributes = HashMap<String, String>()

    fun isGameState(stateAttr: GameStateAttr): Boolean = isAttribute(GameStateAttr.GAME_STATE_ATTR, stateAttr.value)

    var isActive = true


    private val playersLock = ReentrantReadWriteLock()
    private var playersCount = 1
    private val players = arrayOfNulls<Session>(MAX_PLAYERS)

    private val activePlayers: List<Session>
        get() = playersLock.read { players.filterNotNull() }

    val isFull: Boolean get() = playersCount == MAX_PLAYERS
    val isJoinable: Boolean get() = isActive && !isFull

    init {
        players[0] = host
    }

    private fun getHostOrNull(): Session? {
        var host = players[0]
        if (host == null) {
            updatePlayerSlots()
            host = players[0]
        }
        return host
    }

    fun getHost(): Session = getHostOrNull() ?: throw GameStoppedException()


    fun join(player: Session) = playersLock.write {
        // TODO: Unsafe could overflow if too many players. implement limit

        player.resetMatchmakingState()

        val gameSlot = playersCount++

        player.setGame(this, gameSlot)

        players[gameSlot] = player

        val host = getHost()

        host.updateSessionFor(player)
        host.pushAll(
            notify(Components.GAME_MANAGER, Commands.NOTIFY_PLAYER_JOINING) {
                number("GID", id)
                +player.createPlayerDataGroup()
            },
            player.createSetSessionPacket()
        )
        activePlayers.forEach {
            player.updateSessionFor(it)
        }

        player.pushAll(
            createMatchmakingResult(player),
            player.createSetSessionPacket()
        )
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


    fun removePlayer(player: Session) {
        removeAtIndex(player.gameSlot)
    }


    fun removePlayerById(playerId: Int) {
        val playerIndex: Int = playersLock.read {
            players.indexOfFirst {
                if (it == null) return@indexOfFirst false
                val playerEntity = it.player
                playerEntity != null && playerEntity.playerId == playerId
            }
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
                players.forEach { it?.push(createRemoveNotification(removedPlayer)) }
                playersLock.write { players[index] = null }
                removedPlayer.clearGame()

                Logger.logIfDebug {
                    val playerEntity = removedPlayer.player ?: return@logIfDebug ""
                    "Removed player in slot $index ${playerEntity.displayName} (${playerEntity.playerId})"
                }
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
                    Logger.logIfDebug {
                        val playerEntity = host.player ?: return@logIfDebug ""
                        "Migrating host for $id to ${playerEntity.displayName} (${playerEntity.playerId})"
                    }
                    migrateHost(host)
                }
            }
        }
    }

    private fun migrateHost(newHost: Session) {
        val startPacket = notify(
            Components.GAME_MANAGER,
            Commands.NOTIFY_HOST_MIGRATION_START
        ) {
            number("GID", id)
            number("HOST", newHost.playerIdSafe)
            number("PMIG", 0x2)
            number("SLOT", newHost.gameSlot)
        }
        pushAll(startPacket)

        pushAll(createNotifySetup())

        val finishPacket = notify(
            Components.GAME_MANAGER,
            Commands.NOTIFY_HOST_MIGRATION_FINISHED
        ) {
            number("GID", id)
        }
        pushAll(finishPacket)


    }


    private fun createRemoveNotification(player: Session): Packet {
        return notify(
            Components.GAME_MANAGER,
            Commands.NOTIFY_PLAYER_REMOVED
        ) {
            number("CNTX", 0x0)
            number("GID", id)
            number("PID", player.playerIdSafe)
            number("REAS", 0x6) // Possible remove reason? Investigate further
        }
    }

    private fun updatePlayersList() {
        val host = getHost()
        val hostPacket = notify(Components.USER_SESSIONS, Commands.FETCH_EXTENDED_DATA) { number("BUID", host.playerIdSafe) }
        for (i in 1 until MAX_PLAYERS) {
            val player = players[i] ?: continue
            val playerPacket = notify(Components.USER_SESSIONS, Commands.FETCH_EXTENDED_DATA) { number("BUID", player.playerIdSafe) }

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
                player.clearGame()
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
        notify(
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
        notify(
            Components.GAME_MANAGER,
            Commands.NOTIFY_GAME_UPDATED
        ) {
            map("ATTR", getAttributes())
            number("GID", id)
        }

    private fun createGameGroup(): GroupTdf {
        val host = getHost()
        val hostPlayer = host.player
        check(hostPlayer != null) { "Host player was null couldn't create game group" }
        val hostId = hostPlayer.playerId

        return group("GAME") {
            val playerIds = ArrayList<ULong>()
            players.forEach {
                if (it != null) {
                    playerIds.add(it.playerIdSafe.toULong())
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
                    +host.createExternalNetGroup()
                    +host.createInternalNetGroup()
                }
            ))
            number("HSES", host.playerIdSafe)
            number("IGNO", 0x0)
            number("MCAP", 0x4)
            +group("NQOS") {
                number("DBPS", host.dbps)
                number("NATT", host.nattType)
                number("UBPS", host.ubps)
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

    private fun createMatchmakingResult(forSession: Session): Packet =
        notify(
            Components.GAME_MANAGER,
            Commands.NOTIFY_GAME_SETUP
        ) {
            +createGameGroup()
            +createPlayersList()
            // Matchmaking result
            optional("REAS", 0x3u, group("VALU") {
                number("FIT", 0x3f7a)
                number("MAXF", 0x5460)
                number("MSID", forSession.matchmakingId)
                number("RSLT", 0x2)
                number("USID", forSession.playerIdSafe)
            })
        }


    private fun getAttribute(key: String): String? = attributesLock.read { attributes[key] }

    private fun isAttribute(key: String, value: String): Boolean = getAttribute(key) == value

    fun getAttributes(): Map<String, String> = attributesLock.read { attributes }

    fun setAttributes(map: Map<String, String>) {
        attributesLock.write { attributes.putAll(map) }
    }

}