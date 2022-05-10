package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.Config
import java.sql.Connection

interface DatabaseAdapter {

    fun connect(config: Config): Connection

}