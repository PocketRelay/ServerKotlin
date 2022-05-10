package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.Config
import com.jacobtread.kme.LOGGER
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class MySQLDatabase : DatabaseAdapter<MySQLDatabase.DBConfig> {

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

    override fun connect(config: DBConfig): Connection {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
        } catch (e: ClassNotFoundException) {
            LOGGER.fatal("Missing MySQL driver")
        }
        try {
            val connection = DriverManager.getConnection(
                "jdbc:mysql://${config.host}:${config.port}/${config.database}",
                config.user,
                config.password
            )
            LOGGER.info("Connected to Database (${config.host}:${config.port})")
            return connection
        } catch (e: SQLException) {
            LOGGER.fatal("Unable to connect to database", e)
        }
    }
}