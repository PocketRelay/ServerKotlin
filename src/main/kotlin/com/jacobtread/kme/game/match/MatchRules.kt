package com.jacobtread.kme.game.match

/**
 * MatchRules Known rules that are used for matching during matchmaking
 *
 * @property key The actual key it-self from the request
 * @property attrKey The attribute name on games
 * @constructor Create empty Rules
 */
enum class MatchRules(val key: String, val attrKey: String) {
    MAP("ME3_gameMapMatchRule", "ME3map"),
    ENEMY("ME3_gameEnemyTypeRule", "ME3gameEnemyType"),
    DIFFICULTY("ME3_gameDifficultyRule", "ME3gameDifficulty");

    companion object {
        /**
         * getByKey Finds the first rule with the same
         * key as the one provided or null if not found
         *
         * @param key The key to search for
         * @return The rule found or null
         */
        fun getByKey(key: String): MatchRules? = values().firstOrNull { it.key == key }
    }
}