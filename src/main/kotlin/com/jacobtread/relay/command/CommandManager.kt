package com.jacobtread.relay.command

import com.jacobtread.relay.command.commands.GamesCommand
import com.jacobtread.relay.exceptions.CommandException
import com.jacobtread.relay.utils.logging.Logger

object CommandManager {

    private val commands = listOf<Command>(
        GamesCommand()
    )

    private fun getCommand(alias: String): Command? {
        val lowercaseAlias = alias.lowercase()
        return commands.firstOrNull { it.aliases.contains(lowercaseAlias) }
    }

    fun start() {
        while (true) {
            val line = readLine()
            if (line.isNullOrBlank()) continue

            val args = line
                .trim()
                .split(" ")
                .filter { it.isNotEmpty() }

            if (args.isEmpty()) continue

            val commandName = args[0]
            val command = getCommand(commandName)

            if (command == null) {
                Logger.error("Command \"$commandName\" not found. Try using \"help\" instead")
                continue
            }

            val commandArgs = args.drop(1)

            try {
                Logger.commandResult("> $line")
                command.execute(commandArgs)
            } catch (e: CommandException) {
                Logger.error("Failed to execute command $commandName cause: ${e.message ?: "Unknown"}")
                Logger.error("Try using one of the following:")
                command.usage.forEach {
                    Logger.error("- $it")
                }
            }
        }
    }
}