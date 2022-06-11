package com.jacobtread.kme.game.match

import com.jacobtread.kme.game.Game
import com.jacobtread.kme.game.GameManager
import com.jacobtread.kme.game.PlayerSession
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object Matchmaking {

    private val waitingPlayers = HashMap<PlayerSession, MatchRuleSet>()
    private val waitingLock = ReentrantReadWriteLock()

    fun onNewGameCreated(game: Game) {
        val attributes = game.getAttributes()
        waitingLock.read {
            val iterator = waitingPlayers.iterator()
            while (iterator.hasNext()) {
                val (session, ruleSet) = iterator.next()
                if (ruleSet.validate(attributes)) {
                    game.join(session)
                    session.send(game.host.createSessionDetails())
                    game.getActivePlayers().forEach {
                        session.send(it.createSessionDetails())
                    }
                    session.send(game.createPoolPacket(false))
                    waitingLock.write {
                        session.matchmaking = false
                        iterator.remove()
                    }
                }
            }
        }
    }

    fun getMatchOrQueue(session: PlayerSession, ruleSet: MatchRuleSet): Game? {
        val game = GameManager.tryFindGame { ruleSet.validate(it.getAttributes()) }
        if (game != null) return game
        session.matchmaking = true
        waitingLock.write { waitingPlayers[session] = ruleSet }
        return null
    }

    fun removeFromQueue(session: PlayerSession) {
        session.matchmaking = false
        waitingLock.write { waitingPlayers.remove(session) }
    }
}