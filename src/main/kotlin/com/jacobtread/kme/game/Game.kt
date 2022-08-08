package com.jacobtread.kme.game

import com.jacobtread.blaze.NotAuthenticatedException
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

    private var gameState: Int = 0x1
    private var gameSetting: Int = 0x11f

    private val attributesLock = ReentrantReadWriteLock()
    private val attributes = HashMap<String, String>(initialAttributes)

    private val playersLock = ReentrantReadWriteLock()
    private var playersCount = 1
    private val players = arrayOfNulls<Session>(MAX_PLAYERS)

    private val isFull: Boolean get() = playersCount == MAX_PLAYERS
    val isJoinable: Boolean get() = !isFull

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
        val sessionPacket = player.createSetSessionPacket()
        host.push(notify(Components.GAME_MANAGER, Commands.NOTIFY_PLAYER_JOINING) {
            number("GID", id)
            +player.createPlayerDataGroup()
        })
        forEachPlayer {
            player.updateSessionFor(it)
            it.updateSessionFor(player)
        }
        pushAll(sessionPacket)
        player.push(createGameSetupPacket(player))
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
        val player = playersLock.read {
            players.firstOrNull {
                if (it == null) return@firstOrNull false
                val playerEntity = it.player
                playerEntity != null && playerEntity.playerId == playerId
            }
        }
        if (player != null) {
            removePlayer(player)
        }
    }

    fun removePlayer(session: Session) {
        Logger.logIfDebug {
            val player = session.player
            if (player != null) {
                "Removing player ${player.displayName} (${player.playerId}) from game $id"
            } else {
                "Removing session ${session.sessionId} from game $id"
            }
        }

        pushAll(
            notify(
                Components.GAME_MANAGER,
                Commands.NOTIFY_PLAYER_REMOVED
            ) {
                number("CNTX", 0x0)
                number("GID", id)
                number("PID", session.playerIdSafe)
                number("REAS", 0x6) // Possible remove reason? Investigate further
            }
        )

        playersLock.write { players[session.gameSlot] = null }

        Logger.logIfDebug {
            val player = session.player
            if (player != null) {
                "Removed player ${player.displayName} (${player.playerId}) from game $id"
            } else {
                "Removed session ${session.sessionId} from game $id"
            }
        }

        updatePlayerSlots()
        updatePlayersList()

        val wasHost = session.gameSlot == 0
        if (playersCount > 0 && wasHost) {
            val newHost = getHostOrNull()
            if (newHost != null) {
                migrateHost(session, newHost)
            }
        }

        session.clearGame()
    }

    private fun migrateHost(lastHost: Session, newHost: Session) {
        Logger.logIfDebug {
            val lastPlayer = lastHost.player
            val newPlayer = newHost.player
            if (lastPlayer != null && newPlayer != null) {
                "Migrating host from ${lastPlayer.displayName} (${lastPlayer.playerId})" +
                        " to ${newPlayer.displayName} (${newPlayer.playerId}) from game $id"
            } else {
                "Migratin host for game $id"
            }
        }

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

        val packet = lastHost.createSetSessionPacket()
        pushAll(packet)
    }

    private fun updatePlayersList() {
        if (playersCount < 1) return
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
        remove(this)
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

        playerSession.setPlayerState(4)

        val a = notify(Components.GAME_MANAGER, Commands.NOTIFY_PLAYER_JOIN_COMPLETED) {
            number("GID", id)
            number("PID", playerId)
        }
        val b = notify(Components.GAME_MANAGER, Commands.NOTIFY_ADMIN_LIST_CHANGE) {
            number("ALST", playerId)
            number("GID", id)
            number("OPER", 0) // 0 = add 1 = remove
            number("UID", host.playerIdSafe)
        }
        pushAll(a, b)
    }


    /**
     * Push a packet to the host only.
     *
     * @param packet The packet to push
     */
    fun pushHost(packet: Packet) {
        val host = getHostOrNull()
        host?.push(packet)
    }

    /**
     * Pushes the provided packet to all the
     * connected players in this game.
     *
     * @param packet The packet to push
     */
    fun pushAll(packet: Packet) {
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
                number("GPVH", GPVH)
                number("GSET", gameSetting)
                number("GSID", GSID)
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
                    // 16250, 16675, 21050, 21500, 21600
                    number("FIT", 0x3f7a) // 0x53fc (4), 0x3f7a (3), 0x5460 (2), 0x4123 (2), 0x523a (2)
                    number("MAXF", 0x5460) /// 0x5460
                    number("MSID", matchmakingSesion.matchmakingId) // Matchmaking session id
                    number("RSLT", matchmakingSesion.gameSlot)
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
    fun matchesRules(ruleSet: MatchRuleSet): Boolean {
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


    companion object {
        const val MAX_PLAYERS = 4
        const val GPVH = 0x5a4f2b378b715c6
        const val GSID = 0x4000000a76b645

        private val gamesLock = ReentrantReadWriteLock()
        private val games = HashMap<ULong, Game>()
        private var gameId = 1uL

        /**
         * Creates a new game with the provided host and
         * initial attributes. Aquires the games lock before
         * creating the game and increments the game id
         *
         * @param host The host for the game
         * @param attributes The initial game attributes
         * @return The created game
         */
        fun create(host: Session, attributes: Map<String, String>): Game {
            val hostPlayer = host.player ?: throw NotAuthenticatedException()
            Logger.info("Created new game ($gameId) hosted by ${hostPlayer.displayName} (${hostPlayer.playerId})")
            val game = Game(gameId, host, attributes)
            gamesLock.write {
                games[gameId] = game
                gameId++
            }
            return game
        }

        /**
         * Retrieves a game by its unique ID.
         *
         * @param id The id of the game
         * @return The found game or null if none match
         */
        fun getById(id: ULong): Game? {
            return gamesLock.read { games[id] }
        }

        /**
         * Searches through the map of games trying to find
         * a game which matches the provided ruleset
         *
         * @param ruleSet The rule set to match against
         * @return The found game or null if none match
         */
        fun getByRules(ruleSet: MatchRuleSet): Game? {
            return gamesLock.read {
                games.values.firstOrNull { it.matchesRules(ruleSet) }
            }
        }

        /**
         * Removes the provided game from the map of
         * games by aquiring the games lock first.
         *
         * @param game The game to remove
         */
        private fun remove(game: Game) {
            Logger.info("Releasing game back to pool (${game.id})")
            gamesLock.write { games.remove(game.id) }
        }
    }
}
