package com.jacobtread.relay.database.tables

import com.jacobtread.relay.database.Database
import com.jacobtread.relay.database.asList
import com.jacobtread.relay.database.data.Player
import com.jacobtread.relay.database.data.PlayerClass
import com.jacobtread.relay.utils.Future
import com.jacobtread.relay.utils.VoidFuture
import org.intellij.lang.annotations.Language
import java.sql.ResultSet

object PlayerClassesTable {

    private fun ResultSet.asPlayerClass(): PlayerClass? {
        if (!next()) return null
        return PlayerClass(
            index = getInt("index"),
            name = getString("name"),
            level = getInt("level"),
            exp = getFloat("exp"),
            promotions = getInt("promotions"),
        )
    }

    fun getByPlayer(player: Player): Future<ArrayList<PlayerClass>> {
        return Database
            .executeQuery("SELECT * FROM `player_classes` WHERE `player_id` = ?") {
                setInt(1, player.playerId)
            }
            .thenApply { outer -> outer.asList { inner -> inner.asPlayerClass() } }
    }

    private fun hasByPlayer(player: Player, index: Int): Future<Boolean> {
        return Database
            .executeExists("SELECT `id` FROM `player_classes` WHERE `player_id` = ? AND `index` = ?") {
                setInt(1, player.playerId)
                setInt(2, index)
            }
    }

    fun setByPlayer(player: Player, playerClass: PlayerClass): VoidFuture {
        return hasByPlayer(player, playerClass.index)
            .thenCompose { exists ->
                @Language("MySQL")
                val query: String
                if (exists) {
                    query = "UPDATE `player_classes` SET `name` = ?, `level` = ?, `exp` = ?, `promotions` = ? WHERE `player_id` = ? AND `index` = ?"
                } else {
                    query = "INSERT INTO `player_classes` (`name`, `level`, `exp`, `promotions`, `player_id`, `index`) VALUES (?, ?, ?, ?, ?, ?)"
                }
                Database.executeUpdateVoid(query) {
                    setString(1, playerClass.name)
                    setInt(2, playerClass.level)
                    setFloat(3, playerClass.exp)
                    setInt(4, playerClass.promotions)
                    setInt(5, player.playerId)
                    setInt(6, playerClass.index)
                }
            }
    }
}