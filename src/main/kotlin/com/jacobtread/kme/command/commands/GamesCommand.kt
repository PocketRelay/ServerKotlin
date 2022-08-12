package com.jacobtread.kme.command.commands

import com.jacobtread.kme.command.Command
import com.jacobtread.kme.game.Game
import com.jacobtread.kme.utils.logging.Logger

class GamesCommand : Command {
    override val name: String get() = "Games"
    override val description: String get() = "Command for listing and inspecting running games on the server"
    override val aliases: Array<String> get() = arrayOf("games", "gl", "gamelist")
    override val usage: String get() = "games list OR games view {id}"

    override fun execute(args: List<String>) {
        val mode = args.value(0)
        if (mode == "list") {

            val output = StringBuilder()
                .appendLine("============= Active Games ==================")
                .appendLine("Use \"games view {id}\" on a game for more info")
                .appendLine("Game ID, Host Name, Player Count")
                .appendLine("=============================================")

            var totalGames = 0
            var totalPlayers = 0
            Game.forEachGame {
                val host = it.getHostOrNull()
                val playerCount = it.getPlayerCount()
                output.append(it.id)
                    .append(", ")
                    .append(host?.player?.displayName ?: "No Host")
                    .append(", ")
                    .append(playerCount)
                    .appendLine()
                totalGames++
                totalPlayers += playerCount
            }
            output.appendLine("=============================================")
                .append("Total Games: ")
                .append(totalGames)
                .append(", Total Players: ")
                .appendLine(totalPlayers)
                .appendLine("=============================================")
            Logger.commandResult(output.toString())
        } else if (mode == "view") {
            val id = args.ulongValue(1)
            val game = Game.getById(id)
            if (game == null) {
                Logger.error("Unable to find a game with the ID of \"$id\"")
                Logger.error("you can find game IDs using \"games list\"")
            } else {
                val output = StringBuilder()
                    .appendLine("=============================================")
            }
        }
    }
}