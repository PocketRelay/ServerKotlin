package com.jacobtread.relay.database

import java.sql.ResultSet

inline fun <T> ResultSet.asList(transform: (ResultSet) -> T?): ArrayList<T> {
    val output = ArrayList<T>()
    while (true) {
        val result = transform(this) ?: break
        output.add(result)
    }
    return output
}