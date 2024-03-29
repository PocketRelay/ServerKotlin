package com.jacobtread.relay.database.models

import com.jacobtread.relay.utils.MEStringParser
import kotlinx.serialization.Serializable

@Serializable
data class PlayerClass(
    val index: Int,
    val name: String,
    var level: Int,
    var exp: Float,
    var promotions: Int,
) {
    fun getKey(): String = "class$index"
    fun toEncoded(): String = StringBuilder("20;4;")
        .append(name).append(';')
        .append(level).append(';')
        .append(exp).append(';')
        .append(promotions)
        .toString()

    companion object {
        fun createFromKeyValue(key: String, value: String): PlayerClass {
            val index = key.substring(5).toInt()
            val parser = MEStringParser(value, 6)
            return PlayerClass(
                index,
                parser.str(),
                parser.int(1),
                parser.float(0f),
                parser.int(0)
            )
        }
    }
}
