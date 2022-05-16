package com.jacobtread.kme.database.repos

import com.jacobtread.kme.LOGGER
import com.jacobtread.kme.game.Player
import com.jacobtread.kme.utils.hashPassword
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

class PlayerCreationException(val reason: Reason, cause: Throwable? = null) : Exception(reason.toString(), cause) {
    enum class Reason {
        EMAIL_TAKEN,
        OTHER
    }
}

class PlayerNotFoundException : Exception()
class ServerErrorException(cause: Throwable? = null) : Exception(cause)

/**
 * PlayersRepository Represents a repository that is used to create, store,
 * search and update player/user data
 *
 * @constructor Create empty PlayersRepository
 */
abstract class PlayersRepository : DatabaseRepository() {

    /**
     * isDisplayNameTaken Database implementation for checking whether the provided
     * display name is already taken by another user account
     *
     * @param name The display name to check for
     * @return Whether the name is taken
     */
    abstract fun isDisplayNameTaken(name: String): Boolean

    /**
     * isEmailTaken Database implementation for checking whether the provided
     * email is already taken by another user account
     *
     * @param email The email to check for
     * @return Whether the email is taken
     */
    abstract fun isEmailTaken(email: String): Boolean

    /**
     * createPlayerInternal Database implementation of creating player wrapped by
     * createPlayer and shouldn't be called directly.
     *
     * @param email The email of the player account
     * @param displayName The display name of the player (username)
     * @param hashedPassword The hashed password
     * @return The ID of the newly created player
     */
    protected abstract fun createPlayerInternal(email: String, displayName: String, hashedPassword: String): Long


    /**
     * getPlayerByName Retrieves a player from the repository that has a matching
     * name to the provided display name
     *
     * @param displayName The name of the player to search for
     * @return The player that was found
     * @throws ServerErrorException Thrown when an exception was occurred between the server and database
     * @throws PlayerNotFoundException Thrown if the player doesn't exist
     */
    @Throws(ServerErrorException::class, PlayerNotFoundException::class)
    abstract fun getPlayerByName(displayName: String): Player

    /**
     * getPlayerByEmail Retrieves a player from the repository that has a matching
     * email to the provided display name
     *
     * @param email The email of the player to search for
     * @return The player that was found
     * @throws ServerErrorException Thrown when an exception was occurred between the server and database
     * @throws PlayerNotFoundException Thrown if the player doesn't exist
     */
    @Throws(ServerErrorException::class, PlayerNotFoundException::class)
    abstract fun getPlayerByEmail(email: String): Player

    /**
     * getPlayerByID Retrieves a player from the repository that has a matching
     * id to the provided display name
     *
     * @param id The id of the player to search for
     * @return The player that was found
     * @throws ServerErrorException Thrown when an exception was occurred between the server and database
     * @throws PlayerNotFoundException Thrown if the player doesn't exist
     */
    @Throws(ServerErrorException::class, PlayerNotFoundException::class)
    abstract fun getPlayerByID(id: Long): Player

    abstract fun setPlayerSessionToken(player: Player, token: String)

    /**
     * getPlayerByNameOrNull Uses getPlayerByName for underlying implementation
     * but returns null if any exceptions are caught
     *
     * @param displayName The name of the player to search for
     * @return The player that was found or null
     */
    fun getPlayerByNameOrNull(displayName: String): Player? {
        return try {
            getPlayerByName(displayName)
        } catch (_: Exception) {
            return null
        }
    }

    @Throws(ServerErrorException::class, PlayerNotFoundException::class)
    abstract fun getPlayerSettings(player: Player)

    @Throws(ServerErrorException::class, PlayerNotFoundException::class)
    abstract fun updatePlayerSettings(player: Player)

    /**
     * createPlayer Creates a new player using the underlying repository implementations
     * along with hashing the password and ensuring the account is unique and doesn't
     * already exist.
     *
     * @param email The email for this account
     * @param displayName The display name for this account
     * @param password The plain text password to use for this account (This will be hashed)
     * @throws PlayerCreationException Thrown if creation of the player fails
     */
    @Throws(PlayerCreationException::class)
    fun createPlayer(email: String, displayName: String, password: String): Player {
        try {
            if (isEmailTaken(email)) throw PlayerCreationException(PlayerCreationException.Reason.EMAIL_TAKEN)
            if (isDisplayNameTaken(displayName)) throw PlayerCreationException(PlayerCreationException.Reason.EMAIL_TAKEN)
            val hashed = hashPassword(password)
            val id = createPlayerInternal(email, displayName, hashed)
            return Player(id, email, displayName, null, hashed, HashMap())
        } catch (e: Exception) {
            if (e is PlayerCreationException) throw e
            throw PlayerCreationException(PlayerCreationException.Reason.OTHER, e)
        }
    }

    /**
     * getPlayerCount Counts the total number of players stored in this
     * database
     *
     * @return The number of players
     */
    abstract fun getPlayerCount(): Long

    /**
     * MySQL Defines the behavior for the players' repository behavior when using
     * a MySQL database. The SQLite database inherits its behavior from this
     *
     * @property connection
     * @constructor Create empty MySQL
     */
    open class MySQL(private val connection: Connection) : PlayersRepository() {

        /**
         * initQuery Abstraction of the query used to create the players'
         * database table. This is used to make to easier for the SQLite
         * adapter to use a different create query
         *
         * @return The string SQL query
         */
        open fun initQuery(): String = """
                CREATE TABLE IF NOT EXISTS `players` (
                    `id` INT(255) AUTO_INCREMENT,
                    `email` VARCHAR(254),
                    `display_name` VARCHAR(99),
                    `session_token` VARCHAR(128) DEFAULT NULL,
                    `password` VARCHAR(128) DEFAULT NULL,
                    `settings` BLOB,
                    UNIQUE KEY `id_index` (`id`) USING BTREE,
                    UNIQUE KEY `name_index` (`display_name`) USING BTREE,
                    PRIMARY KEY (`id`)
                );
            """.trimIndent()

