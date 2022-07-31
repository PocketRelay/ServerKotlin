package com.jacobtread.kme.game

import com.jacobtread.blaze.NotAuthenticatedException
import com.jacobtread.kme.servers.main.Session
import com.jacobtread.kme.utils.logging.Logger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object GameManager {

    private val gamesLock = ReentrantReadWriteLock()
    private val games = HashMap<ULong, Game>()
    private var gameId: ULong = 1uL

    fun createGame(host: Session): Game = gamesLock.write {
        val hostPlayer = host.player ?: throw NotAuthenticatedException()
        removeInactive()
        val game = Game(gameId, host)

        Logger.info("Created new game (${game.id}) hosted by ${hostPlayer.displayName}")
        games[game.id] = game
        gameId++
        game
    }

    fun createGameWithID(host: Session, id: ULong): Game = gamesLock.write {
        val hostPlayer = host.player ?: throw NotAuthenticatedException()
        removeInactive()
        val game = Game(id, host)
        Logger.info("Created new game (${game.id}) hosted by ${hostPlayer.displayName}")
        games[id] = game
        game
    }


    fun tryFindGame(test: (Game) -> Boolean): Game? = gamesLock.read { games.values.firstOrNull(test) }

    private fun removeInactive() {
        val removeKeys = ArrayList<ULong>()
        games.forEach { (key, game) ->
            if (!game.isActive) removeKeys.add(key)
        }
        removeKeys.forEach { games.remove(it) }
    }

    fun getGameById(id: ULong): Game? = gamesLock.read { games.values.firstOrNull { it.id == id } }
    fun releaseGame(game: Game) = gamesLock.write {
        Logger.info("Releasing game back to pool (${game.id})")
        games.remove(game.id)
    }
}
