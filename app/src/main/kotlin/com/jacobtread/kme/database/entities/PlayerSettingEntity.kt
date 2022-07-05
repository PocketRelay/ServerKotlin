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
         * setSettingUnknown Stores setting key values pairs for settings
         * that are not parsed. Will update existing values if there are
         * any otherwise will create new row
         *
         * @param playerEntity The player to set the setting for
         * @param key The setting key
         * @param value The setting value
         */
        fun setSetting(playerEntity: PlayerEntity, key: String, value: String) {
            val playerId = playerEntity.id
            PlayerSettingEntity.updateOrCreate({ (PlayerSettingsTable.player eq playerId) and (PlayerSettingsTable.key eq key) }) {
                this.player = playerId
                this.key = key
                this.value = value
            }
        }
    }

    var player by PlayerSettingsTable.player
    var key by PlayerSettingsTable.key
    var value by PlayerSettingsTable.value
}