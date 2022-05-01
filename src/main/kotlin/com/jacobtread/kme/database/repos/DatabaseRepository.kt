package com.jacobtread.kme.database.repos

import java.sql.Connection

abstract class DatabaseRepository(val connection: Connection) {
    abstract fun init()
}