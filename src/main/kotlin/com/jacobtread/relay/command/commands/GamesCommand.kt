package com.jacobtread.relay.command.commands

import com.jacobtread.relay.command.Command
import com.jacobtread.relay.data.attr.DifficultyAttr
import com.jacobtread.relay.data.attr.EnemyTypeAttr
import com.jacobtread.relay.data.attr.MapsAttr
import com.jacobtread.relay.exceptions.CommandException
import com.jacobtread.relay.game.Game
import com.jacobtread.relay.utils.logging.Logger

class GamesCommand : Command {
    override val name: String get() = "Games"
    override val description: String get() = "Command for listing and inspecting running games on the server"
    override val aliases: Array<String> get() = arrayOf("games", "gl", "gamelist")
    override val usage: Array<String>
        get() = arrayOf(
            "games list",
            "games view {id}",
            "games get-attr {id} {key}",
            "games set-attr {id} {key} {value}"
        )

    override fun execute(args: List<String>) {
        when (val mode = args.value(0)) {
            "list" -> executeListGames()
            "view", "get-attr", "set-attr" -> {
                val id = args.ulongValue(1)
                val game = Game.getById(id)
                if (game == null) {
                    Logger.error("Unable to find a game with the ID of \"$id\"")
                    Logger.error("you can find game IDs using \"games list\"")
                    return
                }

                when (mode) {
                    "view" -> executeViewGame(game)
                    "get-attr" -> executeGetAttr(game, args)
                    "set-attr" -> executeSetAttr(game, args)
                }
            }

            else -> throw CommandException("Unknown action \"$mode\"")
        }
    }

    private fun executeListGames() {
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
    }

    private fun executeViewGame(game: Game) {
        val attributes = game.getCopyOfAttributes()

        val difficulty = DifficultyAttr.getFromAttr(attributes)
        val enemy = EnemyTypeAttr.getFromAttr(attributes)
        val map = MapsAttr.getFromAttr(attributes)

        val output = StringBuilder()
            .appendLine("================ Viewing Game ================")
            .append("ID: ").append(game.id)
            .append(", State: ").append(game.gameState)
            .append(", Setting: ").append(game.gameSetting).appendLine()
            .append("Enemy: ").append(enemy.enemyName)
            .append(", Difficulty: ").append(difficulty.difficultyName).appendLine()
            .append("Map: ").append(map.mapName).append(" (").append(map.location).append(")").appendLine()
            .appendLine("================== Players ==================")
            .appendLine("ID, Display Name ")
            .appendLine("=============================================")

        game.appendPlayersTo(output)
        output.appendLine()
            .appendLine("================ Attributes =================")

        attributes.forEach { (key, value) ->
            output.append(key)
                .append(": ")
                .append(value)
                .appendLine()
        }

        output.appendLine("=============================================")
        Logger.commandResult(output.toString())
    }

    private fun executeGetAttr(game: Game, args: List<String>) {
        val key = args.value(2)
        val attribtues = game.getCopyOfAttributes()
        val attributeValue = attribtues[key]
        if (attributeValue == null) {
            Logger.error("The attribute \"$key\" is not set on this game")
        } else {
            Logger.info("The attribute \"$key\" is set to: \"$attributeValue\"")
        }
    }

    private fun executeSetAttr(game: Game, args: List<String>) {
        val key = args.value(2)
        val value = args.value(3)
        game.setAttributes(mapOf(key to value))
        Logger.info("Set attribute \"$key\" to \"$value\"")
    }
}