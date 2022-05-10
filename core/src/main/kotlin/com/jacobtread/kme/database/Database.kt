package com.jacobtread.kme.database

import com.jacobtread.kme.Config
import com.jacobtread.kme.database.adapter.MySQLDatabase
import com.jacobtread.kme.database.adapter.SQLiteDatabase
import com.jacobtread.kme.database.repos.PlayersRepository
import java.sql.Connection

class Database(val connection: Connection) {

    companion object {
        fun connect(config: Config): Database {
            val connection = when (config.database.type) {
                Config.DatabaseType.MySQL -> MySQLDatabase().connect(config.database.mysql)
                Config.DatabaseType.SQLite -> SQLiteDatabase().connect(config.database.sqlite)
            }
            return Database(connection)
        }
    }

    val playerRepository = PlayersRepository(connection)

    init {
        playerRepository.init()
    }

}