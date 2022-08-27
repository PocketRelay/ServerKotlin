package com.jacobtread.relay.database.models

import com.jacobtread.relay.database.tables.GalaxyAtWarTable
import com.jacobtread.relay.database.tables.PlayerCharactersTable
import com.jacobtread.relay.database.tables.PlayerClassesTable
import com.jacobtread.relay.database.tables.PlayersTable
import com.jacobtread.relay.utils.MEStringParser
import com.jacobtread.relay.utils.comparePasswordHash
import com.jacobtread.relay.utils.generateRandomString
import kotlinx.serialization.Serializable
import java.util.concurrent.CompletableFuture as Future

@Serializable
data class Player(
    /**
     * The unique id for this player which uniquely identifies it amoungst
     * all the players in the datbase
     */
    val playerId: Int,
    /**
     * The email address used when created this account. In future this could be used
     * to send out emails for password resets and such.
     */
    val email: String,
    /**
     * The unique display name for this player. Currently, this
     * is the first 99 chars of the email until a system for
     * updating this is added.
     */
    var displayName: String,
    private val password: String,
    private var sessionToken: String? = null,
    /**
     * The total number of usable credits that this player has
     */
    var credits: Int = 0,
    /**
     * The total number of credits that this player has spent
     */
    var creditsSpent: Int = 0,
    /**
     * The total number of games that this player has played
     */
    var gamesPlayed: Int = 0,
    /**
     * The total number of seconds that this player has spent
     * inside of games.
     */
    var secondsPlayed: Long = 0,
    /**
     * List of values representing the amount/level of each
     * inventory item this player has.
     */
    var inventory: String = "",

    var faceCodes: String? = null,
    var newItem: String? = null,

    /**
     * The challenge reward banner to display behind the player profile
     * see the known values listed in [com.jacobtread.relay.data.constants.ChallengeRewards]
     */
    var csReward: Int = 0,

    var completion: String? = null,
    var progress: String? = null,
    var cscompletion: String? = null,
    var cstimestamps1: String? = null,
    var cstimestamps2: String? = null,
    var cstimestamps3: String? = null,
) {

    fun getGalaxyAtWarData(): Future<GalaxyAtWarData> {
        return GalaxyAtWarTable.getByPlayer(this)
            .thenApply { value ->
                value.applyDecay()
                if (value.isModified) {
                    GalaxyAtWarTable.setByPlayer(this, value)
                }
                value
            }
    }

    fun getSessionToken(): String {
        var sessionToken = sessionToken
        if (sessionToken == null) {
            sessionToken = generateRandomString(128)
            PlayersTable.setSessionToken(this, sessionToken)
        }
        return sessionToken
    }

    fun getNullableSessionToken(): String? = sessionToken

    fun isSessionToken(token: String): Boolean {
        return sessionToken != null && sessionToken == token
    }

    fun isMatchingPassword(password: String): Boolean {
        return comparePasswordHash(password, this.password)
    }

    fun getTotalPromotions(): Future<Int> {
        return PlayerClassesTable.getByPlayer(this)
            .thenApply { clases -> clases.sumOf { it.promotions } }
    }

    fun getN7Rating(): Future<Int> {
        return PlayerClassesTable.getByPlayer(this)
            .thenApply { classes ->
                var level = 0
                var promotions = 0
                classes.forEach {
                    level += it.level
                    promotions += it.promotions
                }
                level + promotions * 30
            }
    }

    class SettingsMapLoader(private val player: Player) {
        private val out = LinkedHashMap<String, String>()

        fun load(): Future<Map<String, String>> {
            val classesFuture = PlayerClassesTable.getByPlayer(player)
                .thenApply { classes -> classes.forEach { out[it.getKey()] = it.toEncoded() } }
            val charactersFuture = PlayerCharactersTable.getByPlayer(player)
                .thenApply { characters -> characters.forEach { out[it.getKey()] = it.toEncoded() } }
            val settingsBase = StringBuilder("20;4;")
                .append(player.credits).append(";-1;0;")
                .append(player.creditsSpent).append(";0;")
                .append(player.gamesPlayed).append(';')
                .append(player.secondsPlayed).append(";0;")
                .append(player.inventory)
                .toString()
            return Future.allOf(classesFuture, charactersFuture)
                .thenApply {
                    player.faceCodes?.apply { out["FaceCodes"] = this }
                    player.newItem?.apply { out["NewItem"] = this }
                    out["csreward"] = player.csReward.toString()

                    player.completion?.apply { out["Completion"] = this }
                    player.progress?.apply { out["Progress"] = this }
                    player.cscompletion?.apply { out["cscompletion"] = this }
                    player.cstimestamps1?.apply { out["cstimestamps"] = this }
                    player.cstimestamps2?.apply { out["cstimestamps2"] = this }
                    player.cstimestamps3?.apply { out["cstimestamps3"] = this }
                    out["Base"] = settingsBase
                    out
                }
        }

    }

    fun createSettingsMap(): Future<Map<String, String>> {
        val loader = SettingsMapLoader(this)
        return loader.load()
    }

    fun setPlayerDataBulk(map: Map<String, String>) {
        val classes = ArrayList<PlayerClass>()
        val characters = ArrayList<PlayerCharacter>()

        map.forEach { (key, value) ->
            if (key.startsWith("class")) {
                val playerClass = PlayerClass.createFromKeyValue(key, value)
                classes.add(playerClass)
            } else if (key.startsWith("char")) {
                val playerCharacter = PlayerCharacter.createFromKeyValue(key, value)
                characters.add(playerCharacter)
            } else {
                setPlayerDataOther(key, value)
            }
        }

        PlayersTable.setPlayerFully(this)
        classes.forEach { PlayerClassesTable.setByPlayer(this, it) }
        characters.forEach { PlayerCharactersTable.setByPlayer(this, it) }
    }

    private fun setPlayerDataOther(key: String, value: String) {
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

    fun setPlayerData(key: String, value: String) {
        if (key.startsWith("class")) {
            val playerClass = PlayerClass.createFromKeyValue(key, value)
            PlayerClassesTable.setByPlayer(this, playerClass)
        } else if (key.startsWith("char")) {
            val playerCharacter = PlayerCharacter.createFromKeyValue(key, value)
            PlayerCharactersTable.setByPlayer(this, playerCharacter)
        } else {
            setPlayerDataOther(key, value)
            PlayersTable.setPlayerPartial(this, key)
        }
    }
}
