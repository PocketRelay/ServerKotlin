package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.Config
import com.jacobtread.kme.LOGGER
import com.jacobtread.kme.database.repos.PlayersRepository
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class MySQLAdapter : SQLAdapter("com.mysql.cj.jdbc.Driver") {

    @Serializable
    data class DatabaseConfig(
        val host: String = "127.0.0.1",
        val port: String = "3306",
        val user: String = "root",
        val password: String = "password",
        val database: String = "kme",
    )

    override fun connectSQL(baseConfig: Config.Database): Connection {
        val config = baseConfig.mysql
        try {
            val connection = DriverManager.getConnection(
                "jdbc:mysql://${config.host}:${config.port}/${config.database}",
                config.user,
                config.password
            )
            LOGGER.info("Connected to MySQL Database (${config.host}:${config.port})")
            return connection
        } catch (e: SQLException) {
            LOGGER.fatal("Unable to connect to MySQL database", e)
        }
    }

    override fun getPlayersRepository(): PlayersRepository {
        return PlayersRepository.MySQL(connection)
    }
}