        override fun init() {
            try {
                val statement = connection.createStatement()
                statement.execute(initQuery())
                LOGGER.info("Created players database table")
            } catch (e: SQLException) {
                LOGGER.fatal("Failed to create players database table", e)
            }
        }

        override fun isDisplayNameTaken(name: String): Boolean {
            val statement = connection.prepareStatement("SELECT `id` FROM `players` WHERE display_name = ? LIMIT 1")
            statement.setString(1, name)
            val resultSet = statement.executeQuery()
            return resultSet.next()
        }

        override fun isEmailTaken(email: String): Boolean {
            val statement = connection.prepareStatement("SELECT `id` FROM `players` WHERE email = ? LIMIT 1")
            statement.setString(1, email)
            val resultSet = statement.executeQuery()
            return resultSet.next()
        }

        override fun createPlayerInternal(email: String, displayName: String, hashedPassword: String): Long {
            val statement = connection.prepareStatement(
                "INSERT INTO `players` (`email`, `display_name`, `password`, `settings`) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )
            statement.setString(1, email)
            statement.setString(2, displayName)
            statement.setString(3, hashedPassword)
            statement.setBytes(4, ByteArray(0))
            statement.executeUpdate()
            val resultSet = statement.generatedKeys
            if (!resultSet.next()) throw IllegalStateException("Missing ID from created player")
            return resultSet.getLong(1)
        }

        override fun getPlayerSettings(player: Player) {
            try {
                val statement = connection.prepareStatement("SELECT `settings` FROM `players` WHERE `id` = ?")
                statement.setLong(1, player.id)
                val result = statement.executeQuery()
                if (!result.next()) throw PlayerNotFoundException()
                val stream = result.getBinaryStream("settings")
                val bytes = stream.readAllBytes()
                player.setSettings(Player.decodeSettings(bytes))
            } catch (e: SQLException) {
                throw ServerErrorException(e)
            }
        }

        override fun updatePlayerSettings(player: Player) {
            val settingsBytes = player.encodeSettings()
            val statement = connection.prepareStatement("UPDATE `players` SET `settings` = ? WHERE `id` = ?")
            statement.setBytes(1, settingsBytes)
            statement.setLong(2, player.id)
            statement.executeUpdate()
        }

        private fun getPlayerFromResult(result: ResultSet): Player {
            val stream = result.getBinaryStream("settings")
            val bytes = stream.readAllBytes()
            return Player(
                id = result.getLong("id"),
                email = result.getString("email"),
                displayName = result.getString("display_name"),
                sessionToken = result.getString("session_token"),
                password = result.getString("password"),
                settings = Player.decodeSettings(bytes)
            )
        }

        override fun getPlayerByName(displayName: String): Player {
            try {
                val statement = connection.prepareStatement("SELECT * FROM `players` WHERE `display_name` = ?")
                statement.setString(1, displayName)
                val result = statement.executeQuery()
                if (!result.next()) throw PlayerNotFoundException()
                return getPlayerFromResult(result)
            } catch (e: SQLException) {
                throw ServerErrorException(e)
            }
        }


        override fun getPlayerByEmail(email: String): Player {
            try {
                val statement = connection.prepareStatement("SELECT * FROM `players` WHERE `email` = ?")
                statement.setString(1, email)
                val result = statement.executeQuery()
                if (!result.next()) throw PlayerNotFoundException()
                return getPlayerFromResult(result)
            } catch (e: SQLException) {
                throw ServerErrorException(e)
            }
        }

        override fun getPlayerByID(id: Long): Player {
            try {
                val statement = connection.prepareStatement("SELECT * FROM `players` WHERE `id` = ?")
                statement.setLong(1, id)
                val result = statement.executeQuery()
                if (!result.next()) throw PlayerNotFoundException()
                return getPlayerFromResult(result)
            } catch (e: SQLException) {
                throw ServerErrorException(e)
            }
        }

        override fun setPlayerSessionToken(player: Player, token: String) {
            val statement = connection.prepareStatement("UPDATE `players` SET `session_token` = ? WHERE `id` = ?")
            statement.setString(1, token)
            statement.setLong(2, player.id)
            statement.executeUpdate()
        }

        override fun getPlayerCount(): Long {
            try {
                // If users are deleted this count becomes inaccurate
                // alternative would be:
                // SELECT COUNT(*) AS count FROM `players`
                // However this could be slower at larger user counts
                val statement = connection.prepareStatement("SELECT MAX(`id`) AS `count` FROM `players`")
                val result = statement.executeQuery()
                if (!result.next()) return 0
                return result.getLong("count")
            } catch (e: SQLException) {
                throw ServerErrorException(e)
            }
        }
    }

    /**
     * SQLite Defines the SQLite behavior for the players' repository. This
     * inherits all other query behavior from the MySQL repository excluding
     * the initialization query
     *
     * @property connection
     * @constructor Create empty SQLite
     */
    class SQLite(private val connection: Connection) : MySQL(connection) {
        override fun initQuery(): String =
            """
            CREATE TABLE IF NOT EXISTS `players` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE ,
                `email` TEXT UNIQUE ,
                `display_name` TEXT UNIQUE,
                `session_token` TEXT DEFAULT NULL,
                `password` TEXT DEFAULT NULL,
                `settings` BLOB
            );
            """.trimIndent()
    }
}
