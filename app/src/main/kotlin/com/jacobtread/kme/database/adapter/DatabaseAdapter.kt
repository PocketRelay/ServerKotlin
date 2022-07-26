package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.database.data.GalaxyAtWarData
import com.jacobtread.kme.database.data.Player
import com.jacobtread.kme.database.data.PlayerCharacter
import com.jacobtread.kme.database.data.PlayerClass

interface DatabaseAdapter {

    fun setup()

    fun isPlayerEmailTaken(email: String): Boolean
    fun getPlayerByEmail(email: String): Player?
    fun getPlayerBySessionToken(sessionToken: String): Player?
    fun getPlayerClasses(player: Player): MutableList<PlayerClass>
    fun getPlayerCharacters(player: Player): MutableList<PlayerCharacter>
    fun getGalaxyAtWarData(player: Player): GalaxyAtWarData?

    fun createPlayer(email: String, hashedPassword: String): Player

    fun updatePlayerFully(player: Player)

    fun setPlayerClass(player: Player, playerClass: PlayerClass)
    fun setPlayerCharacter(player: Player, playerCharacter: PlayerCharacter)
    fun setPlayerSessionToken(player: Player, sessionToken: String)
    fun setUpdatedPlayerData(player: Player, key: String)
    fun setGalaxyAtWarData(player: Player, galaxyAtWarData: GalaxyAtWarData)
}