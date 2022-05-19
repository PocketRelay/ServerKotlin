package com.jacobtread.kme.game

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object GameManager {

    private val gamesLock = ReentrantReadWriteLock()
    private val games = HashMap<Int, Game>()
    private var gameId: Int = 0

    fun createGame(host: PlayerSession): Game = gamesLock.write {
        val id = gameId++
        val game = Game(id, id, host)
        games[id] = game
        game
    }

    fun getFreeGame(): Game? = gamesLock.read { games.values.find { it.isJoinable() } }
    fun getGameById(id: Int): Game? = gamesLock.read { games.values.find { it.id == id } }
    fun releaseGame(game: Game) = gamesLock.write { games.remove(game.id) }

}