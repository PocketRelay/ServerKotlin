package com.jacobtread.kme.game

import com.jacobtread.blaze.group
import com.jacobtread.blaze.notify
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.tdf.Tdf
import com.jacobtread.blaze.tdf.types.GroupTdf
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.exceptions.GameException
import com.jacobtread.kme.game.match.MatchRuleSet
import com.jacobtread.kme.utils.logging.Logger
import java.util.Map.copyOf
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Represents a game on the server. Manages the players and any
 * other game related logic such as state, settings and attributes
 *
 * @property id The unique identifier for this game
 * @constructor Creates a new game instance.
 *
 * @param host The session which started hosting this game
 * @param initialAttributes The initial attributes provided in game creation
 */
class Game(
    val id: ULong,
    host: Session,
    initialAttributes: Map<String, String>,
) {

    companion object {
        const val MAX_PLAYERS = 4
    }

    private var gameState: Int = 0x1
    private var gameSetting: Int = 0x11f

    private val attributesLock = ReentrantReadWriteLock()
    private val attributes = HashMap<String, String>(initialAttributes)

    var isActive = true


    private val playersLock = ReentrantReadWriteLock()
    private var playersCount = 1
    private val players = arrayOfNulls<Session>(MAX_PLAYERS)

    private val isFull: Boolean get() = playersCount == MAX_PLAYERS
    val isJoinable: Boolean get() = isActive && !isFull

    init {
        players[0] = host
        host.setGame(this, 0)
    }

    fun setupHost() {
        val host = getHost()
        host.pushAll(
            createGameSetupPacket(null),
            host.createSetSessionPacket()
        )
    }

    fun join(player: Session) = playersLock.write {
        if (playersCount >= MAX_PLAYERS) throw GameException.GameFullException()
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
        forEachPlayer {
            player.updateSessionFor(it)
            it.updateSessionFor(player)
        }
        player.pushAll(
            createGameSetupPacket(player),
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

    fun removeAtIndex(index: Int) {
        val lastHost = getHostOrNull()
        playersLock.read {
            Logger.logIfDebug { "Removing player at id $index" }
            val removedPlayer = players[index]
            if (removedPlayer != null) {
                val removeNotifiation = notify(
                    Components.GAME_MANAGER,
                    Commands.NOTIFY_PLAYER_REMOVED
                ) {
                    number("CNTX", 0x0)
                    number("GID", id)
                    number("PID", removedPlayer.playerIdSafe)
                    number("REAS", 0x6) // Possible remove reason? Investigate further
                }
                players.forEach { it?.push(removeNotifiation) }
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
                    migrateHost(lastHost, host)
                }
            }
        }
    }

    private fun migrateHost(lastHost: Session?, newHost: Session) {
        pushAll(notify(
            Components.GAME_MANAGER,
            Commands.NOTIFY_HOST_MIGRATION_START
        ) {
            number("GID", id)
            number("HOST", newHost.playerIdSafe)
            number("PMIG", 0x2)
            number("SLOT", newHost.gameSlot)
        })

        setGameState(0x82)

        pushAll(notify(
            Components.GAME_MANAGER,
            Commands.NOTIFY_HOST_MIGRATION_FINISHED
        ) {
            number("GID", id)
        })

        if (lastHost != null) {
            val packet = lastHost.createSetSessionPacket()
            pushAll(packet)
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

    fun updateMeshConnection(playerSession: Session) {
        val playerEntity = playerSession.player ?: return
        val playerId = playerEntity.playerId
        val host = getHost()
        val a = notify(Components.GAME_MANAGER, Commands.NOTIFY_GAME_PLAYER_STATE_CHANGE) {
            number("GID", id)
            number("PID", playerId)
            number("STAT", 4)
        }
        val b = notify(Components.GAME_MANAGER, Commands.NOTIFY_PLAYER_JOIN_COMPLETED) {
            number("GID", id)
            number("PID", playerId)
        }
        val c = notify(Components.GAME_MANAGER, Commands.NOTIFY_ADMIN_LIST_CHANGE) {
            number("ALST", playerId)
            number("GID", id)
            number("OPER", 0) // 0 = add 1 = remove
            number("UID", host.playerIdSafe)
        }
        pushAll(a, b, c)
    }

    /**
     * Pushes the provided packet to all the
     * connected players in this game.
     *
     * @param packet The packet to push
     */
    private fun pushAll(packet: Packet) {
        forEachPlayer { player -> player.push(packet) }
    }

    /**
     * Pushes all the provided packets to all the
     * connected players in this game.
     *
     * @param packets The packets to push
     */
    private fun pushAll(vararg packets: Packet) {
        forEachPlayer { player -> player.pushAll(*packets) }
    }

    /**
     * Inline function for safely iterating over all the
     * players in the match.
     *
     * (Obtains a read lock before iterating)
     *
     * @param action The action to run on each player
     */
    private inline fun forEachPlayer(action: (player: Session) -> Unit) {
        playersLock.read {
            players.forEach {
                if (it != null) action(it)
            }
        }
    }

    private fun createGameSetupPacket(matchmakingSesion: Session?): Packet {
        return notify(
            Components.GAME_MANAGER,
            Commands.NOTIFY_GAME_SETUP
        ) {
            val host = getHost()
            val hostPlayer = host.player
            check(hostPlayer != null) { "Host player was null couldn't create game group" }
            val hostId = hostPlayer.playerId

            val playerIds = ArrayList<ULong>()
            val playerGroups = ArrayList<GroupTdf>()
            forEachPlayer { player ->
                playerIds.add(player.playerIdSafe.toULong())
                playerGroups.add(player.createPlayerDataGroup())
            }

            +group("GAME") {
                // Game Admins
                list("ADMN", playerIds)
                map("ATTR", getCopyOfAttributes())
                list("CAP", listOf(0x4, 0x0))
                number("GID", id)
                text("GNAM", hostPlayer.displayName)
                number("GPVH", 0x5a4f2b378b715c6)
                number("GSET", gameSetting)
                number("GSID", 0x4000000a76b645)
                number("GSTA", gameState)
                text("GTYP", "")
                // Host network information
                list("HNET", listOf(host.createHNET()))
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
                text("VSTR", "ME3-295976325-179181965240128") // Mass effect version string?
                blob("XNNC")
                blob("XSES")
            }

            list("PROS", Tdf.GROUP, playerGroups)

            if (matchmakingSesion != null) {
                optional("REAS", 0x3u, group("VALU") {
                    number("FIT", 0x3f7a)
                    number("MAXF", 0x5460)
                    number("MSID", matchmakingSesion.matchmakingId) // Matchmaking session id
                    number("RSLT", 0x2)
                    number("USID", matchmakingSesion.playerIdSafe) // Player ID
                })
            } else {
                optional("REAS", group("VALU") {
                    number("DCTX", 0x0)
                })
            }
        }
    }

    /**
     * Retrieves the host player for this game
     * (This is always the first player) or returns
     * null if there are no players in the game.
     *
     * @return The host player session or null if none
     */
    private fun getHostOrNull(): Session? {
        var host = players[0]
        if (host == null) {
            updatePlayerSlots()
            host = players[0]
        }
        return host
    }

    /**
     * Retrieves the host player but without the
     * possibility of being null. If the host is
     * null then a [GameException.StoppedException]
     * is thrown instead
     *
     * @return The host player session
     */
    private fun getHost(): Session = getHostOrNull() ?: throw GameException.StoppedException()


    /**
     * Retrieves the attributes map from within its lock
     * and creates a copy which is then returned.
     *
     * @return The copy of the attributes map.
     */
    private fun getCopyOfAttributes(): Map<String, String> {
        return attributesLock.read { copyOf(attributes) }
    }

    /**
     * Checks if this game matches the provided rule
     * set.
     *
     * @param ruleSet The rule set to check against.
     * @return Whether this game matches.
     */
    fun matchesRules(ruleSet: MatchRuleSet): Boolean{
        return attributesLock.read { ruleSet.validate(attributes) }
    }

    /**
     * Sets the attributes for this game. The changes to the attributes
     * are then send to the connected players
     *
     * @param attributes The map of attribute key value pairs.
     */
    fun setAttributes(attributes: Map<String, String>) {
        attributesLock.write { this.attributes.putAll(attributes) }
        pushAll(
            notify(
                Components.GAME_MANAGER,
                Commands.NOTIFY_GAME_UPDATED
            ) {
                map("ATTR", getCopyOfAttributes())
                number("GID", id)
            }
        )
    }

    /**
     * Handles setting the game state value. When the game state is
     * changed all the players are informed of this change with a
     * [Components.GAME_MANAGER] / [Commands.NOTIFY_GAME_STATE_CHANGE]
     *
     * @param value The new game state value
     */
    fun setGameState(value: Int) {
        gameState = value
        pushAll(
            notify(
                Components.GAME_MANAGER,
                Commands.NOTIFY_GAME_STATE_CHANGE
            ) {
                number("GID", id)
                number("GSTA", value)
            }
        )
    }

    /**
     * Handles setting the game setting value. When the game setting is
     * changed all the players are informed of this change with a
     * [Components.GAME_MANAGER] / [Commands.NOTIFY_GAME_SETTINGS_CHANGE]
     *
     * @param value The new game setting value
     */
    fun setGameSetting(value: Int) {
        gameSetting = value
        pushAll(
            notify(
                Components.GAME_MANAGER,
                Commands.NOTIFY_GAME_SETTINGS_CHANGE
            ) {
                number("ATTR", value)
                number("GID", id)
            }
        )
    }
}
