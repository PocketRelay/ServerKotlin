package com.jacobtread.kme.data.attr

enum class DifficultyAttr(val difficultyName: String, val key: String) {
    BRONZE("Bronze", "difficulty0"),
    SILVER("Silver", "difficulty1"),
    GOLD("Gold", "difficulty2"),
    PLATINUM("Platinum", "difficulty3");

    companion object {
        private const val DIFFICULTY_ATTR = "ME3gameDifficulty"
        const val DIFFICULTY_RULE = "ME3_gameDifficultyRule"

        fun getFromAttr(map: Map<String, String>): DifficultyAttr {
            val value = map[DIFFICULTY_ATTR] ?: return BRONZE
            return values().firstOrNull { it.key == value } ?: BRONZE
        }
    }
}
