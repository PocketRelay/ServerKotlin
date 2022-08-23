package com.jacobtread.relay.database.tables

import com.jacobtread.relay.database.Database
import com.jacobtread.relay.database.Table
import com.jacobtread.relay.database.models.GalaxyAtWarData
import com.jacobtread.relay.database.models.Player
import com.jacobtread.relay.utils.Future
import com.jacobtread.relay.utils.VoidFuture
import org.intellij.lang.annotations.Language
import java.sql.ResultSet

object GalaxyAtWarTable : Table {

    @Language("MySQL")
    override fun sql(): String = """
        -- Galaxy At War Table
        CREATE TABLE IF NOT EXISTS `player_gaw`
        (
            `id`            INT(255)    NOT NULL PRIMARY KEY AUTO_INCREMENT,
            `player_id`     INT(255)    NOT NULL,
            `last_modified` BIGINT(255) NOT NULL,
            `group_a`       INT(8)      NOT NULL,
            `group_b`       INT(8)      NOT NULL,
            `group_c`       INT(8)      NOT NULL,
            `group_d`       INT(8)      NOT NULL,
            `group_e`       INT(8)      NOT NULL,

            FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
        );
    """.trimIndent()

    private fun ResultSet.asGalaxyAtWarData(): GalaxyAtWarData? {
        if (!next()) return null
        return GalaxyAtWarData(
            lastModified = getLong("last_modified"),
            groupA = getInt("group_a"),
            groupB = getInt("group_b"),
            groupC = getInt("group_c"),
            groupD = getInt("group_d"),
            groupE = getInt("group_e"),
        )
    }

    fun hasByPlayer(player: Player): Future<Boolean> {
        return Database
            .exists("SELECT * FROM `player_gaw` WHERE `player_id` = ? LIMIT 1") {
                setInt(1, player.playerId)
            }
    }

    fun getByPlayer(player: Player): Future<GalaxyAtWarData> {
        return Database
            .query("SELECT * FROM `player_gaw` WHERE `player_id` = ? LIMIT 1") {
                setInt(1, player.playerId)
            }
            .thenCompose {
                val future = Future<GalaxyAtWarData>()
                val result = it.asGalaxyAtWarData()
                if (result == null) {
                    val defaultData = GalaxyAtWarData.createDefault()
                    setByPlayer(player, defaultData)
                    future.complete(defaultData)
                } else {
                    future.complete(result)
                }
                future
            }
    }

    fun setByPlayer(player: Player, galaxyAtWarData: GalaxyAtWarData): VoidFuture {
        return hasByPlayer(player)
            .thenCompose { exists ->
                @Language("MySQL")
                val query: String
                if (exists) {
                    query = """
                       UPDATE `player_gaw` SET 
                       `last_modified` = ?, 
                       `group_a` = ?, 
                       `group_b` = ?, 
                       `group_c` = ? , 
                       `group_d` = ? , 
                       `group_e` = ?
                       WHERE `player_id` = ?
                    """.trimIndent()
                } else {
                    query = """
                    INSERT INTO `player_gaw` (
                        `last_modified`, 
                        `group_a`,
                        `group_b`, 
                        `group_c`, 
                        `group_d`, 
                        `group_e`,
                        `player_id`
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                }
                Database.update(query) {
                    setLong(1, galaxyAtWarData.lastModified)
                    setInt(2, galaxyAtWarData.groupA)
                    setInt(3, galaxyAtWarData.groupB)
                    setInt(4, galaxyAtWarData.groupC)
                    setInt(5, galaxyAtWarData.groupD)
                    setInt(6, galaxyAtWarData.groupE)
                    setInt(7, player.playerId)
                }
            }
    }
}