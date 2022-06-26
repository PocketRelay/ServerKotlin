package com.jacobtread.kme.utils.logging

enum class Level(val levelName: String, val index: Byte, private val colorCode: String) {
    INFO("INFO", 4, "\u001b[36m"),
    WARN("WARN", 3, "\u001B[33m"),
    ERROR("ERROR", 2, "\u001B[31m"),
    FATAL("FATAL", 1, "\u001B[31m"),
    DEBUG("DEBUG", 5, "\u001B[30m");

    /**
     * coloredText Appends the bold as well as the
     * color code for the level to the level name
     * for pretty terminal formatting
     *
     * @return The text with the color codes surrounding it
     */
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

