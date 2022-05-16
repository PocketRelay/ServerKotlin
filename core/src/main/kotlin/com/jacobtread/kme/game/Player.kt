package com.jacobtread.kme.game

import com.jacobtread.kme.database.repos.PlayerNotFoundException
import com.jacobtread.kme.database.repos.PlayersRepository
import com.jacobtread.kme.database.repos.ServerErrorException
import com.jacobtread.kme.utils.compareHashPassword
import io.netty.buffer.Unpooled

typealias SettingsMap = HashMap<String, String>

data class Player(
    val id: Long,
    val email: String,
    val displayName: String,
    val sessionToken: String?,
    val password: String,
    val settings: SettingsMap,
) {

    companion object {
        fun decodeSettings(bytes: ByteArray): HashMap<String, String> {
            if (bytes.size < 4) return HashMap()
            try {
                val buf = Unpooled.wrappedBuffer(bytes)
                val size = buf.readInt()
                val map = HashMap<String, String>(size)
                repeat(size) {
                    try {
                        val keySize = buf.readInt()
                        val keyBytes = ByteArray(keySize)
                        buf.readBytes(keyBytes)
                        val key = String(keyBytes, Charsets.UTF_8)

                        val valueSize = buf.readInt()
                        val valueBytes = ByteArray(valueSize)
                        buf.readBytes(valueBytes)
                        val value = String(valueBytes, Charsets.UTF_8)

                        map[key] = value
                    } catch (_: Throwable) {
                    }
                }
                return map
            } catch (e: Throwable) {
                return HashMap()
            }
        }
    }

    private var base: Base? = null
    private var classes: List<PlayerClass>? = null
    private var challengeStats: ChallengeStats? = null
    private var characters: List<PlayerCharacter>? = null

    fun encodeSettings(): ByteArray {
        val buf = Unpooled.buffer()
        buf.writeInt(settings.size)
        settings.forEach { (key, value) ->
            buf.writeInt(key.length)
            buf.writeBytes(key.toByteArray())
            buf.writeInt(value.length)
            buf.writeBytes(value.toByteArray())
        }
        val bytes = ByteArray(buf.readableBytes())
        buf.readBytes(bytes)
        return bytes
    }

    private fun getChallengeStats(): ChallengeStats {
        var challengeStats = this.challengeStats
        if (challengeStats != null) return challengeStats
        challengeStats = ChallengeStats.parse(settings)
        this.challengeStats = challengeStats
        return challengeStats
    }

    private fun getClasses(): List<PlayerClass> {
        var classes = this.classes
        if (classes != null) return classes
        classes = loadClasses()
        this.classes = classes
        return classes
    }

    fun getChallengePoints(): Int {
        val stats = getChallengeStats()
        if (stats.completion.size < 2) return 0
        return stats.completion[1]
    }

    fun getN7Rating(): Int {
        val classes = getClasses()
        val totalLevel = classes
            .sumOf { it.level }
        val totalPromotions = classes
            .sumOf { it.promotions }
            .times(30)
        return totalLevel + totalPromotions
    }

    fun updateSetting(key: String, value: String, repo: PlayersRepository) {
        try {
            settings[key] = value
            repo.updatePlayerSettings(this)
        } catch (e: PlayerNotFoundException) {
            e.printStackTrace()
        } catch (e: ServerErrorException) {
            e.printStackTrace()
        }
    }

    data class Base(
        val credits: Long = 0,
        val c: Int = -1,
        val d: Int = 0,
        val creditsSpent: Long = 0,
        val e: Int = 0,
        val gamesPlayed: Long,
        val secondsPlayed: Long,
        val f: Int = 0,
        val inventory: String,
    ) {
        override fun toString(): String {
            val builder = StringBuilder()
            builder.append("Base=20;4;")
                .append(credits).append(';')
                .append(c).append(';')
                .append(d).append(';')
                .append(creditsSpent).append(';')
                .append(e).append(';')
                .append(gamesPlayed).append(';')
                .append(secondsPlayed).append(';')
                .append(f).append(';')
                .append(inventory)
            return builder.toString()
        }
    }

    class ChallengeStats(
        val completion: IntArray,
        val progress: IntArray,
        val cscompletion: IntArray,
        val cstimestamps: IntArray,
        val cstimestamps2: IntArray,
        val cstimestamps3: IntArray,
    ) {

        companion object {
            fun parse(values: SettingsMap): ChallengeStats {
                val completion = parseIntArray(values["Completion"])
                val progress = parseIntArray(values["Progress"])
                val cscompletion = parseIntArray(values["cscompletion"])
                val cstimestamps = parseIntArray(values["cstimestamps"])
                val cstimestamps2 = parseIntArray(values["cstimestamps2"])
                val cstimestamps3 = parseIntArray(values["cstimestamps3"])
                return ChallengeStats(
                    completion, progress, cscompletion,
                    cstimestamps, cstimestamps2, cstimestamps3
                )
            }

            private fun parseIntArray(value: String?): IntArray {
                return if (value == null) {
                    IntArray(0)
                } else {
                    val parts = value.split(',')
                    val out = IntArray(parts.size)
                    parts.forEachIndexed { index, value ->
                        out[index] = value.toInt()
                    }
                    out
                }
            }
        }

    }

    data class PlayerClass(
        val name: String,
        val level: Int,
        val exp: Float,
        val promotions: Int,
    ) {
        companion object {
            val CLASS_NAMES = arrayOf(
                "Adept",
                "Soldier",
                "Engineer",
                "Sentinel",
                "Infiltrator",
                "Vanguard"
            )

            fun parse(value: String): PlayerClass {
                val parts = value.split(";", limit = 6)
                require(parts.size == 6) { "Invalid player class" }
                val name = parts[2]
                val level = parts[3].toIntOrNull() ?: 1
                val exp = parts[4].toFloatOrNull() ?: 0f
                val promotions = parts[5].toIntOrNull() ?: 0
                return PlayerClass(name, level, exp, promotions)
            }
        }

        override fun toString(): String {
            val builder = StringBuilder()
                .append("20;4;")
                .append(name).append(';')
                .append(level).append(';')
                .append(exp).append(';')
                .append(promotions)
            return builder.toString()
        }
    }

    private fun loadClasses(): List<PlayerClass> {
        val out = ArrayList<PlayerClass>(6)
        for (i in 1..6) {
            val clazz = settings["class$i"] ?: break
            out.add(PlayerClass.parse(clazz))
        }
        return out
    }

    data class PlayerCharacter(
        val kitName: String,
        val characterName: String,
        val tint1: Int,
        val tint2: Int,
        val pattern: Int,
        val patternColor: Int,
        val phong: Int,
        val emissive: Int,
        val skinTone: Int,
        val secondsPlayed: Long,
        val timeStampYear: Int,
        val timeStampMonth: Int,
        val timeStampDay: Int,
        val timeStampSeconds: Int,
        val powers: String,
        val hotkeys: String,
        val weapons: String,
        val weaponMods: String,
        val deployed: Boolean,
        val leveledUp: Boolean,
    ) {
        override fun toString(): String {
            val builder = StringBuilder()
            builder.append("20;4;")
                .append(kitName).append(';')
                .append(characterName).append(';')
                .append(tint1).append(';')
                .append(tint2).append(';')
                .append(pattern).append(';')
                .append(patternColor).append(';')
                .append(phong).append(';')
                .append(emissive).append(';')
                .append(skinTone).append(';')
                .append(secondsPlayed).append(';')
                .append(timeStampYear).append(';')
                .append(timeStampMonth).append(';')
                .append(timeStampDay).append(';')
                .append(timeStampSeconds).append(';')
                .append(powers).append(';')
                .append(hotkeys).append(';')
                .append(weapons).append(';')
                .append(weaponMods).append(';')
                .append(if (deployed) "True" else "False").append(';')
                .append(if (leveledUp) "True" else "False")
            return builder.toString()
        }
    }


    fun isMatchingPassword(value: String): Boolean {
        return compareHashPassword(value, this.password)
    }

    fun setSettings(settings: MutableMap<String, String>) {
        this.settings.clear()
        this.settings.putAll(settings)
    }

}