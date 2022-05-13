package com.jacobtread.kme.database

import com.jacobtread.kme.Config
import com.jacobtread.kme.database.adapter.DatabaseAdapter
import com.jacobtread.kme.database.adapter.MySQLAdapter
import com.jacobtread.kme.database.adapter.SQLiteAdapter

class Database(adapter: DatabaseAdapter, val config: Config) {

    companion object {
        fun connect(config: Config): Database {
            val adapter: DatabaseAdapter = when (config.database.type) {
                Config.DatabaseType.MySQL -> MySQLAdapter()
                Config.DatabaseType.SQLite -> SQLiteAdapter()
            }
            adapter.connect(config.database)
            return Database(adapter, config)
        }
    }

    val playerRepository = adapter.getPlayersRepository()

    init {
        playerRepository.init()
    }

}