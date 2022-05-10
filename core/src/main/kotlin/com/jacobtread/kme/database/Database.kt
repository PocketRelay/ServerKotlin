package com.jacobtread.kme.database

import com.jacobtread.kme.Config
import com.jacobtread.kme.database.adapter.MySQLDatabase
import com.jacobtread.kme.database.adapter.SQLiteDatabase
import com.jacobtread.kme.database.repos.PlayersRepository
import java.sql.Connection

class Database(val connection: Connection) {

    companion object {
        fun connect(config: Config): Database {
            val adapter = when (config.database.type) {
                Config.DatabaseType.MySQL -> MySQLDatabase()
                Config.DatabaseType.SQLite -> SQLiteDatabase()
            }
            val connection = adapter.connect(config)
            return Database(connection)
        }
    }

    val playerRepository = PlayersRepository(connection)

    init {
        playerRepository.init()
    }

}