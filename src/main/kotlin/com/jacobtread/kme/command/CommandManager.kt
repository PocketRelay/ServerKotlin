package com.jacobtread.kme.command

object CommandManager {

    private val commands = listOf<Command>()

    fun start() {
        while (true) {
            val line = readLine()
            println("> Recieved command: $line")
        }
    }
}