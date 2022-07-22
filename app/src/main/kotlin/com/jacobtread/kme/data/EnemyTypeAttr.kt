package com.jacobtread.kme.data

enum class EnemyTypeAttr(val enemyName: String, val key: String) {
    RANDOM("Unknown Enemy", "random"),
    CERBERUS("Cerberus", "enemy1"),
    GETH("Geth", "enemy2"),
    REAPER("Reaper", "enemy3"),
    COLLECTOR("Collector", "enemy4");

    companion object {
        const val ENEMY_TYPE_ATTR = "ME3gameEnemyType"
    }
}