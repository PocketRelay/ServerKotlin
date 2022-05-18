package com.jacobtread.kme.utils

class MEStringParser(value: String, size: Int) {

    private val parts = value.split(';', limit = size)
    private var index: Int = 0

    init {
        require(parts.size == size) { "Failed to parse me3 contents"}
    }

    fun skip(amount: Int) {
        index += amount
    }

    fun str(): String = parts[index++]
    fun bool(): Boolean = parts[index++].equals("true", ignoreCase = true)
    fun int(default: Int = 0): Int = parts[index++].toIntOrNull() ?: default
    fun long(default: Long = 0L): Long = parts[index++].toLongOrNull() ?: default
    fun float(default: Float = 0f): Float = parts[index++].toFloatOrNull() ?: default
}