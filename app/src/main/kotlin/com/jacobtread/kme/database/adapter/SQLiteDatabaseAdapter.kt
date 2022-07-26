package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.database.RuntimeDriver
import com.jacobtread.kme.database.data.GalaxyAtWarData
import com.jacobtread.kme.database.data.Player
import com.jacobtread.kme.database.data.PlayerCharacter
import com.jacobtread.kme.database.data.PlayerClass
import com.jacobtread.kme.exceptions.DatabaseException
import com.jacobtread.kme.utils.logging.Logger
import java.nio.file.Paths
import java.sql.*
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

class SQLiteDatabaseAdapter(file: String) : DatabaseAdapter {

    private val connection: Connection

    init {
        val version = "3.36.0.3"
        RuntimeDriver.createRuntimeDriver(
            "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/$version/sqlite-jdbc-$version.jar",
            "org.sqlite.JDBC",
            "sqlite.jar"
        )

        val path = Paths.get(file).absolute()
        val parentDir = path.parent
        if (parentDir.notExists()) parentDir.createDirectories()
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:$file")
        } catch (e: SQLException) {
            Logger.fatal("Unable to connect to SQLite database", e)
        }
    }

    private fun createDatabaseTables() {
        val tableSql = """
            | -- Players Table
            | CREATE TABLE IF NOT EXISTS `players`
            | (
            |     `id`              INTEGER
            |         CONSTRAINT players_pk
            |             PRIMARY KEY AUTOINCREMENT,
            |     `email`           TEXT NOT NULL,
            |     `display_name`    TEXT NOT NULL,
            |     `session_token`   TEXT    DEFAULT NULL,
            |     `password`        TEXT NOT NULL ,
            |     `credits`         INTEGER DEFAULT 0,
            |     `credits_spent`   INTEGER DEFAULT 0,
            |     `games_played`    INTEGER DEFAULT 0,
            |     `seconds_played`  INTEGER DEFAULT 0,
            |     `inventory`       TEXT    DEFAULT '',
            |     `csreward`        INTEGER DEFAULT 0,
            |     `face_codes`      TEXT    DEFAULT '20;',
            |     `new_item`        TEXT    DEFAULT '20;4;',
            |     `completion`      TEXT    DEFAULT NULL,
            |     `progress`        TEXT    DEFAULT NULL,
            |     `cs_completion`   TEXT    DEFAULT NULL,
            |     `cs_timestamps_1` TEXT    DEFAULT NULL,
            |     `cs_timestamps_2` TEXT    DEFAULT NULL,
            |     `cs_timestamps_3` TEXT    DEFAULT NULL
            | );
            | 
            | -- Player Classes Table
            | CREATE TABLE IF NOT EXISTS `player_classes`
            | (
            |     `id`         INTEGER
            |         CONSTRAINT player_classes_pk
            |             PRIMARY KEY AUTOINCREMENT,
            |     `player_id`  INTEGER
            |         constraint player_classes_players_id_fk
            |             references players (`id`) NOT NULL,
            |     `index`      INTEGER              NOT NULL,
            |     `name`       TEXT                 NOT NULL,
            |     `level`      INTEGER              NOT NULL,
            |     `exp`        REAL                 NOT NULL,
            |     `promotions` INTEGER              NOT NULL
            | );
            | 
            | -- Player Characters Table
            | CREATE TABLE IF NOT EXISTS `player_characters`
            | (
            |     `id`                INTEGER
            |         CONSTRAINT player_characters_pk
            |             PRIMARY KEY AUTOINCREMENT,
            |     `player_id`         INTEGER
            |         CONSTRAINT player_characters_players_id_fk
            |             REFERENCES players (`id`) NOT NULL,
            |     `index`             INTEGER       NOT NULL,
            |     `kit_name`          TEXT          NOT NULL,
            |     `name`              TEXT          NOT NULL,
            |     `tint1`             INTEGER       NOT NULL,
            |     `tint2`             INTEGER       NOT NULL,
            |     `pattern`           INTEGER       NOT NULL,
            |     `pattern_color`      INTEGER       NOT NULL,
            |     `phong`             INTEGER       NOT NULL,
            |     `emissive`          INTEGER       NOT NULL,
            |     `skin_tone`         INTEGER       NOT NULL,
            |     `seconds_played`    INTEGER       NOT NULL,
            | 
            |     `timestamp_year`    INTEGER       NOT NULL,
            |     `timestamp_month`   INTEGER       NOT NULL,
            |     `timestamp_day`     INTEGER       NOT NULL,
            |     `timestamp_seconds` INTEGER       NOT NULL,
            | 
            |     `powers`            TEXT          NOT NULL,
            |     `hotkeys`           TEXT          NOT NULL,
            |     `weapons`           TEXT          NOT NULL,
            |     `weapon_mods`       TEXT          NOT NULL,
            | 
            |     `deployed`          INTEGER       NOT NULL,
            |     `leveled_up`        INTEGER       NOT NULL
            | );
            | 
            | -- Galaxy At War Table
            | CREATE TABLE IF NOT EXISTS `player_gaw`
            | (
            |     `id`            INTEGER
            |         CONSTRAINT player_classes_pk
            |             PRIMARY KEY AUTOINCREMENT,
            |     `player_id`     INTEGER
            |         constraint player_classes_players_id_fk
            |             references players (`id`) NOT NULL,
            |     `last_modified` INTEGER           NOT NULL,
            |     `group_a`       INTEGER           NOT NULL,
            |     `group_b`       INTEGER           NOT NULL,
            |     `group_c`       INTEGER           NOT NULL,
            |     `group_d`       INTEGER           NOT NULL,
            |     `group_e`       INTEGER           NOT NULL
            | );
        """.trimMargin()
        try {
            val statement = connection.createStatement()
            statement.executeUpdate(tableSql)
        } catch (e: SQLException) {
            Logger.fatal("Failed to create database tables", e)
        }
    }

    override fun setup() {
        createDatabaseTables()
    }

    override fun isPlayerEmailTaken(email: String): Boolean {
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

    private fun getPlayerFromResultSet(resultSet: ResultSet): Player {
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

    override fun getPlayerById(id: Int): Player? {
        try {
            val statement = connection.prepareStatement("SELECT * FROM `players` WHERE `id` = ? LIMIT 1")
            statement.setInt(1, id)
            val resultSet = statement.executeQuery()
            if (!resultSet.next()) return null
            val player = getPlayerFromResultSet(resultSet)
            statement.close()
            return player
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in getPlayerById", e)
        }
    }

    override fun getPlayerByEmail(email: String): Player? {
        try {
            val statement = connection.prepareStatement("SELECT * FROM `players` WHERE `email` = ? LIMIT 1")
            statement.setString(1, email)
            val resultSet = statement.executeQuery()
            if (!resultSet.next()) return null
            val player = getPlayerFromResultSet(resultSet)
            statement.close()
            return player
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in getPlayerById", e)
        }
    }

    override fun getPlayerBySessionToken(sessionToken: String): Player? {
        try {
            val statement = connection.prepareStatement("SELECT * FROM `players` WHERE `session_token` = ? LIMIT 1")
            statement.setString(1, sessionToken)
            val resultSet = statement.executeQuery()
            if (!resultSet.next()) return null
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
                "INSERT INTO `players` (`email`, `display_name`, `password`) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
            statement.setString(1, email)
            statement.setString(2, email)
            statement.setString(3, hashedPassword)
            statement.executeUpdate()
            val generatedKeys = statement.generatedKeys
            if (generatedKeys.next()) {
                val id = generatedKeys.getInt("id")
                statement.close()
                return createDefaultPlayerFrom(id, email, hashedPassword)
            } else {
                throw DatabaseException("Creating player failed. No id key was generated ")
            }
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in createPlayer", e)
        }
    }

    private fun createDefaultPlayerFrom(id: Int, email: String, password: String): Player {
        return Player(
            playerId = id,
            email = email,
            displayName = email,
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
                    |`cs_timestamps_3` = ?
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
            statement.setInt(16, player.playerId)
            statement.executeUpdate()
            statement.close()
        } catch (e: SQLException) {
            throw DatabaseException("SQLException in updatePlayerFully", e)
        }
    }

    private fun hasPlayerClass(player: Player, index: Int): Boolean {
        val statement = connection.prepareStatement("SELECT `id` FROM `player_classes` WHERE `player_id` = ? AND `index` = ?")
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

    override fun setGalaxyAtWarData(player: Player, galaxyAtWarData: GalaxyAtWarData) {
        TODO("Not yet implemented")
    }
}