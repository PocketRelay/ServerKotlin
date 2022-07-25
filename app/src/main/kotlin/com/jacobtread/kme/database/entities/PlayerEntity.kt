package com.jacobtread.kme.database.entities

import com.jacobtread.kme.database.firstOrNullSafe
import com.jacobtread.kme.database.tables.PlayerCharactersTable
import com.jacobtread.kme.database.tables.PlayerClassesTable
import com.jacobtread.kme.database.tables.PlayersTable
import com.jacobtread.kme.tools.MEStringParser
import com.jacobtread.kme.tools.hashPassword
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction

class PlayerEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerEntity>(PlayersTable) {

        fun isEmailTaken(email: String): Boolean = transaction { !(find { PlayersTable.email eq email }.limit(1).empty()) }

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

        fun createSerialList(offset: Int, limit: Int): List<Serial> {
            return transaction {
                all()
                    .limit(limit, (offset * limit).toLong())
                    .map { it.createSerial() }
            }
        }

    }

    val playerId: Int get() = id.value

    var email by PlayersTable.email
        private set

    var displayName by PlayersTable.displayName
        private set
    var password by PlayersTable.password

    private var _sessionToken by PlayersTable.sessionToken

    var credits by PlayersTable.credits
    var creditsSpent by PlayersTable.creditsSpent
    var gamesPlayed by PlayersTable.gamesPlayed
    var secondsPlayed by PlayersTable.secondsPlayed
    var inventory by PlayersTable.inventory

    var faceCodes by PlayersTable.faceCodes
    var newItem by PlayersTable.newItem
    var csReward by PlayersTable.challengeReward

    var completion by PlayersTable.completion
    var progress by PlayersTable.progress
    var cscompletion by PlayersTable.challengeCompletion
    var cstimestamps1 by PlayersTable.cstimestamps1
    var cstimestamps2 by PlayersTable.cstimestamps2
    var cstimestamps3 by PlayersTable.cstimestamps3

    private val classes by PlayerClassEntity referrersOn PlayerClassesTable.player
    private val characters by PlayerCharacterEntity referrersOn PlayerCharactersTable.player

    val galaxyAtWar: GalaxyAtWarEntity get() = GalaxyAtWarEntity.forPlayer(this)

    /**
     * Property which represents the total number of promotions that this player
     * entity has for each of its player classes. This is executed in a transaction
     */
    val totalPromotions: Int get() = transaction { classes.sumOf { it.promotions } }

    val n7Rating: Int
        get() = transaction {
            var level = 0
            var promotions = 0
            classes.forEach {
                level += it.level
                promotions += it.promotions
            }
            level + promotions * 30
        }

    private val settingsBase: String
        get() = StringBuilder("20;4;")
            .append(credits).append(";-1;0;")
            .append(creditsSpent).append(";0;")
            .append(gamesPlayed).append(';')
            .append(secondsPlayed).append(";0;")
            .append(inventory)
            .toString()


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
            if (sessionToken == null) { // If there is no session token create one
                sessionToken = createSessionToken()
                transaction { _sessionToken = sessionToken }
            }
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
            PlayerClassEntity.updateOrCreate(this, index, value)
        } else if (key.startsWith("char")) { // Character Setting
            val index = key.substring(4).toInt()
            PlayerCharacterEntity.updateOrCreate(this, index, value)
        } else {
            transaction {
                when (key) {
                    "Base" -> {
                        val parser = MEStringParser(value, 11)
                        credits = parser.int()
                        parser.skip(2) // Skip -1;0
                        creditsSpent = parser.int()
                        parser.skip(1)
                        gamesPlayed = parser.int()
                        secondsPlayed = parser.long()
                        parser.skip(1)
                        inventory = parser.str()
                    }
                    "FaceCodes" -> faceCodes = value
                    "NewItem" -> newItem = value
                    // (Possible name is Challenge Selected Reward)
                    "csreward" -> csReward = value.toIntOrNull() ?: 0
                    "Completion" -> completion = value
                    "Progress" -> progress = value
                    "cscompletion" -> cscompletion = value
                    "cstimestamps" -> cstimestamps1 = value
                    "cstimestamps2" -> cstimestamps2 = value
                    "cstimestamps3" -> cstimestamps3 = value
                }
            }
        }
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
                out[playerClass.key] = playerClass.toEncoded()
            }
            for (character in characters) {
                out[character.key] = character.toEncoded()
            }

            out["FaceCodes"] = faceCodes
            out["NewItem"] = newItem
            out["csreward"] = csReward.toString()

            completion?.apply { out["Completion"] = this }
            progress?.apply { out["Progress"] = this }
            cscompletion?.apply { out["cscompletion"] = this }
            cstimestamps1?.apply { out["cstimestamps"] = this }
            cstimestamps2?.apply { out["cstimestamps2"] = this }
            cstimestamps3?.apply { out["cstimestamps3"] = this }
            out["Base"] = settingsBase

        }
        return out
    }

    /**
     * Serializable representation of a player entity object
     * this contains the fields which should be serialized
     *
     * @see createSerial For creating this object from a player entity
     * @property id The id of the player entity
     * @property email The email of the player entity
     * @property displayName The display name of the player entity
     * @property credits The number of credits the player entity has
     * @property creditsSpend The number of credits the player entity has spent
     * @property gamesPlayed The number of games played by the player
     * @property secondsPlayed The number of seconds played by the player
     * @property inventory The encoded inventory string of the player
     * @constructor Create empty Serial
     */
    @Serializable
    data class Serial(
        val id: Int,
        val email: String,
        val displayName: String,
        val credits: Int,
        val creditsSpend: Int,
        val gamesPlayed: Int,
        val secondsPlayed: Long,
        val inventory: String,
    ) {
        /**
         * Sets all the properties from the serializable
         * player on the provided player entity. Runs in a
         * transaction so changes are present on the database
         *
         * @param playerEntity The player entity to apply to
         */
        fun apply(playerEntity: PlayerEntity) {
            transaction {
                playerEntity.displayName = displayName
                playerEntity.email = email
                playerEntity.credits = credits
                playerEntity.creditsSpent = creditsSpend
                playerEntity.gamesPlayed = gamesPlayed
                playerEntity.secondsPlayed = secondsPlayed
                playerEntity.inventory = inventory
            }
        }
    }

    /**
     * Creates a serializable player object for this
     * player entity so that it can be encoded into
     * a JSON format. This serial object only contains
     * properties that should be shared to other sources
     *
     * @return The created serial object
     */
    fun createSerial(): Serial = Serial(playerId, email, displayName, credits, creditsSpent, gamesPlayed, secondsPlayed, inventory)
}