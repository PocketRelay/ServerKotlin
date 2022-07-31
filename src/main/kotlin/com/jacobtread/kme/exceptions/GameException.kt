package com.jacobtread.kme.exceptions

open class GameException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, throwable: Throwable) : super(message, throwable)

    class StoppedException : GameException("Tried to access game that's already stopped / stopping.")
    class GameFullException : GameException("Tried to join game that was already full")
}

