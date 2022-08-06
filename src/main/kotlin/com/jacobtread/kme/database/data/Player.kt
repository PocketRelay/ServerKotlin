package com.jacobtread.kme.database.data

import com.jacobtread.kme.Environment
import com.jacobtread.kme.utils.MEStringParser
import com.jacobtread.kme.utils.comparePasswordHash

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
    val displayName: String,
    private val password: String,
    private var sessionToken: String?,
    /**
     * The total number of usable credits that this player has
     */
    var credits: Int,
    /**
     * The total number of credits that this player has spent
     */
    var creditsSpent: Int,
    /**
     * The total number of games that this player has played
     */
    var gamesPlayed: Int,
    /**
     * The total number of seconds that this player has spent
     * inside of games.
     */
    var secondsPlayed: Long,
    /**
     * List of values representing the amount/level of each
     * inventory item this player has.
     */
    var inventory: String,

    var faceCodes: String?,
    var newItem: String?,

    /**
     * The challenge reward banner to display behind the player profile
     * see the known values listed in [com.jacobtread.kme.data.constants.ChallengeRewards]
     */
    var csReward: Int,

    var completion: String?,
    var progress: String?,
    var cscompletion: String?,
    var cstimestamps1: String?,
    var cstimestamps2: String?,
    var cstimestamps3: String?,
) {

    init {
        // makeGod()
    }

    fun makeGod() {
        inventory = "F".repeat(1342)
        credits = Int.MAX_VALUE - (Int.MAX_VALUE / 24)
        csReward = 154
        val completionBuilder = StringBuilder("22")
        repeat(221) { completionBuilder.append(",255") }
        completion = completionBuilder.toString()
        Environment.database.updatePlayerFully(this)
        val classes = Environment.database.getPlayerClasses(this)
        classes.forEach { playerClass ->
            playerClass.level = 20
            playerClass.promotions = 200
            Environment.database.setPlayerClass(this, playerClass)
        }

        val characters = Environment.database.getPlayerCharacters(this)
        characters.forEach { playerCharacter ->
            val powers = playerCharacter.getParsedPowers()
            if (powers.isNotEmpty()) {
                powers.forEach {
                    it.level = 6.000f
                    it.rank4 = 3
                    it.rank5 = 3
                    it.rank6 = 3
                }
                playerCharacter.setPowersFromParsed(powers)
                Environment.database.setPlayerCharacter(this, playerCharacter)
            }
        }
    }


    fun getGalaxyAtWarData(): GalaxyAtWarData {
        val value = Environment.database.getGalaxyAtWarData(this)
        value.applyDecay()
        if (value.isModified) {
            Environment.database.setGalaxyAtWarData(this, value)
        }
        return value
    }

    fun getSessionToken(): String {
        var sessionToken = sessionToken
        if (sessionToken == null) {
            val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPSQRSTUVWXYZ0123456789-"
            val output = StringBuilder()
            repeat(128) { output.append(chars.random()) }
            sessionToken = output.toString()
            Environment.database.setPlayerSessionToken(this, sessionToken)
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

    fun getTotalPromotions(): Int {
        val classes = Environment.database.getPlayerClasses(this)
        return classes.sumOf { it.promotions }
    }

    fun getN7Rating(): Int {
        val classes = Environment.database.getPlayerClasses(this)
        var level = 0
        var promotions = 0
        classes.forEach {
            level += it.level
            promotions += it.promotions
        }
        return level + promotions * 30
    }

    fun createSettingsMap(): LinkedHashMap<String, String> {
        val out = LinkedHashMap<String, String>()

        val classes = Environment.database.getPlayerClasses(this)
        val characters = Environment.database.getPlayerCharacters(this)

        classes.forEach { out[it.getKey()] = it.toEncoded() }
        characters.forEach { out[it.getKey()] = it.toEncoded() }

        faceCodes?.apply { out["FaceCodes"] = this }
        newItem?.apply { out["NewItem"] = this }
        out["csreward"] = csReward.toString()

        completion?.apply { out["Completion"] = this }
        progress?.apply { out["Progress"] = this }
        cscompletion?.apply { out["cscompletion"] = this }
        cstimestamps1?.apply { out["cstimestamps"] = this }
        cstimestamps2?.apply { out["cstimestamps2"] = this }
        cstimestamps3?.apply { out["cstimestamps3"] = this }

        val settingsBase = StringBuilder("20;4;")
            .append(credits).append(";-1;0;")
            .append(creditsSpent).append(";0;")
            .append(gamesPlayed).append(';')
            .append(secondsPlayed).append(";0;")
            .append(inventory)
            .toString()
        out["Base"] = settingsBase

        return out
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

        val database = Environment.database
        database.updatePlayerFully(this)
        classes.forEach { database.setPlayerClass(this, it) }
        characters.forEach { database.setPlayerCharacter(this, it) }
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
            Environment.database.setPlayerClass(this, playerClass)
        } else if (key.startsWith("char")) {
            val playerCharacter = PlayerCharacter.createFromKeyValue(key, value)
            Environment.database.setPlayerCharacter(this, playerCharacter)
        } else {
            setPlayerDataOther(key, value)
            Environment.database.setUpdatedPlayerData(this, key)
        }
    }
}
