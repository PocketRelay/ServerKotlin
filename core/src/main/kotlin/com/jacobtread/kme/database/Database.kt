package com.jacobtread.kme.database

import com.jacobtread.kme.Config
import com.jacobtread.kme.LOGGER
import com.jacobtread.kme.database.repos.PlayersRepository
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class Database(val connection: Connection) {

    companion object {
        fun connect(config: Config): Database {
            val dbConfig = config.database
            try {
                Class.forName("com.mysql.cj.jdbc.Driver")
            } catch (e: ClassNotFoundException) {
                LOGGER.fatal("Missing MySQL driver")
            }
            try {
                val connection = DriverManager.getConnection(
                    "jdbc:mysql://${dbConfig.host}:${dbConfig.port}/${dbConfig.database}",
                    dbConfig.user,
                    dbConfig.password
                )
                LOGGER.info("Connected to Database (${dbConfig.host}:${dbConfig.port})")
                LOGGER.info("Setting up Database...")
                return Database(connection)
            } catch (e: SQLException) {
                LOGGER.fatal("Unable to connect to database", e)
            }
        }
    }

    val playerRepository = PlayersRepository(connection)

    init {
        playerRepository.init()
    }

}