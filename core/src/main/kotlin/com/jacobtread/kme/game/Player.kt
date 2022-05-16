package com.jacobtread.kme.game

import com.jacobtread.kme.database.repos.PlayerNotFoundException
import com.jacobtread.kme.database.repos.PlayersRepository
import com.jacobtread.kme.database.repos.ServerErrorException
import com.jacobtread.kme.utils.compareHashPassword
import io.netty.buffer.Unpooled

data class Player(
    val id: Long,
    val email: String,
    val displayName: String,
    val sessionToken: String?,
    val password: String,
    val settings: HashMap<String, String>,
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

    data class PlayerClass(
        val index: Int,
        val name: String,
        val level: Int,
        val exp: Float,
        val promotions: Int,
    ) {
        override fun toString(): String {
            val builder = StringBuilder()
            builder.append("class").append(index)
                .append("=20;4;")
                .append(name).append(';')
                .append(level).append(';')
                .append(exp).append(';')
                .append(promotions)
            return builder.toString()
        }
    }

    fun loadClasses(value: String): Array<PlayerClass> {
        return emptyArray()
    }

    data class PlayerCharacter(
        val index: Int,
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
            builder.append("char").append(index)
                .append("=20;4;")
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