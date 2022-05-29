package com.jacobtread.kme.game

import com.jacobtread.kme.utils.logging.Logger
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.BiPredicate
import java.util.function.Predicate
import kotlin.concurrent.read
import kotlin.concurrent.write

object GameManager {

    private val gamesLock = ReentrantReadWriteLock()
    private val games = HashMap<Long, Game>()
    private var gameId: Int = 0

    fun createGame(host: PlayerSession): Game = gamesLock.write {
        removeInactive()
        val game = Game(gameId + Game.MIN_ID, gameId + Game.MIN_MID, host)
        Logger.info("Created new game (${game.id}, ${game.mid}) hosted by ${host.player.displayName}")
        games[game.id] = game
        gameId++
        game
    }


    fun tryFindGame(test: (Game) -> Boolean): Game? = gamesLock.read { games.values.firstOrNull(test) }

    private fun removeInactive() {
        val removeKeys = ArrayList<Long>()
        games.forEach { (key, game) ->
            if (game.isInActive()) removeKeys.add(key)
        }
        removeKeys.forEach { games.remove(it) }
    }

    fun getGameById(id: Long): Game? = gamesLock.read { games.values.find { it.id == id } }
    fun releaseGame(game: Game) = gamesLock.write {
        Logger.info("Releasing game back to pool (${game.id}, ${game.mid})")
        games.remove(game.id)
    }

}