package com.jacobtread.kme.database.entities

import com.jacobtread.kme.database.*
import com.jacobtread.kme.database.tables.PlayersTable
import com.jacobtread.kme.tools.hashPassword
import com.jacobtread.kme.tools.unixTimeSeconds
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction


class PlayerEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerEntity>(PlayersTable) {

        fun isEmailTaken(email: String): Boolean = transaction { !(find { PlayersTable.email eq email }.limit(1).empty()) }

        fun getByEmail(email: String): PlayerEntity? = transaction {
            find { PlayersTable.email eq email }
                .limit(1)
                .firstOrNull()
        }

        fun byEmail(email: String): PlayerEntity? {
            return firstOrNullSafe { PlayersTable.email eq email }
        }

        fun bySessionToken(sessionToken: String): PlayerEntity? {
            return firstOrNullSafe { PlayersTable.sessionToken eq sessionToken }

        }

        fun create(email: String, password: String): PlayerEntity {
            val hashedPassword = hashPassword(password)
            return transaction {
                PlayerEntity.new {
                    this.email = email
                    this.displayName = email
                    this.password = hashedPassword
                }
            }
        }

        fun createSerializableList(offset: Int, limit: Int): List<Serial> {
            return transaction {
                all()
                    .limit(limit, (offset * limit).toLong())
                    .map {
                        Serial(
                            it.playerId,
                            it.email,
                            it.displayName,
                            it.getSettingsBase()
                        )
                    }
            }
        }

    }

    val playerId: Int get() = id.value

    var email by PlayersTable.email
    var displayName by PlayersTable.displayName
    var password by PlayersTable.password

    private var _sessionToken by PlayersTable.sessionToken
    var settingsBase by PlayersTable.settingsBase

    private val classes by PlayerClass referrersOn PlayerClasses.player
    private val characters by PlayerCharacter referrersOn PlayerCharacters.player
    private val settings by PlayerSetting referrersOn PlayerSettings.player

    val galaxyAtWar: PlayerGalaxyAtWar get() {
        val existing = PlayerGalaxyAtWar.firstOrNullSafe { PlayerGalaxyAtWars.player eq this@PlayerEntity.id }
        if (existing != null) {
            existing.applyDecay()
            return existing
        }
        return PlayerGalaxyAtWar.create(this)
    }


    /**
     * createSessionToken Creates a randomly generated session token
     * with the length of 128 chars
     *
     * @return The created random session token
     */
    private fun createSessionToken(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPSQRSTUVWXYZ0123456789-"
        val output = StringBuilder()
        repeat(128) { output.append(chars.random()) }
        return output.toString()
    }

    /**
     * sessionToken Retrieves the current session token or creates a
     * new session token if there is not already one
     */
    val sessionToken: String
        get() {
            var sessionToken = _sessionToken
            if (sessionToken == null) sessionToken = createNewSessionToken()
            return sessionToken
        }

    fun createNewSessionToken(): String {
        val sessionToken = createSessionToken()
        transaction { _sessionToken = sessionToken }
        return sessionToken
    }

    /**
     * isSessionToken Checks if the provided token is the
     * players' session token
     *
     * @param token The token to check
     * @return Whether the tokens match
     */
    fun isSessionToken(token: String): Boolean = _sessionToken != null && token == _sessionToken

    /**
     * setSetting Updates a user setting. Settings that are parsed such
     * as classes, characters, and the base setting are handled separately
     * and other settings get their own rows in the setting table
     *
     * @param key The key of the setting
     * @param value The value of the setting
     */
    fun setSetting(key: String, value: String) {
        if (key.startsWith("class")) { // Class Setting
            val index = key.substring(5).toInt()
            PlayerClass.setClassFrom(this, index, value)
        } else if (key.startsWith("char")) { // Character Setting
            val index = key.substring(4).toInt()
            PlayerCharacter.setCharacterFrom(this, index, value)
        } else if (key == "Base") { // Base Setting
            transaction { settingsBase = value }
        } else { // Other Setting
            PlayerSetting.setSetting(this, key, value)
        }
    }


    /**
     * getSettingsBase Parses the base settings field and returns
     * it as a PlayerSettingsBase object if parsing fails then a
     * default PlayerSettingsBase is returned instead
     *
     * @return The player settings base
     */
    fun getSettingsBase(): PlayerSettingsBase {
        val base = settingsBase
        return if (base != null) PlayerSettingsBase.createFromValue(base) else PlayerSettingsBase()
    }

    /**
     * createSettingsMap Stores all the settings from this in a LinkedHashMap
     * so that they can be sent to the client
     *
     * @return
     */
    fun createSettingsMap(): LinkedHashMap<String, String> {
        val out = LinkedHashMap<String, String>()
        transaction {
            for (playerClass in classes) {
                out[playerClass.mapKey()] = playerClass.mapValue()
            }
            for (character in characters) {
                out[character.mapKey()] = character.mapValue()
            }
            for (setting in settings) {
                out[setting.key] = setting.value
            }
            settingsBase?.let { out["Base"] = it }
        }
        return out
    }

    /**
     * getN7Rating Produces a rating value based on the total
     * level and number of promotions this player has.
     *
     * @return The calculated N7 rating
     */
    fun getN7Rating(): Int {
        return transaction {
            var level = 0
            var promotions = 0
            for (playerClass in classes) {
                level += playerClass.level
                promotions += playerClass.promotions
            }
            level + promotions * 30
        }
    }

    fun getTotalPromotions(): Int {
        return transaction { classes.sumOf { it.promotions } }
    }

    @Serializable
    data class Serial(
        val id: Int,
        val email: String,
        val displayName: String,
        val settings: PlayerSettingsBase,
    )
}