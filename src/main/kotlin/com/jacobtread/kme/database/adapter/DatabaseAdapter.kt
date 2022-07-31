package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.database.data.GalaxyAtWarData
import com.jacobtread.kme.database.data.Player
import com.jacobtread.kme.database.data.PlayerCharacter
import com.jacobtread.kme.database.data.PlayerClass
import com.jacobtread.kme.exceptions.DatabaseException

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

    @Throws(DatabaseException::class)
    fun getPlayerClasses(player: Player): MutableList<PlayerClass>

    @Throws(DatabaseException::class)
    fun getPlayerCharacters(player: Player): MutableList<PlayerCharacter>

    @Throws(DatabaseException::class)
    fun getGalaxyAtWarData(player: Player): GalaxyAtWarData

    @Throws(DatabaseException::class)
    fun createPlayer(email: String, hashedPassword: String): Player

    @Throws(DatabaseException::class)
    fun updatePlayerFully(player: Player)

    @Throws(DatabaseException::class)
    fun setPlayerClass(player: Player, playerClass: PlayerClass)

    @Throws(DatabaseException::class)
    fun setPlayerCharacter(player: Player, playerCharacter: PlayerCharacter)

    @Throws(DatabaseException::class)
    fun setPlayerSessionToken(player: Player, sessionToken: String)

    @Throws(DatabaseException::class)
    fun setUpdatedPlayerData(player: Player, key: String)

    @Throws(DatabaseException::class)
    fun setGalaxyAtWarData(player: Player, galaxyAtWarData: GalaxyAtWarData)
}
