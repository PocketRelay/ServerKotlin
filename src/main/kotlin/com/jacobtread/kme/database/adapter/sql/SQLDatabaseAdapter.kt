package com.jacobtread.kme.database.adapter.sql

import com.jacobtread.kme.data.retriever.OriginDetailsRetriever
import com.jacobtread.kme.database.adapter.DatabaseAdapter
import com.jacobtread.kme.database.data.GalaxyAtWarData
import com.jacobtread.kme.database.data.Player
import com.jacobtread.kme.database.data.PlayerCharacter
import com.jacobtread.kme.database.data.PlayerClass
import com.jacobtread.kme.exceptions.DatabaseException
import java.sql.*

@Suppress("LargeClass")
abstract class SQLDatabaseAdapter(
    protected val connection: Connection,
) : DatabaseAdapter {

    override fun isEmailTaken(email: String): Boolean {
        try {
            val statement = connection.prepareStatement("SELECT `id` FROM `players` WHERE email = ? LIMIT 1")
            statement.setString(1, email)
            val resultSet = statement.executeQuery()
            val result = resultSet.next()
            statement.close()
            return result
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in isPlayerEmailTaken", e)
        }
    }

    private fun getPlayerFromResultSet(resultSet: ResultSet): Player? {
        if (!resultSet.next()) return null
        return Player(
            playerId = resultSet.getInt("id"),
            email = resultSet.getString("email"),
            displayName = resultSet.getString("display_name"),
            password = resultSet.getString("password"),
            sessionToken = resultSet.getString("session_token"),
            credits = resultSet.getInt("credits"),
            creditsSpent = resultSet.getInt("credits_spent"),
            gamesPlayed = resultSet.getInt("games_played"),
            secondsPlayed = resultSet.getLong("seconds_played"),
            inventory = resultSet.getString("inventory"),
            csReward = resultSet.getInt("csreward"),
            faceCodes = resultSet.getString("face_codes"),
            newItem = resultSet.getString("new_item"),
            completion = resultSet.getString("completion"),
            progress = resultSet.getString("progress"),
            cscompletion = resultSet.getString("cs_completion"),
            cstimestamps1 = resultSet.getString("cs_timestamps_1"),
            cstimestamps2 = resultSet.getString("cs_timestamps_2"),
            cstimestamps3 = resultSet.getString("cs_timestamps_3"),
        )
    }

    override fun getPlayers(offset: Int, count: Int): List<Player> {
        try {
            val players = ArrayList<Player>()
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery("SELECT * FROM `players` LIMIT $count OFFSET $offset")
            var player: Player?
            while (true) {
                player = getPlayerFromResultSet(resultSet)
                if (player == null) break
                players.add(player)
            }
            statement.close()
            return players
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in getPlayers", e)
        }
    }

    override fun getPlayerById(id: Int): Player? {
        try {
            val statement = connection.prepareStatement("SELECT * FROM `players` WHERE `id` = ? LIMIT 1")
            statement.setInt(1, id)
            val resultSet = statement.executeQuery()
            val player = getPlayerFromResultSet(resultSet)
            statement.close()
            return player
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in getPlayerById", e)
        }
    }

    override fun getPlayerByEmail(email: String): Player? {
        try {
            val statement = connection.prepareStatement("SELECT * FROM `players` WHERE `email` = ? AND `origin` = ? LIMIT 1")
            statement.setString(1, email)
            statement.setBoolean(2, false)
            val resultSet = statement.executeQuery()
            val player = getPlayerFromResultSet(resultSet)
            statement.close()
            return player
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in getPlayerById", e)
        }
    }

    override fun getPlayerBySessionToken(sessionToken: String): Player? {
        try {
            val statement = connection.prepareStatement("SELECT * FROM `players` WHERE `session_token` = ? AND `origin` = ? LIMIT 1")
            statement.setString(1, sessionToken)
            statement.setBoolean(2, false)
            val resultSet = statement.executeQuery()
            val player = getPlayerFromResultSet(resultSet)
            statement.close()
            return player
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in getPlayerById", e)
        }
    }

    private fun getPlayerClassFromResultSet(resultSet: ResultSet): PlayerClass {
        return PlayerClass(
            index = resultSet.getInt("index"),
            name = resultSet.getString("name"),
            level = resultSet.getInt("level"),
            exp = resultSet.getFloat("exp"),
            promotions = resultSet.getInt("promotions"),
        )
    }

    override fun getPlayerClasses(player: Player): MutableList<PlayerClass> {
        try {
            val statement = connection.prepareStatement("SELECT * FROM `player_classes` WHERE `player_id` = ?")
            statement.setInt(1, player.playerId)
            val resultSet = statement.executeQuery()
            val results = ArrayList<PlayerClass>()
            while (resultSet.next()) {
                val value = getPlayerClassFromResultSet(resultSet)
                results.add(value)
            }
            statement.close()
            return results
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in getPlayerClasses", e)
        }
    }

    private fun getPlayerCharacterFromResultSet(resultSet: ResultSet): PlayerCharacter {
        return PlayerCharacter(
            index = resultSet.getInt("index"),
            kitName = resultSet.getString("kit_name"),
            name = resultSet.getString("name"),
            tint1 = resultSet.getInt("tint1"),
            tint2 = resultSet.getInt("tint2"),
            pattern = resultSet.getInt("pattern"),
            patternColor = resultSet.getInt("pattern_color"),
            phong = resultSet.getInt("phong"),
            emissive = resultSet.getInt("emissive"),
            skinTone = resultSet.getInt("skin_tone"),
            secondsPlayed = resultSet.getLong("seconds_played"),
            timestampYear = resultSet.getInt("timestamp_year"),
            timestampMonth = resultSet.getInt("timestamp_month"),
            timestampDay = resultSet.getInt("timestamp_day"),
            timestampSeconds = resultSet.getInt("timestamp_seconds"),
            powers = resultSet.getString("powers"),
            hotkeys = resultSet.getString("hotkeys"),
            weapons = resultSet.getString("weapons"),
            weaponMods = resultSet.getString("weapon_mods"),
            deployed = resultSet.getBoolean("deployed"),
            leveledUp = resultSet.getBoolean("leveled_up"),
        )
    }

    override fun getPlayerCharacters(player: Player): MutableList<PlayerCharacter> {
        try {
            val statement = connection.prepareStatement("SELECT * FROM `player_characters` WHERE `player_id` = ?")
            statement.setInt(1, player.playerId)
            val resultSet = statement.executeQuery()
            val results = ArrayList<PlayerCharacter>()
            while (resultSet.next()) {
                val value = getPlayerCharacterFromResultSet(resultSet)
                results.add(value)
            }
            statement.close()
            return results
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in getPlayerCharacters", e)
        }
    }

    override fun getGalaxyAtWarData(player: Player): GalaxyAtWarData {
        try {
            val statement = connection.prepareStatement("SELECT * FROM `player_gaw` WHERE `player_id` = ? LIMIT 1")
            statement.setInt(1, player.playerId)
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                val galaxyAtWarData = GalaxyAtWarData(
                    lastModified = resultSet.getLong("last_modified"),
                    groupA = resultSet.getInt("group_a"),
                    groupB = resultSet.getInt("group_b"),
                    groupC = resultSet.getInt("group_c"),
                    groupD = resultSet.getInt("group_d"),
                    groupE = resultSet.getInt("group_e"),
                )
                statement.close()
                return galaxyAtWarData
            } else {
                statement.close()
                val defaultData = GalaxyAtWarData.createDefault()
                setGalaxyAtWarData(player, defaultData)
                return defaultData
            }
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in getGalaxyAtWarData", e)
        }
    }

    override fun createPlayer(email: String, hashedPassword: String): Player {
        try {
            val statement = connection.prepareStatement(
                "INSERT INTO `players` (`email`, `display_name`, `password`, `inventory`, `origin`) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
            statement.setString(1, email)
            statement.setString(2, email.take(99)) // Display name
            statement.setString(3, hashedPassword) // Password
            statement.setString(4, "") // Inventory
            statement.setBoolean(5, false)
            statement.executeUpdate()
            val generatedKeys = statement.generatedKeys
            if (generatedKeys.next()) {
                val id = generatedKeys.getInt(1)
                statement.close()
                return createDefaultPlayerFrom(id, email, email, hashedPassword)
            } else {
                throw DatabaseException("Creating player failed. No id key was generated ")
            }
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in createPlayer", e)
        }
    }

    private fun getExistingOriginPlayer(details: OriginDetailsRetriever.OriginDetails): Player? {
        try {
            val statement = connection.prepareStatement("SELECT * FROM `players` WHERE  `email` = ? AND `origin` = ? LIMIT 1")
            statement.setString(1, details.email)
            statement.setBoolean(2, true)
            val resultSet = statement.executeQuery()
            val player = getPlayerFromResultSet(resultSet)
            statement.close()
            return player
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in getExistingOriginPlayer", e)
        }
    }

    private fun createOriginPlayer(details: OriginDetailsRetriever.OriginDetails): Player {
        try {
            val statement = connection.prepareStatement(
                "INSERT INTO `players` (`email`, `display_name`, `password`, `inventory`, `origin`) VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
            statement.setString(1, details.email)
            statement.setString(2, details.displayName) // Display name
            statement.setString(3, "") // Password
            statement.setString(4, "") // Inventory
            statement.setBoolean(5, true) // Origin
            statement.executeUpdate()
            val generatedKeys = statement.generatedKeys
            if (generatedKeys.next()) {
                val id = generatedKeys.getInt(1)
                statement.close()
                val player = createDefaultPlayerFrom(id, details.email, details.displayName, "")
                val dataMap = details.dataMap
                if (dataMap.isNotEmpty()) {
                    player.setPlayerDataBulk(dataMap)
                }
                return player
            } else {
                throw DatabaseException("Creating origin player failed. No id key was generated ")
            }
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in createOriginPlayer", e)
        }
    }

    override fun getOriginPlayer(token: String): Player? {
        val details = OriginDetailsRetriever.retrieve(token) ?: return null
        val existingPlayer = getExistingOriginPlayer(details)
        if (existingPlayer != null) return existingPlayer
        return createOriginPlayer(details)
    }

    private fun createDefaultPlayerFrom(id: Int, email: String, displayName: String, password: String): Player {
        return Player(
            playerId = id,
            email = email,
            displayName = displayName,
            password = password,
            sessionToken = null,
            credits = 0, creditsSpent = 0, gamesPlayed = 0, secondsPlayed = 0, inventory = "",
            faceCodes = "20;", newItem = "20;4;", csReward = 0, completion = null, progress = null,
            cscompletion = null, cstimestamps1 = null, cstimestamps2 = null, cstimestamps3 = null,
        )
    }

    override fun updatePlayerFully(player: Player) {
        try {
            val statement = connection.prepareStatement(
                """
                    |UPDATE `players`
                    |SET `session_token` = ?, `credits` = ?, `credits_spent` = ?,
                    |`games_played` = ?, `seconds_played` = ?, `inventory` = ?,
                    |`csreward` = ?, `face_codes` = ?, `new_item` = ?, `completion` = ?,
                    |`progress` = ?, `cs_completion` = ?, `cs_timestamps_1` = ?, `cs_timestamps_2` = ?,
                    |`cs_timestamps_3` = ?, `display_name` = ?
                    |WHERE `id` = ?
                """.trimMargin(),
            )
            statement.setString(1, player.getNullableSessionToken())
            statement.setInt(2, player.credits)
            statement.setInt(3, player.creditsSpent)
            statement.setInt(4, player.gamesPlayed)
            statement.setLong(5, player.secondsPlayed)
            statement.setString(6, player.inventory)
            statement.setInt(7, player.csReward)
            statement.setString(8, player.faceCodes)
            statement.setString(9, player.newItem)
            statement.setString(10, player.completion)
            statement.setString(11, player.progress)
            statement.setString(12, player.cscompletion)
            statement.setString(13, player.cstimestamps1)
            statement.setString(14, player.cstimestamps2)
            statement.setString(15, player.cstimestamps3)
            statement.setString(16, player.displayName)
            statement.setInt(17, player.playerId)
            statement.executeUpdate()
            statement.close()
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in updatePlayerFully", e)
        }
    }

    private fun hasPlayerClass(player: Player, index: Int): Boolean {
        val statement = connection.prepareStatement(
            "SELECT `id` FROM `player_classes` WHERE `player_id` = ? AND `index` = ?"
        )
        statement.setInt(1, player.playerId)
        statement.setInt(2, index)
        val resultSet = statement.executeQuery()
        val result = resultSet.next()
        statement.close()
        return result
    }

    override fun setPlayerClass(player: Player, playerClass: PlayerClass) {
        try {
            val hasExistingClass = hasPlayerClass(player, playerClass.index)
            if (hasExistingClass) {
                val statement = connection.prepareStatement(
                    "UPDATE `player_classes` SET `name` = ?, `level` = ?, `exp` = ?, `promotions` = ? WHERE `player_id` = ? AND `index` = ?"
                )
                statement.setString(1, playerClass.name)
                statement.setInt(2, playerClass.level)
                statement.setFloat(3, playerClass.exp)
                statement.setInt(4, playerClass.promotions)
                statement.setInt(5, player.playerId)
                statement.setInt(6, playerClass.index)
                statement.executeUpdate()
                statement.close()
            } else {
                val statement = connection.prepareStatement(
                    "INSERT INTO `player_classes` (`player_id`, `index`, `name`, `level`, `exp`, `promotions`) VALUES (?, ?, ?, ?, ?, ?)"
                )
                statement.setInt(1, player.playerId)
                statement.setInt(2, playerClass.index)
                statement.setString(3, playerClass.name)
                statement.setInt(4, playerClass.level)
                statement.setFloat(5, playerClass.exp)
                statement.setInt(6, playerClass.promotions)
                statement.executeUpdate()
                statement.close()
            }
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in setPlayerClass", e)
        }
    }

    private fun hasPlayerCharacter(player: Player, index: Int): Boolean {
        val statement = connection.prepareStatement("SELECT `id` FROM `player_characters` WHERE `player_id` = ? AND `index` = ?")
        statement.setInt(1, player.playerId)
        statement.setInt(2, index)
        val resultSet = statement.executeQuery()
        val result = resultSet.next()
        statement.close()
        return result
    }

    override fun setPlayerCharacter(player: Player, playerCharacter: PlayerCharacter) {
        try {
            val hasExistingCharacter = hasPlayerCharacter(player, playerCharacter.index)
            if (hasExistingCharacter) {
                val statement = connection.prepareStatement(
                    """
                    |UPDATE `player_characters`
                    |SET `kit_name` = ?, `name` = ?, `tint1` = ?, `tint2` = ?,
                    |`pattern` = ?, `pattern_color` = ?, `phong` = ?, `emissive` = ?,
                    |`skin_tone` = ?, `seconds_played` = ?, `timestamp_year` = ?,
                    |`timestamp_month` = ?, `timestamp_day` = ?, `timestamp_seconds` = ?,
                    |`powers` = ?, `hotkeys` = ?, `weapons` = ?, `weapon_mods` = ?,
                    |`deployed` = ?, `leveled_up` = ?
                    |WHERE `player_id` = ? AND `index` = ?
                    """.trimMargin()
                )
                statement.setString(1, playerCharacter.kitName)
                statement.setString(2, playerCharacter.name)
                statement.setInt(3, playerCharacter.tint1)
                statement.setInt(4, playerCharacter.tint2)
                statement.setInt(5, playerCharacter.pattern)
                statement.setInt(6, playerCharacter.patternColor)
                statement.setInt(7, playerCharacter.phong)
                statement.setInt(8, playerCharacter.emissive)
                statement.setInt(9, playerCharacter.skinTone)
                statement.setLong(10, playerCharacter.secondsPlayed)
                statement.setInt(11, playerCharacter.timestampYear)
                statement.setInt(12, playerCharacter.timestampMonth)
                statement.setInt(13, playerCharacter.timestampDay)
                statement.setInt(14, playerCharacter.timestampSeconds)
                statement.setString(15, playerCharacter.powers)
                statement.setString(16, playerCharacter.hotkeys)
                statement.setString(17, playerCharacter.weapons)
                statement.setString(18, playerCharacter.weaponMods)
                statement.setBoolean(19, playerCharacter.deployed)
                statement.setBoolean(20, playerCharacter.leveledUp)
                statement.setInt(21, player.playerId)
                statement.setInt(22, playerCharacter.index)
                statement.executeUpdate()
                statement.close()
            } else {
                val statement = connection.prepareStatement(
                    """
                        |INSERT INTO `player_characters` 
                        |(
                        |     `player_id`, `index`, `kit_name`, `name`, `tint1`, `tint2`,
                        |     `pattern`, `pattern_color`, `phong`, `emissive`, `skin_tone`, 
                        |     `seconds_played`, `timestamp_year`, `timestamp_month`, `timestamp_day`, 
                        |     `timestamp_seconds`, `powers`, `hotkeys`, `weapons`, `weapon_mods`, `deployed`, 
                        |     `leveled_up`
                        |)
                        | VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimMargin()
                )
                statement.setInt(1, player.playerId)
                statement.setInt(2, playerCharacter.index)
                statement.setString(3, playerCharacter.kitName)
                statement.setString(4, playerCharacter.name)
                statement.setInt(5, playerCharacter.tint1)
                statement.setInt(6, playerCharacter.tint2)
                statement.setInt(7, playerCharacter.pattern)
                statement.setInt(8, playerCharacter.patternColor)
                statement.setInt(9, playerCharacter.phong)
                statement.setInt(10, playerCharacter.emissive)
                statement.setInt(11, playerCharacter.skinTone)
                statement.setLong(12, playerCharacter.secondsPlayed)
                statement.setInt(13, playerCharacter.timestampYear)
                statement.setInt(14, playerCharacter.timestampMonth)
                statement.setInt(15, playerCharacter.timestampDay)
                statement.setInt(16, playerCharacter.timestampSeconds)
                statement.setString(17, playerCharacter.powers)
                statement.setString(18, playerCharacter.hotkeys)
                statement.setString(19, playerCharacter.weapons)
                statement.setString(20, playerCharacter.weaponMods)
                statement.setBoolean(21, playerCharacter.deployed)
                statement.setBoolean(22, playerCharacter.leveledUp)
                statement.executeUpdate()
                statement.close()
            }
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in setPlayerClass", e)
        }
    }

    override fun setPlayerSessionToken(player: Player, sessionToken: String) {
        try {
            val statement = connection.prepareStatement("UPDATE `players` SET `session_token` = ? WHERE `id` = ?")
            statement.setString(1, sessionToken)
            statement.setInt(2, player.playerId)
            statement.executeUpdate()
            statement.close()
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in setPlayerSessionToken", e)
        }
    }

    override fun setUpdatedPlayerData(player: Player, key: String) {
        try {
            when (key) {
                "Base" -> {
                    val statement = connection.prepareStatement(
                        """
                    |UPDATE `players` SET 
                    |`credits` = ?, `credits_spent` = ?, 
                    |`games_played` = ?, `seconds_played` = ?,
                    |`inventory` = ?
                    |WHERE `id` = ?
                    """.trimMargin()
                    )
                    statement.setInt(1, player.credits)
                    statement.setInt(2, player.creditsSpent)
                    statement.setInt(3, player.gamesPlayed)
                    statement.setLong(4, player.secondsPlayed)
                    statement.setString(5, player.inventory)
                    statement.setInt(6, player.playerId)
                    statement.executeUpdate()
                    statement.close()
                }

                "FaceCodes" -> {
                    val statement = connection.prepareStatement("UPDATE `players` SET `face_codes` = ? WHERE `id` = ?")
                    statement.setString(1, player.faceCodes)
                    statement.setInt(2, player.playerId)
                    statement.executeUpdate()
                    statement.close()
                }

                "NewItem" -> {
                    val statement = connection.prepareStatement("UPDATE `players` SET `new_item` = ? WHERE `id` = ?")
                    statement.setString(1, player.newItem)
                    statement.setInt(2, player.playerId)
                    statement.executeUpdate()
                    statement.close()
                }
                // (Possible name is Challenge Selected Reward)
                "csreward" -> {
                    val statement = connection.prepareStatement("UPDATE `players` SET `csreward` = ? WHERE `id` = ?")
                    statement.setInt(1, player.csReward)
                    statement.setInt(2, player.playerId)
                    statement.executeUpdate()
                    statement.close()
                }

                "Completion" -> {
                    val statement = connection.prepareStatement("UPDATE `players` SET `completion` = ? WHERE `id` = ?")
                    statement.setString(1, player.completion)
                    statement.setInt(2, player.playerId)
                    statement.executeUpdate()
                    statement.close()
                }

                "Progress" -> {
                    val statement = connection.prepareStatement("UPDATE `players` SET `progress` = ? WHERE `id` = ?")
                    statement.setString(1, player.progress)
                    statement.setInt(2, player.playerId)
                    statement.executeUpdate()
                    statement.close()
                }

                "cscompletion" -> {
                    val statement = connection.prepareStatement("UPDATE `players` SET `cs_completion` = ? WHERE `id` = ?")
                    statement.setString(1, player.cscompletion)
                    statement.setInt(2, player.playerId)
                    statement.executeUpdate()
                    statement.close()
                }

                "cstimestamps" -> {
                    val statement = connection.prepareStatement("UPDATE `players` SET `cs_timestamps_1` = ? WHERE `id` = ?")
                    statement.setString(1, player.cstimestamps1)
                    statement.setInt(2, player.playerId)
                    statement.executeUpdate()
                    statement.close()
                }

                "cstimestamps2" -> {
                    val statement = connection.prepareStatement("UPDATE `players` SET `cs_timestamps_2` = ? WHERE `id` = ?")
                    statement.setString(1, player.cstimestamps2)
                    statement.setInt(2, player.playerId)
                    statement.executeUpdate()
                    statement.close()
                }

                "cstimestamps3" -> {
                    val statement = connection.prepareStatement("UPDATE `players` SET `cs_timestamps_3` = ? WHERE `id` = ?")
                    statement.setString(1, player.cstimestamps3)
                    statement.setInt(2, player.playerId)
                    statement.executeUpdate()
                    statement.close()
                }
            }
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in setUpdatedPlayerData", e)
        }
    }


    private fun hasGalaxyAtWarData(player: Player): Boolean {
        val statement = connection.prepareStatement("SELECT `id` FROM player_gaw WHERE `player_id` = ?")
        statement.setInt(1, player.playerId)
        val resultSet = statement.executeQuery()
        val result = resultSet.next()
        statement.close()
        return result
    }

    private fun PreparedStatement.setGalaxyAtWar(start: Int, galaxyAtWarData: GalaxyAtWarData) {
        setLong(start, galaxyAtWarData.lastModified)
        setInt(start + 1, galaxyAtWarData.groupA)
        setInt(start + 2, galaxyAtWarData.groupB)
        setInt(start + 3, galaxyAtWarData.groupC)
        setInt(start + 4, galaxyAtWarData.groupD)
        setInt(start + 5, galaxyAtWarData.groupE)
    }

    override fun setGalaxyAtWarData(player: Player, galaxyAtWarData: GalaxyAtWarData) {
        try {
            val hasData = hasGalaxyAtWarData(player)
            if (hasData) {
                val statement = connection.prepareStatement(
                    """
                       UPDATE `player_gaw` SET 
                       `last_modified` = ?, 
                       `group_a` = ?, 
                       `group_b` = ?, 
                       `group_c` = ? , 
                       `group_d` = ? , 
                       `group_e` = ?
                       WHERE `player_id` = ?
                    """.trimIndent()
                )
                statement.setGalaxyAtWar(1, galaxyAtWarData)
                statement.setInt(7, player.playerId)
                statement.executeUpdate()
                statement.close()
            } else {
                val statement = connection.prepareStatement(
                    """
                    INSERT INTO `player_gaw` (
                        `player_id`, 
                        `last_modified`, 
                        `group_a`,
                        `group_b`, 
                        `group_c`, 
                        `group_d`, 
                        `group_e`
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                )
                statement.setInt(1, player.playerId)
                statement.setGalaxyAtWar(2, galaxyAtWarData)
                statement.executeUpdate()
                statement.close()
            }
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in setGalaxyAtWarData", e)
        }
    }
}
