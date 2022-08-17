package com.jacobtread.relay.data.attr

enum class GameStateAttr(val value: String, val intValue: Int = 0) {
    IN_LOBBY("IN_LOBBY", 130),
    IN_LOBBY_LONGTIME("IN_LOBBY_LONGTIME"),
    IN_GAME_STARTING("IN_GAME_STARTING"),
    IN_GAME_MIDGAME("IN_GAME_MIDGAME"),
    IN_GAME_FINISHING("IN_GAME_FINISHING");

    companion object {
        private const val GAME_STATE_ATTR = "ME3gameState"
        const val GAME_STATE_RULE = "ME3_gameStateMatchRule"

        fun getFromAttr(map: Map<String, String>): GameStateAttr {
            val value = map[GAME_STATE_ATTR] ?: return IN_LOBBY
            return values().firstOrNull { it.value == value } ?: IN_LOBBY
        }
    }
}
