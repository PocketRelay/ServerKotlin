package com.jacobtread.kme.utils.logging

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Level(val levelName: String, val index: Byte, val colorCode: String) {
    @SerialName("INFO")
    INFO("INFO", 4, "\u001b[36m"),

    @SerialName("WARN")
    WARN("WARN", 3, "\u001B[33m"),

    @SerialName("ERROR")
    ERROR("ERROR", 2, "\u001B[31m"),

    @SerialName("FATAL")
    FATAL("FATAL", 1, "\u001B[31m"),

    @SerialName("DEBUG")
    DEBUG("DEBUG", 5, "\u001B[30m");

    fun coloredText(): String = "\u001b[1m$colorCode[$levelName]\u001B[0m\u001B[0m"

    companion object {
        fun fromName(name: String): Level {
            return when (name.lowercase()) {
                "warn" -> WARN
                "error" -> ERROR
                "fatal" -> FATAL
                "debug" -> DEBUG
                else -> INFO
            }
        }
    }
}

