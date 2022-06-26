package com.jacobtread.kme.tools

/**
 * MEStringParser Simple parser for parsing strings from ME3 that are split
 * by the ';' character. Uses a simple builder/iterator design. Allows the
 * values to be iterated and that increases the index and there are methods
 * for parsing as int or long etc
 *
 * @constructor
 *
 * @param value The encoded value
 * @param size The number of parts to parse
 */
class MEStringParser(value: String, size: Int) {

    private val parts = value.split(';', limit = size)
    private var index: Int = 0

    init {
        // Ensure that we have the size of data required
        require(parts.size == size) { "Failed to parse me3 contents"}
    }

    /**
     * skip Skips the provided amount of elements
     *
     * @param amount
     */
    fun skip(amount: Int) {
        index += amount
    }

    /**
     * str Returns the string at the current index
     * and increments the index
     *
     * @return The string value at this index
     */
    fun str(): String = parts[index++]

    /**
     * bool Returns the boolean value at the current
     * index and increments the index
     *
     * @return The boolean value
     */
    fun bool(): Boolean = parts[index++].equals("true", ignoreCase = true)

    /**
     * int Returns the int value at the current
     * index and increments the index
     *
     * @return The int value
     */
    fun int(default: Int = 0): Int = parts[index++].toIntOrNull() ?: default

    /**
     * long Returns the long value at the current
     * index and increments the index
     *
     * @return The long value
     */
    fun long(default: Long = 0L): Long = parts[index++].toLongOrNull() ?: default

    /**
     * float Returns the float value at the current
     * index and increments the index
     *
     * @return The float value
     */
    fun float(default: Float = 0f): Float = parts[index++].toFloatOrNull() ?: default
}