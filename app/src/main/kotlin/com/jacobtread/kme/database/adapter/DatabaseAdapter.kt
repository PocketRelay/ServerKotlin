package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.database.data.GalaxyAtWarData
import com.jacobtread.kme.database.data.Player
import com.jacobtread.kme.database.data.PlayerCharacter
import com.jacobtread.kme.database.data.PlayerClass
import com.jacobtread.kme.exceptions.DatabaseException

interface DatabaseAdapter {

    @Throws(DatabaseException::class)
    fun setup()

    @Throws(DatabaseException::class)
    fun isPlayerEmailTaken(email: String): Boolean

    @Throws(DatabaseException::class)
    fun getPlayerById(id: Int): Player?

    @Throws(DatabaseException::class)
    fun getPlayerByEmail(email: String): Player?

    @Throws(DatabaseException::class)
    fun getPlayerBySessionToken(sessionToken: String): Player?

    @Throws(DatabaseException::class)
    fun getPlayerClasses(player: Player): MutableList<PlayerClass>

    @Throws(DatabaseException::class)
    fun getPlayerCharacters(player: Player): MutableList<PlayerCharacter>

    @Throws(DatabaseException::class)
    fun getGalaxyAtWarData(player: Player): GalaxyAtWarData?

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