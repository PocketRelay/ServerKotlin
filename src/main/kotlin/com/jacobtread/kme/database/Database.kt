package com.jacobtread.kme.database

import com.jacobtread.kme.Config
import com.jacobtread.kme.LOGGER
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object Database {

    lateinit var connection: Connection

    fun connect(config: Config) {
        val dbConfig = config.database
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
        } catch (e: ClassNotFoundException) {
            LOGGER.fatal("Missing MySQL driver")
        }
        try {
            connection = DriverManager.getConnection(
                "jdbc:mysql://${dbConfig.host}:${dbConfig.port}",
                dbConfig.user,
                dbConfig.password
            )
            LOGGER.info("Connected to Database (${dbConfig.host}:${dbConfig.port})")
        } catch (e: SQLException) {
            LOGGER.fatal("Unable to connect to database", e)
        }
    }

}