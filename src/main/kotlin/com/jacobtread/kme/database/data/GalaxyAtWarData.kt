package com.jacobtread.kme.database.data

import com.jacobtread.kme.Environment
import com.jacobtread.kme.utils.unixTimeDaysSince
import com.jacobtread.kme.utils.unixTimeSeconds

data class GalaxyAtWarData(
    var lastModified: Long,
    var groupA: Int,
    var groupB: Int,
    var groupC: Int,
    var groupD: Int,
    var groupE: Int,
) {
    val average: Int get() = (groupA + groupB + groupC + groupD + groupE) / 5

    var isModified = false

    /**
     * Adds the provided amounts to each of the values updating the isModified
     * state and ensuring that the new value is within the range of the minimum
     * and maximum values (5000 - 10099)
     *
     * @param ai The first group increment value
     * @param bi The second group increment value
     * @param ci The third group increment value
     * @param di The fourth group increment value
     * @param ei The fifth group increment value
     */
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

    /**
     * Applies the rediness decay. This is calculated by removing
     * 100 points for every [Environment.gawReadinessDecay] * days
     * passed
     */
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