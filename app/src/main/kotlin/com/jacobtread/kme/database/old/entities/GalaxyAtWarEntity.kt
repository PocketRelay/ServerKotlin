package com.jacobtread.kme.database.old.entities

import com.jacobtread.kme.Environment
import com.jacobtread.kme.database.firstOrNullSafe
import com.jacobtread.kme.database.old.tables.GalaxyAtWarTable
import com.jacobtread.kme.tools.unixTimeDaysSince
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Represents an entity of the galaxy at war statistics for
 * a specific player.
 *
 * @constructor Creates a new entity with the provided id
 *
 * @param id The id of the entity
 */
class GalaxyAtWarEntity(id: EntityID<Int>) : IntEntity(id) {

    /**
     * The player which this galaxy at war statistics are for
     */
    private var player by GalaxyAtWarTable.player

    /**
     * The time in unix seconds when this entity was last modified.
     * This is used in [applyDecay] for decaying the statistics
     * after a certain amount of time has passed.
     */
    private var lastModified by GalaxyAtWarTable.lastModified

    var groupA by GalaxyAtWarTable.groupA
        private set
    var groupB by GalaxyAtWarTable.groupB
        private set
    var groupC by GalaxyAtWarTable.groupC
        private set
    var groupD by GalaxyAtWarTable.groupD
        private set
    var groupE by GalaxyAtWarTable.groupE
        private set

    /**
     * Calculates the average of all the readiness values.
     */
    val average: Int get() = (groupA + groupB + groupC + groupD + groupE) / 5

    /**
     * Adds the provided amounts to each of the values updating them
     * in a transaction ensuring that the new value is within the
     * range of the minimum and maximum values (5000 - 10099)
     *
     * @param ai The first group increment value
     * @param bi The second group increment value
     * @param ci The third group increment value
     * @param di The fourth group increment value
     * @param ei The fifth group increment value
     */
    fun add(ai: Int, bi: Int, ci: Int, di: Int, ei: Int) {
        transaction {
            val minValue = 5000
            val maxValue = 10099
            groupA = (groupA + ai).coerceIn(minValue, maxValue)
            groupB = (groupB + bi).coerceIn(minValue, maxValue)
            groupC = (groupC + ci).coerceIn(minValue, maxValue)
            groupD = (groupD + di).coerceIn(minValue, maxValue)
            groupE = (groupE + ei).coerceIn(minValue, maxValue)
        }
    }

    /**
     * Applies the rediness decay. This is calculated by removing
     * 100 points for every [Environment.gawReadinessDecay] * days
     * passed
     */
    fun applyDecay() {
        if (Environment.gawReadinessDecay <= 0f) return
        transaction {
            val daysPassed = unixTimeDaysSince(lastModified)
            val decayValue = (Environment.gawReadinessDecay * daysPassed * 100).toInt()
            add(-decayValue, -decayValue, -decayValue, -decayValue, -decayValue)
        }
    }

    companion object : IntEntityClass<GalaxyAtWarEntity>(GalaxyAtWarTable) {

        /**
         * Retrieves the galaxy at war entity for the provided player.
         * Creating a new entity if one does not exist and applying the
         * global readiness decay if it is enabled
         *
         * @param playerEntity The player entity to retrieve for
         * @return The galaxy at war entity for this player
         */
        fun forPlayer(playerEntity: PlayerEntity): GalaxyAtWarEntity {
            val existing = firstOrNullSafe { GalaxyAtWarTable.player eq playerEntity.id }
            if (existing != null) { // If this player already has a galaxy at war entity
                existing.applyDecay() // Apply the decay to the values
                return existing
            }
            // Create a new galaxy at war entity
            return transaction {
                new {
                    player = playerEntity.id
                }
            }
        }
    }
}