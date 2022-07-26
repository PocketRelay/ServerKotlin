package com.jacobtread.kme.database.data

import com.jacobtread.kme.Environment
import com.jacobtread.kme.tools.unixTimeDaysSince
import com.jacobtread.kme.tools.unixTimeSeconds

data class GalaxyAtWarData(
    var lastModified: Long,
    var groupA: Int,
    var groupB: Int,
    var groupC: Int,
    var groupD: Int,
    var groupE: Int,
) {

    var isModified = false

    fun add(ai: Int, bi: Int, ci: Int, di: Int, ei: Int) {
        val minValue = 5000
        val maxValue = 10099
        groupA = (groupA + ai).coerceIn(minValue, maxValue)
        groupB = (groupB + bi).coerceIn(minValue, maxValue)
        groupC = (groupC + ci).coerceIn(minValue, maxValue)
        groupD = (groupD + di).coerceIn(minValue, maxValue)
        groupE = (groupE + ei).coerceIn(minValue, maxValue)
        isModified = true
    }

    fun applyDecay() {
        if (Environment.gawReadinessDecay <= 0f) return
        val daysPassed = unixTimeDaysSince(lastModified)
        val decayValue = (Environment.gawReadinessDecay * daysPassed * 100).toInt()
        add(-decayValue, -decayValue, -decayValue, -decayValue, -decayValue)
    }

    companion object {
        fun createDefault(): GalaxyAtWarData {
            return GalaxyAtWarData(
                unixTimeSeconds(),
                5000,
                5000,
                5000,
                5000,
                5000
            )
        }
    }

}