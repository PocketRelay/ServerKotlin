package com.jacobtread.kme.data.attr

enum class GameStateAttr(val value: String) {
    IN_LOBBY("IN_LOBBY"),
    IN_LOBBY_LONGTIME("IN_LOBBY_LONGTIME"),
    IN_GAME_STARTING("IN_GAME_STARTING"),
    IN_GAME_MIDGAME("IN_GAME_MIDGAME"),
    IN_GAME_FINISHING("IN_GAME_FINISHING");

    companion object {
        const val GAME_STATE_ATTR = "ME3gameState"
        const val GAME_STATE_RULE = "ME3_gameStateMatchRule"
    }
}