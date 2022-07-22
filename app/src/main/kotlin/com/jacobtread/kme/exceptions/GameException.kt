package com.jacobtread.kme.exceptions

open class GameException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, throwable: Throwable) : super(message, throwable)
}

class GameStoppedException : GameException("Tried to access game that's already stopping.")