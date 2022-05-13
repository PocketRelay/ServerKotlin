package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.Config
import com.jacobtread.kme.LOGGER
import com.jacobtread.kme.database.repos.PlayersRepository
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

class SQLiteAdapter : SQLAdapter("org.sqlite.JDBC") {

    @Serializable
    data class DatabaseConfig(
        @Comment("The database file path")
        val file: String = "data/app.db",
    )

    override fun connectSQL(baseConfig: Config.Database): Connection {
        val config = baseConfig.sqlite
        try {
            val file = config.file
            val parentDir = Paths.get(file).absolute().parent
            if (parentDir.notExists()) parentDir.createDirectories()
            val connection = DriverManager.getConnection("jdbc:sqlite:$file")
            LOGGER.info("Connected to SQLite Database ($file)")
            return connection
        } catch (e: SQLException) {
            LOGGER.fatal("Unable to connect to database", e)
        }
    }

    override fun getPlayersRepository(): PlayersRepository {
        return PlayersRepository.SQLite(connection)
    }
}