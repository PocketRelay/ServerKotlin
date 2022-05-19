package com.jacobtread.kme.game

import java.util.concurrent.locks.ReentrantReadWriteLock

object GameManager {

    private val gamesLock = ReentrantReadWriteLock()
    private val games = ArrayList<Game>()

}