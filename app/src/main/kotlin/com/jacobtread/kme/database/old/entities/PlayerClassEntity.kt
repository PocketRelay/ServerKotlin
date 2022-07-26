package com.jacobtread.kme.database.old.entities

import com.jacobtread.kme.database.old.tables.PlayerClassesTable
import com.jacobtread.kme.database.updateOrCreate
import com.jacobtread.kme.tools.MEStringParser
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class PlayerClassEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerClassEntity>(PlayerClassesTable) {

        /**
         * setClassFrom Sets the player's class from the provided index and value.
         * This will update existing data or create a new row in the classes table
         *
         * @param playerEntity The player this class belongs to
         * @param index The index/id of this player class
         * @param value The encoded class data
         */
        fun updateOrCreate(playerEntity: PlayerEntity, index: Int, value: String) {
            PlayerClassEntity.updateOrCreate({ (PlayerClassesTable.player eq playerEntity.id) and (PlayerClassesTable.index eq index) }) {
                this.player = playerEntity.id
                this.index = index
                val parser = MEStringParser(value, 6)
                name =parser.str()
                level = parser.int(1)
                exp = parser.float(0f)
                promotions = parser.int(0)
            }
        }
    }

    var player by PlayerClassesTable.player
    var index by PlayerClassesTable.index
    var name by PlayerClassesTable.name
    var level by PlayerClassesTable.level
    var exp by PlayerClassesTable.exp
    var promotions by PlayerClassesTable.promotions

    val key: String get() = "class$index"
    fun toEncoded(): String = StringBuilder("20;4;")
        .append(name).append(';')
        .append(level).append(';')
        .append(exp).append(';')
        .append(promotions)
        .toString()
}