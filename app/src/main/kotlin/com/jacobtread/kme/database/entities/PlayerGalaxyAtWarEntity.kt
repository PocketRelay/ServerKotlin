package com.jacobtread.kme.database.entities

import com.jacobtread.kme.Environment
import com.jacobtread.kme.database.tables.PlayerGalaxyAtWarsTable
import com.jacobtread.kme.tools.unixTimeSeconds
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.max
import kotlin.math.min

class PlayerGalaxyAtWarEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerGalaxyAtWarEntity>(PlayerGalaxyAtWarsTable) {
        fun create(playerEntity: PlayerEntity): PlayerGalaxyAtWarEntity {
            return transaction {
                new {
                    player = playerEntity.id
                    timestamp = unixTimeSeconds()
                }
            }
        }
    }

    var player by PlayerGalaxyAtWarsTable.player
    var timestamp by PlayerGalaxyAtWarsTable.timestamp

    var a by PlayerGalaxyAtWarsTable.a
    var b by PlayerGalaxyAtWarsTable.b
    var c by PlayerGalaxyAtWarsTable.c
    var d by PlayerGalaxyAtWarsTable.d
    var e by PlayerGalaxyAtWarsTable.e

    fun increase(ai: Int, bi: Int, ci: Int, di: Int, ei: Int) {
        transaction {
            val maxValue = 10099
            a = min(maxValue, a + ai)
            b = min(maxValue, b + bi)
            c = min(maxValue, c + ci)
            d = min(maxValue, d + di)
            e = min(maxValue, e + ei)
        }
    }

    fun applyDecay() {
        if (Environment.gawReadinessDecay > 0f) {
            transaction {
                val minValue = 5000
                val time = unixTimeSeconds()
                val timeDifference = time - timestamp
                val days = timeDifference / 86400f
                val decayValue = (Environment.gawReadinessDecay * days * 100).toInt()
                a = max(minValue, a - decayValue)
                b = max(minValue, b - decayValue)
                c = max(minValue, c - decayValue)
                d = max(minValue, d - decayValue)
                e = max(minValue, e - decayValue)
            }
        }
    }

    fun average(): Int = (a + b + c + d + e) / 5
}