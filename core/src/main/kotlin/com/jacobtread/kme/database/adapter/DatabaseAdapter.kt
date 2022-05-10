package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.Config
import java.sql.Connection

interface DatabaseAdapter<C> {

    fun connect(config: C): Connection

}