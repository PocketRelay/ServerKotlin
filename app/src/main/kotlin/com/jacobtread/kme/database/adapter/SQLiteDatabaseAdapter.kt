package com.jacobtread.kme.database.adapter

import com.jacobtread.kme.database.data.GalaxyAtWarData
import com.jacobtread.kme.database.data.Player
import com.jacobtread.kme.database.data.PlayerCharacter
import com.jacobtread.kme.database.data.PlayerClass

class SQLiteDatabaseAdapter : DatabaseAdapter {
    private fun createDatabases() {
    }

    override fun setup() {
        TODO("Not yet implemented")
    }

    override fun isPlayerEmailTaken(email: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getPlayerByEmail(email: String): Player? {
        TODO("Not yet implemented")
    }

    override fun getPlayerBySessionToken(sessionToken: String): Player? {
        TODO("Not yet implemented")
    }

    override fun getPlayerClasses(player: Player): MutableList<PlayerClass> {
        TODO("Not yet implemented")
    }

    override fun getPlayerCharacters(player: Player): MutableList<PlayerCharacter> {
        TODO("Not yet implemented")
    }

    override fun getGalaxyAtWarData(player: Player): GalaxyAtWarData? {
        TODO("Not yet implemented")
    }

    override fun createPlayer(email: String, hashedPassword: String): Player {
        TODO("Not yet implemented")
    }

    override fun updatePlayerFully(player: Player) {
        TODO("Not yet implemented")
    }

    override fun setPlayerClass(player: Player, playerClass: PlayerClass) {
        TODO("Not yet implemented")
    }

    override fun setPlayerCharacter(player: Player, playerCharacter: PlayerCharacter) {
        TODO("Not yet implemented")
    }

    override fun setPlayerSessionToken(player: Player, sessionToken: String) {
        TODO("Not yet implemented")
    }

    override fun setUpdatedPlayerData(player: Player, key: String) {
        TODO("Not yet implemented")
    }

    override fun setGalaxyAtWarData(player: Player, galaxyAtWarData: GalaxyAtWarData) {
        TODO("Not yet implemented")
    }
}