package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.LOGGER
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

class SQLiteDatabase : DatabaseAdapter<SQLiteDatabase.DBConfig> {

    @Serializable
    data class DBConfig(
        @Comment("The database host address")
        val file: String = "data/app.db",
    )

    override fun connect(config: DBConfig): Connection {
        try {
            Class.forName("org.sqlite.JDBC")
        } catch (e: ClassNotFoundException) {
            LOGGER.fatal("Missing MySQL driver")
        }
        try {
            val file = config.file
            val parentDir = Paths.get(file).parent
            if (parentDir.notExists()) parentDir.createDirectories()
            val connection = DriverManager.getConnection("jdbc:sqlite:$file")
            LOGGER.info("Connected to SQLite Database ($file)")
            return connection
        } catch (e: SQLException) {
            LOGGER.fatal("Unable to connect to database", e)
        }
    }
}