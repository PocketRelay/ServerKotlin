package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.database.data.GalaxyAtWarData
import com.jacobtread.kme.database.data.Player
import com.jacobtread.kme.database.data.PlayerCharacter
import com.jacobtread.kme.database.data.PlayerClass
import com.jacobtread.kme.exceptions.DatabaseException

/**
 * Database agnostic adapter to allow database-independ creation
 * and modification of data.
 *
 * This abstraction allows me to use  different kinds of databases
 * with this single implementation (e.g. SQL and NoSQL)
 *
 * Currently, the only implementations of this are
 * - [com.jacobtread.kme.database.adapter.sql.MySQLDatabaseAdapter]
 * - [com.jacobtread.kme.database.adapter.sql.SQLDatabaseAdapter]
 *
 * But more can be added later on
 */
interface DatabaseAdapter {

    /**
     * Called when the database is initialized. Should be used
     * to create database tables and set up the database
     */
    @Throws(DatabaseException::class)
    fun setup()

    /**
     * Checks the database to see if there are any players
     * with the provided email set. Used to check if the
     * email provided while creating an acocunt is unique
     *
     * @param email The email to check for
     * @return Whether the email was taken
     */
    @Throws(DatabaseException::class)
    fun isEmailTaken(email: String): Boolean

    /**
     * Retrieves a player from the database where the player
     * has the same ID as the provided ID. Will return null
     * if there is no player with that ID. Used for silent
     * authentication
     *
     * @param id The id of the player to find
     * @return The found player or null if none exist
     */
    @Throws(DatabaseException::class)
    fun getPlayerById(id: Int): Player?

    /**
     * Retrieves a player from the database where the player has
     * the same  email as the provided email. Will return null if
     * there are no players with that email. Used when logging in
     * using an email and password
     *
     * @param email The email of the player to find
     * @return The found player or null if none exist
     */
    @Throws(DatabaseException::class)
    fun getPlayerByEmail(email: String): Player?

    /**
     * Retrieves a player from the database where the player has
     * the same session token as the provided session token. Will
     * return null if there are no players with that session token.
     *
     * Used for resuming sessions using the player session token.
     *
     * @param sessionToken The session token of the player to find
     * @return The found player or null if none exist
     */
    @Throws(DatabaseException::class)
    fun getPlayerBySessionToken(sessionToken: String): Player?

    /**
     * Sets the value of the session token for the
     * provided player
     *
     * @param player The player to set the session token for
     * @param sessionToken The session token to set
     */
    @Throws(DatabaseException::class)
    fun setPlayerSessionToken(player: Player, sessionToken: String)

    /**
     * Updates specific portions of the player data using
     * the provided key. This key is the key provided to
     * the [Player.setPlayerData] function and the fields
     * updated in this function should be those which
     * were updated in that.
     *
     * @param player The player to update
     * @param key The key to use for updating
     */
    @Throws(DatabaseException::class)
    fun setUpdatedPlayerData(player: Player, key: String)

    /**
     * Creates a new player in the database that has the
     * provided email and hashed password. Returns the
     * newly created player object with its default values
     *
     * @param email The email for the player to use
     * @param hashedPassword The hashed password for the player
     * @return The created player
     */
    @Throws(DatabaseException::class)
    fun createPlayer(email: String, hashedPassword: String): Player

    /**
     * Handles retrieving an Origin player from the database.
     * Origin accounts require a connection to the official
     * server in order to figure out the account information.
     * This is because the token changes alot.
     *
     * @param token The origin token
     * @return The created player
     */
    @Throws(DatabaseException::class)
    fun getOriginPlayer(token: String): Player?

    /**
     * Updates all the mutable fields on the player object
     * in the database regardless of whether they have been
     * modified or not.
     *
     * @param player The player to update
     */
    @Throws(DatabaseException::class)
    fun updatePlayerFully(player: Player)

    /**
     * Retrieves a list of player classes for the provided
     * player from the database.
     *
     * @param player The player whose classes to retrieve
     * @return The list of player classes
     */
    @Throws(DatabaseException::class)
    fun getPlayerClasses(player: Player): MutableList<PlayerClass>

    /**
     * Sets the specific class for the provided player. This
     * will update the existing player class entries or create
     * new entries.
     *
     * @param player The player this class belongs to
     * @param playerClass The player class data
     */
    @Throws(DatabaseException::class)
    fun setPlayerClass(player: Player, playerClass: PlayerClass)

    /**
     * Retrieves a list of player characters for the
     * provided player from the database.
     *
     * @param player The player whose characters to retrieve
     * @return The list of player characters
     */
    @Throws(DatabaseException::class)
    fun getPlayerCharacters(player: Player): MutableList<PlayerCharacter>

    /**
     * Sets the specific character for the provided player. This
     * will update existing player character entries or create new
     * entries
     *
     * @param player The player the character belongs to
     * @param playerCharacter The player character data
     */
    @Throws(DatabaseException::class)
    fun setPlayerCharacter(player: Player, playerCharacter: PlayerCharacter)


    /**
     * Retrieves the Galaxy At War Data for the provided
     * player. It is expected that if this function doesn't
     * find any galaxy at war data that it instead should
     * create new galaxy at war data and store that using
     * [setGalaxyAtWarData]
     *
     * @param player The player to retrieve the galaxy at war data from
     * @return The existing galaxy at war data or a fresh instance
     */
    @Throws(DatabaseException::class)
    fun getGalaxyAtWarData(player: Player): GalaxyAtWarData

    /**
     * Sets the Galaxy At War Data for the provided player. This
     * updates the existing values in the database or creates a
     * new entry if none eixst.
     *
     * @param player The player this Galaxy At War data belongs to
     * @param galaxyAtWarData The galaxy at war data itself
     */
    @Throws(DatabaseException::class)
    fun setGalaxyAtWarData(player: Player, galaxyAtWarData: GalaxyAtWarData)
}
