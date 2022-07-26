package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.database.RuntimeDriver
import com.jacobtread.kme.utils.logging.Logger
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class MySQLDatabaseAdapter(
    host: String,
    port: Int,
    user: String,
    password: String,
    database: String,
) : SQLDatabaseAdapter(createConnection(host, port, user, password, database)) {

    companion object {
        fun createConnection(
            host: String,
            port: Int,
            user: String,
            password: String,
            database: String,
        ): Connection {
            val version = "8.0.29"
            RuntimeDriver.createRuntimeDriver(
                "https://repo1.maven.org/maven2/mysql/mysql-connector-java/$version/mysql-connector-java-$version.jar",
                "com.mysql.cj.jdbc.Driver",
                "mysql.jar"
            )
            try {
                return DriverManager.getConnection("jdbc:mysql://${host}:${port}/${database}", user, password)
            } catch (e: SQLException) {
                Logger.fatal("Unable to connect to SQLite database", e)
            }
        }
    }

    override fun setup() {
        TODO("Implement creation of MySQL tables")
    }
}