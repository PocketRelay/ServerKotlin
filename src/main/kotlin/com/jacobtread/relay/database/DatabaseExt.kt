package com.jacobtread.relay.database

import java.sql.PreparedStatement
import java.sql.ResultSet

typealias StatementSetup = PreparedStatement.() -> Unit

inline fun <T> ResultSet.asList(transform: (ResultSet) -> T?): ArrayList<T> {
    val output = ArrayList<T>()
    while (true) {
        val result = transform(this) ?: break
        output.add(result)
    }
    return output
}