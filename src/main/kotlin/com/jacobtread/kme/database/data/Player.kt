package com.jacobtread.kme.database.data

import com.jacobtread.kme.Environment
import com.jacobtread.kme.tools.MEStringParser
import com.jacobtread.kme.tools.comparePasswordHash

data class Player(
    val playerId: Int,
    val email: String,
    val displayName: String,
    private val password: String,
    private var sessionToken: String?,

    var credits: Int,

    var creditsSpent: Int,
    var gamesPlayed: Int,
    var secondsPlayed: Long,
    var inventory: String,

    var faceCodes: String?,
    var newItem: String?,
    var csReward: Int,

    var completion: String?,
    var progress: String?,
    var cscompletion: String?,
    var cstimestamps1: String?,
    var cstimestamps2: String?,
    var cstimestamps3: String?,
) {

    fun updateFully() {
        Environment.database.updatePlayerFully(this)
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

    fun setPlayerData(key: String, value: String) {
        if (key.startsWith("class")) {
            val playerClass = PlayerClass.createFromKeyValue(key, value)
            Environment.database.setPlayerClass(this, playerClass)
        } else if (key.startsWith("char")) {
            val playerCharacter = PlayerCharacter.createFromKeyValue(key, value)
            Environment.database.setPlayerCharacter(this, playerCharacter)
        } else {
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
            Environment.database.setUpdatedPlayerData(this, key)
        }
    }

}