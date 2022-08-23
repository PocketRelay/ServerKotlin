package com.jacobtread.relay.database.tables

import com.jacobtread.relay.database.Database
import com.jacobtread.relay.database.Table
import com.jacobtread.relay.database.asList
import com.jacobtread.relay.database.models.Player
import com.jacobtread.relay.database.models.PlayerClass
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import java.util.concurrent.CompletableFuture as Future

object PlayerClassesTable : Table {

    @Language("MySQL")
    override fun sql(): String = """
        -- Player Classes Table
        CREATE TABLE IF NOT EXISTS `player_classes`
        (
            `id`         INT(255) NOT NULL PRIMARY KEY AUTO_INCREMENT,
            `player_id`  INT(255) NOT NULL,
            `index`      INT(2)   NOT NULL,
            `name`       TEXT     NOT NULL,
            `level`      INT(3)   NOT NULL,
            `exp`        FLOAT(4) NOT NULL,
            `promotions` INT(255) NOT NULL,
        
            FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
        );
    """.trimIndent()

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
            .query("SELECT * FROM `player_classes` WHERE `player_id` = ?") {
                setInt(1, player.playerId)
            }
            .thenApply { outer -> outer.asList { inner -> inner.asPlayerClass() } }
    }

    private fun hasByPlayer(player: Player, index: Int): Future<Boolean> {
        return Database
            .exists("SELECT `id` FROM `player_classes` WHERE `player_id` = ? AND `index` = ?") {
                setInt(1, player.playerId)
                setInt(2, index)
            }
    }

    fun setByPlayer(player: Player, playerClass: PlayerClass): Future<Void> {
        return hasByPlayer(player, playerClass.index)
            .thenCompose { exists ->
                @Language("MySQL")
                val query: String
                if (exists) {
                    query = "UPDATE `player_classes` SET `name` = ?, `level` = ?, `exp` = ?, `promotions` = ? WHERE `player_id` = ? AND `index` = ?"
                } else {
                    query = "INSERT INTO `player_classes` (`name`, `level`, `exp`, `promotions`, `player_id`, `index`) VALUES (?, ?, ?, ?, ?, ?)"
                }
                Database.update(query) {
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