package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.Config
import com.jacobtread.kme.database.repos.PlayersRepository

interface DatabaseAdapter {
    fun getPlayersRepository(): PlayersRepository
    fun connect(baseConfig: Config.Database)
}
