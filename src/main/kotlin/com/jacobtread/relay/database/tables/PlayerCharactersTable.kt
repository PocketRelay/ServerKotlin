package com.jacobtread.relay.database.tables

import com.jacobtread.relay.database.Database
import com.jacobtread.relay.database.asList
import com.jacobtread.relay.database.data.Player
import com.jacobtread.relay.database.data.PlayerCharacter
import com.jacobtread.relay.utils.Future
import com.jacobtread.relay.utils.VoidFuture
import org.intellij.lang.annotations.Language
import java.sql.ResultSet

object PlayerCharactersTable {

    private fun ResultSet.asPlayerCharacter(): PlayerCharacter? {
        if (!next()) return null
        return PlayerCharacter(
            index = getInt("index"),
            kitName = getString("kit_name"),
            name = getString("name"),
            tint1 = getInt("tint1"),
            tint2 = getInt("tint2"),
            pattern = getInt("pattern"),
            patternColor = getInt("pattern_color"),
            phong = getInt("phong"),
            emissive = getInt("emissive"),
            skinTone = getInt("skin_tone"),
            secondsPlayed = getLong("seconds_played"),
            timestampYear = getInt("timestamp_year"),
            timestampMonth = getInt("timestamp_month"),
            timestampDay = getInt("timestamp_day"),
            timestampSeconds = getInt("timestamp_seconds"),
            powers = getString("powers"),
            hotkeys = getString("hotkeys"),
            weapons = getString("weapons"),
            weaponMods = getString("weapon_mods"),
            deployed = getBoolean("deployed"),
            leveledUp = getBoolean("leveled_up"),
        )
    }

    fun getByPlayer(player: Player): Future<ArrayList<PlayerCharacter>> {
        return Database
            .executeQuery("SELECT * FROM `player_characters` WHERE `player_id` = ?") {
                setInt(1, player.playerId)
            }
            .thenApply { outer -> outer.asList { inner -> inner.asPlayerCharacter() } }
    }

    private fun hasByPlayer(player: Player, index: Int): Future<Boolean> {
        return Database
            .executeExists("SELECT `id` FROM `player_characters` WHERE `player_id` = ? AND `index` = ?") {
                setInt(1, player.playerId)
                setInt(2, index)
            }
    }

    fun setByPlayer(player: Player, playerCharacter: PlayerCharacter): VoidFuture {
        return hasByPlayer(player, playerCharacter.index)
            .thenCompose { exists ->
                @Language("MySQL")
                val query: String
                if (exists) {
                    query = """
                        UPDATE `player_characters` SET 
                        `kit_name` = ?, `name` = ?, `tint1` = ?, `tint2` = ?,
                        `pattern` = ?, `pattern_color` = ?, `phong` = ?, `emissive` = ?,
                        `skin_tone` = ?, `seconds_played` = ?, `timestamp_year` = ?,
                        `timestamp_month` = ?, `timestamp_day` = ?, `timestamp_seconds` = ?,
                        `powers` = ?, `hotkeys` = ?, `weapons` = ?, `weapon_mods` = ?,
                        `deployed` = ?, `leveled_up` = ?
                        WHERE `player_id` = ? AND `index` = ?
                    """.trimIndent()
                } else {
                    query = """
                        INSERT INTO `player_characters` 
                        (
                             `kit_name`, `name`, `tint1`, `tint2`,
                             `pattern`, `pattern_color`, `phong`, `emissive`, `skin_tone`, 
                             `seconds_played`, `timestamp_year`, `timestamp_month`, `timestamp_day`, 
                             `timestamp_seconds`, `powers`, `hotkeys`, `weapons`, `weapon_mods`, `deployed`, 
                             `leveled_up`, `player_id`, `index`
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                }
                Database.executeUpdateVoid(query) {
                    setString(1, playerCharacter.kitName)
                    setString(2, playerCharacter.name)
                    setInt(3, playerCharacter.tint1)
                    setInt(4, playerCharacter.tint2)
                    setInt(5, playerCharacter.pattern)
                    setInt(6, playerCharacter.patternColor)
                    setInt(7, playerCharacter.phong)
                    setInt(8, playerCharacter.emissive)
                    setInt(9, playerCharacter.skinTone)
                    setLong(10, playerCharacter.secondsPlayed)
                    setInt(11, playerCharacter.timestampYear)
                    setInt(12, playerCharacter.timestampMonth)
                    setInt(13, playerCharacter.timestampDay)
                    setInt(14, playerCharacter.timestampSeconds)
                    setString(15, playerCharacter.powers)
                    setString(16, playerCharacter.hotkeys)
                    setString(17, playerCharacter.weapons)
                    setString(18, playerCharacter.weaponMods)
                    setBoolean(19, playerCharacter.deployed)
                    setBoolean(20, playerCharacter.leveledUp)
                    setInt(21, player.playerId)
                    setInt(22, playerCharacter.index)
                }
            }
    }
}