package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.Config
import com.jacobtread.kme.LOGGER
import java.sql.Connection

abstract class SQLAdapter(className: String) : DatabaseAdapter {

    lateinit var connection: Connection

    init {
        try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            LOGGER.fatal("SQL based missing database driver className: $className")
        }
    }

    override fun connect(baseConfig: Config.Database) {
        connection = connectSQL(baseConfig)
    }

    abstract fun connectSQL(baseConfig: Config.Database): Connection
}