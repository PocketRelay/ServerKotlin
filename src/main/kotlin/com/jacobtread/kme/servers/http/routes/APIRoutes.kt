package com.jacobtread.kme.servers.http.routes

import com.jacobtread.kme.database.Player
import com.jacobtread.kme.database.PlayerSettingsBase
import com.jacobtread.kme.servers.http.router.*
import io.netty.handler.codec.http.HttpResponseStatus.OK
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction

fun RoutingGroup.routeGroupApi() {
    group("api") {
        routePlayersList()
        routePlayerSettings()
        routeUpdatePlayer()
    }
}

@Serializable
data class PlayerSerial(
    val id: Int,
    val email: String,
    val displayName: String,
    val settings: PlayerSettingsBase,
)

private fun RoutingGroup.routePlayersList() {
    get("players") {
        val limit = queryInt("limit", default = 10)
        val offset = queryInt("offset", default = 0)
        val playersList = transaction {
            Player.all()
                .limit(limit, (offset * limit).toLong())
                .map {
                    PlayerSerial(
                        it.playerId,
                        it.email,
                        it.displayName,
                        it.getSettingsBase()
                    )
                }
        }
        responseJson(playersList)
    }
}

private fun RoutingGroup.routePlayerSettings() {
    get("playerSettings") {
        val playerId = queryInt("id")
        val player = Player.getById(playerId) ?: throw BadRequestException()
        responseJson(player.createSettingsMap())
    }
    post("setPlayerSettings") {
        val playerId = queryInt("id")
        val player = Player.getById(playerId) ?: throw BadRequestException()
        val settings = contentJson<Map<String, String>>()
        settings.forEach { player.setSetting(it.key, it.value) }
        response(OK)
    }
}

private fun RoutingGroup.routeUpdatePlayer() {
    post("updatePlayer") {
        val player = contentJson<PlayerSerial>()
        val existing = Player.getById(player.id) ?: throw BadRequestException()
        transaction {
            existing.displayName = player.displayName
            existing.email = player.email
            if (existing.settingsBase != null) {
                val existingBase = existing.getSettingsBase()
                val clone = PlayerSettingsBase(
                    player.settings.credits,
                    existingBase.c,
                    existingBase.d,
                    player.settings.creditsSpent,
                    existingBase.e,
                    player.settings.gamesPlayed,
                    player.settings.secondsPlayed,
                    existingBase.f,
                    player.settings.inventory
                )
                existing.settingsBase = clone.mapValue()
            }
        }
        response(OK)
    }
}