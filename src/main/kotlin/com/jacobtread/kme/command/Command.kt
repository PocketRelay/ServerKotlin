package com.jacobtread.kme.command

import com.jacobtread.kme.exceptions.CommandException

interface Command {

    val name: String
    val description: String
    val aliases: Array<String>

    fun execute(args: Array<String>)

    companion object {

        fun Array<String>.value(index: Int): String {
            if (size > index) {
                return get(index)
            } else {
                throw CommandException("Expected ${index + 1} arguments but only got $size")
            }
        }

        fun Array<String>.intValue(index: Int): Int {
            val value = value(index)
            try {
                return value.toInt()
            } catch (e: NumberFormatException) {
                throw CommandException("Expected argument \"$value\" to be an integer")
            }
        }

        fun Array<String>.floatValue(index: Int): Int {
            val value = value(index)
            try {
                return value.toInt()
            } catch (e: NumberFormatException) {
                throw CommandException("Expected argument \"$value\" to be a number")
            }
        }

        fun Array<String>.doubleValue(index: Int): Int {
            val value = value(index)
            try {
                return value.toInt()
            } catch (e: NumberFormatException) {
                throw CommandException("Expected argument \"$value\" to be a number")
            }
        }
    }
}