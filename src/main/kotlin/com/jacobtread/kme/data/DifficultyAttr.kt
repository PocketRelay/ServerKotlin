package com.jacobtread.kme.data

enum class DifficultyAttr(val difficultyName: String, val key: String) {
    BRONZE("Bronze", "difficulty0"),
    SILVER("Silver", "difficulty1"),
    GOLD("Gold", "difficulty2"),
    PLATINUM("Platinum", "difficulty3");

    companion object {
        const val DIFFICULTY_ATTR = "ME3gameDifficulty"
        const val DIFFICULTY_RULE = "ME3_gameDifficultyRule"
    }
}