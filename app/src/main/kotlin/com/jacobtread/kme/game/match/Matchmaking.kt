package com.jacobtread.kme.game.match

import com.jacobtread.kme.game.Game
import com.jacobtread.kme.game.GameManager
import com.jacobtread.kme.game.PlayerSession
import com.jacobtread.kme.utils.logging.Logger
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Matchmaking Shared object for handling match making for finding games
 * for players that are waiting for games
 *
 * @constructor Create empty Matchmaking
 */
object Matchmaking {

    private const val MATCHMAKING_TIMEOUT = 1000 * 60 // 1 Minute

    private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    /**
     * waitingPlayers A map of the players who are waiting for a match
     * mapped to the rule set that the match must be valid for
     */
    private val waitingPlayers = HashMap<PlayerSession, MatchRuleSet>()

    /**
     * waitingLock A lock for ensuring thread safety for the waiting map across all
     * the netty worker threads
     */
    private val waitingLock = ReentrantReadWriteLock()

    private var matchmakingId: ULong = 2uL

    init {
        scheduledExecutorService.scheduleWithFixedDelay(this::updateMatchmakingQueue, 0, 30, TimeUnit.SECONDS)
    }

    /**
     * onGameCreated Invoked when the GameManager creates a new game. This is here
     * to check if this newly created game matches any of the rule sets for existing
     * waiting players. If it does then the players are added to that game
     *
     * @param game The newly created game
     */
    internal fun onGameCreated(game: Game) {
        val attributes = game.getAttributes()
        waitingLock.read {
            val iterator = waitingPlayers.iterator()
            while (iterator.hasNext()) {
                val (session, ruleSet) = iterator.next()
                if (ruleSet.validate(attributes) && game.isJoinable) {
                    game.join(session)
                    waitingLock.write {
                        iterator.remove()
                    }
                }
            }
        }
    }

    /**
     * getMatchOrQueue Tries to get a match for the provided player session
     * that matches the provided rule set but if none are available the player
     * will be added to the waiting players list
     *
     * @param session The session of the player wanting to matchmake
     * @param ruleSet The rule set for the player
     * @return The game or null if they were added to the waiting list
     */
    fun getMatchOrQueue(session: PlayerSession, ruleSet: MatchRuleSet): Game? {
        if (session.matchmakingId == 1uL) {
            session.matchmakingId = matchmakingId++
        }
        val game = GameManager.tryFindGame { ruleSet.validate(it.getAttributes()) }
        if (game != null) return game
        session.matchmaking = true
        session.startedMatchmaking = System.currentTimeMillis()
        waitingLock.write { waitingPlayers[session] = ruleSet }
        return null
    }

    /**
     * removeFromQueue Removes the player session from the match making
     * queue and the waiting player list
     *
     * @param session The session of the player
     */
    fun removeFromQueue(session: PlayerSession) {
        session.matchmaking = false
        waitingLock.write { waitingPlayers.remove(session) }
    }

    private fun updateMatchmakingQueue() {
        Logger.logIfDebug { "Running scheduled matchmaking queue update..." }
        val currentTime = System.currentTimeMillis()
        waitingLock.read {
            if (waitingPlayers.isEmpty()) {
                matchmakingId = 2uL
            } else {
                val iterator = waitingPlayers.iterator()
                while (iterator.hasNext()) {
                    val (session, _) = iterator.next()
                    val timeElapsed = currentTime - session.startedMatchmaking
                    if (timeElapsed >= MATCHMAKING_TIMEOUT) {
                        Logger.info("Player matchmaking timed out ${session.displayName} (${session.playerId})")
                        session.notifyMatchmakingFailed()
                        waitingLock.write {
                            iterator.remove()
                        }
                    } else {
                        session.pushMatchmakingStatus()
                    }
                }
            }
        }
    }
}