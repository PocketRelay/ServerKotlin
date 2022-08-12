package com.jacobtread.kme.command

import com.jacobtread.kme.exceptions.CommandException

interface Command {

    val name: String
    val description: String
    val aliases: Array<String>
    val usage: String

    fun execute(args: List<String>)

    fun List<String>.value(index: Int): String {
        if (size > index) {
            return get(index)
        } else {
            throw CommandException("Expected ${index + 1} arguments but only got $size")
        }
    }

    fun List<String>.intValue(index: Int): Int {
        val value = value(index)
        try {
            return value.toInt()
        } catch (e: NumberFormatException) {
            throw CommandException("Expected argument \"$value\" to be an integer")
        }
    }

    fun List<String>.longValue(index: Int): Long {
        val value = value(index)
        try {
            return value.toLong()
        } catch (e: NumberFormatException) {
            throw CommandException("Expected argument \"$value\" to be an integer")
        }
    }

    fun List<String>.ulongValue(index: Int): ULong {
        val value = value(index)
        try {
            return value.toULong()
        } catch (e: NumberFormatException) {
            throw CommandException("Expected argument \"$value\" to be an integer")
        }
    }

    fun List<String>.floatValue(index: Int): Int {
        val value = value(index)
        try {
            return value.toInt()
        } catch (e: NumberFormatException) {
            throw CommandException("Expected argument \"$value\" to be a number")
        }
    }

    fun List<String>.doubleValue(index: Int): Int {
        val value = value(index)
        try {
            return value.toInt()
        } catch (e: NumberFormatException) {
            throw CommandException("Expected argument \"$value\" to be a number")
        }
    }

    fun List<String>.booleanValue(index: Int): Boolean {
        val value = value(index)
        return value.toBooleanStrictOrNull() ?: throw CommandException("Expected argument \"$value\" to be true/false")
    }
}