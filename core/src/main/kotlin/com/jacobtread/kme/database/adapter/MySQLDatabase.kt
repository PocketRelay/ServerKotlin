package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.Config
import com.jacobtread.kme.LOGGER
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class MySQLDatabase : DatabaseAdapter {

    @Serializable
    data class DBConfig(
        @Comment("The database host address")
        val host: String = "127.0.0.1",
        @Comment("The database port")
        val port: String = "3306",
        @Comment("The database account username")
        val user: String = "root",
        @Comment("The database account password")
        val password: String = "password",
        @Comment("The database to use")
        val database: String = "kme",
    )

    override fun connect(config: Config): Connection {
        val mysqlConfig = config.database.mysql
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
        } catch (e: ClassNotFoundException) {
            LOGGER.fatal("Missing MySQL driver")
        }
        try {
            val connection = DriverManager.getConnection(
                "jdbc:mysql://${mysqlConfig.host}:${mysqlConfig.port}/${mysqlConfig.database}",
                mysqlConfig.user,
                mysqlConfig.password
            )
            LOGGER.info("Connected to Database (${mysqlConfig.host}:${mysqlConfig.port})")
            return connection
        } catch (e: SQLException) {
            LOGGER.fatal("Unable to connect to database", e)
        }
    }
}