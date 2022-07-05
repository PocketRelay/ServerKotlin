package com.jacobtread.kme.database.entities

import com.jacobtread.kme.database.tables.PlayerSettingsTable
import com.jacobtread.kme.database.updateOrCreate
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

class PlayerSettingEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerSettingEntity>(PlayerSettingsTable) {

        /**
         * Updates the existing setting with the provided key to the
         * provided value or creates a new setting with that value
         *
         * @param playerEntity The player entity this setting is for
         * @param key The key that identifies this setting
         * @param value
         */
        fun updateOrCreate(playerEntity: PlayerEntity, key: String, value: String) {
            val playerId = playerEntity.id
            PlayerSettingEntity.updateOrCreate({ (PlayerSettingsTable.player eq playerId) and (PlayerSettingsTable.key eq key) }) {
                this.player = playerId
                this.key = key
                this.value = value
            }
        }
    }

    private var player by PlayerSettingsTable.player
    var key by PlayerSettingsTable.key
    var value by PlayerSettingsTable.value
}