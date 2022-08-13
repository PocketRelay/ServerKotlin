package com.jacobtread.kme.data.attr

enum class EnemyTypeAttr(val enemyName: String, val key: String) {
    RANDOM("Unknown Enemy", "random"),
    CERBERUS("Cerberus", "enemy1"),
    GETH("Geth", "enemy2"),
    REAPER("Reaper", "enemy3"),
    COLLECTOR("Collector", "enemy4");

    companion object {
        private const val ENEMY_TYPE_ATTR = "ME3gameEnemyType"
        const val ENEMY_TYPE_RULE = "ME3_gameEnemyTypeRule"

        fun getFromAttr(map: Map<String, String>): EnemyTypeAttr {
            val value = map[ENEMY_TYPE_ATTR] ?: return RANDOM
            return values().firstOrNull { it.key == value } ?: RANDOM
        }
    }
}
