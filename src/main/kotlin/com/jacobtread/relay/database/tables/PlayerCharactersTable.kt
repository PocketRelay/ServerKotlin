package com.jacobtread.relay.database.tables

import com.jacobtread.relay.database.Database
import com.jacobtread.relay.database.Table
import com.jacobtread.relay.database.asList
import com.jacobtread.relay.database.models.Player
import com.jacobtread.relay.database.models.PlayerCharacter
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import java.util.concurrent.CompletableFuture as Future

object PlayerCharactersTable : Table {

    @Language("MySQL")
    override fun sql(): String = """
         -- Player Characters Table
        CREATE TABLE IF NOT EXISTS `player_characters`
        (
            `id`                INT(255)    NOT NULL PRIMARY KEY AUTO_INCREMENT,
            `player_id`         INT(255)    NOT NULL,
            `index`             INT(3)      NOT NULL,
            `kit_name`          TEXT        NOT NULL,
            `name`              TEXT        NOT NULL,
            `tint1`             INT(4)      NOT NULL,
            `tint2`             INT(4)      NOT NULL,
            `pattern`           INT(4)      NOT NULL,
            `pattern_color`     INT(4)      NOT NULL,
            `phong`             INT(4)      NOT NULL,
            `emissive`          INT(4)      NOT NULL,
            `skin_tone`         INT(4)      NOT NULL,
            `seconds_played`    BIGINT(255) NOT NULL,
        
            `timestamp_year`    INT(255)    NOT NULL,
            `timestamp_month`   INT(255)    NOT NULL,
            `timestamp_day`     INT(255)    NOT NULL,
            `timestamp_seconds` INT(255)    NOT NULL,
        
            `powers`            TEXT        NOT NULL,
            `hotkeys`           TEXT        NOT NULL,
            `weapons`           TEXT        NOT NULL,
            `weapon_mods`       TEXT        NOT NULL,
        
            `deployed`          BOOLEAN     NOT NULL,
            `leveled_up`        BOOLEAN     NOT NULL,
        
            FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
        );
    """.trimIndent()

    /**
     * Function for converting the data stored on a [ResultSet]
     * into a [PlayerCharacter] object. Will return null if there
     * is no rows in the [ResultSet]
     *
     * @return The player character data or null
     */
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

    /**
     * Retrieves the player characters for the provided player
     *
     * @param player The player to get characters for
     * @return The future with the value of the player characters list
     */
    fun getByPlayer(player: Player): Future<ArrayList<PlayerCharacter>> {
        return Database
            .query("SELECT * FROM `player_characters` WHERE `player_id` = ?") {
                setInt(1, player.playerId)
            }
            .thenApply { outer -> outer.asList { inner -> inner.asPlayerCharacter() } }
    }


    /**
     * Checks whether the provided player has a character at
     * the provided index.
     *
     * @param player The player to check
     * @param index The index to check
     * @return The future with the value of whether the player has a character at that index
     */
    private fun hasByPlayer(player: Player, index: Int): Future<Boolean> {
        return Database
            .exists("SELECT `id` FROM `player_characters` WHERE `player_id` = ? AND `index` = ?") {
                setInt(1, player.playerId)
                setInt(2, index)
            }
    }


    /**
     * Sets the provided player character for the provided player
     * will create a new player character row or update existing ones
     *
     * @param player The player to set the character for
     * @param playerCharacter The player character data
     * @return The future for the update
     */
    fun setByPlayer(player: Player, playerCharacter: PlayerCharacter): Future<Void> {
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
                Database.update(query) {
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