package com.jacobtread.kme.servers.http.routes

import com.jacobtread.kme.database.Player
import com.jacobtread.kme.database.PlayerSettingsBase
import com.jacobtread.kme.servers.http.router.RoutingGroup
import com.jacobtread.kme.servers.http.router.group
import io.netty.handler.codec.http.HttpResponseStatus
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
    get("players") { _, request ->
        val limit = request.queryInt("limit", default = 10)
        val offset = request.queryInt("offset", default = 0)
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
        request.json(playersList)
    }
}

private fun RoutingGroup.routePlayerSettings() {
    get("playerSettings") { _, request ->
        val playerId = request.queryInt("id")
        val player = Player.getById(playerId)
            ?: return@get request.response(HttpResponseStatus.BAD_REQUEST)
        request.json(player.createSettingsMap())
    }

    post("setPlayerSettings") { _, request ->
        val playerId = request.queryInt("id")
        val player = Player.getById(playerId)
            ?: return@post request.response(HttpResponseStatus.BAD_REQUEST)
        val settings = request.contentJson<Map<String, String>>()
        settings?.forEach { player.setSetting(it.key, it.value) }
    }
}

private fun RoutingGroup.routeUpdatePlayer() {
    post("updatePlayer") { _, request ->
        val player = request.contentJson<PlayerSerial>()
            ?: return@post request.response(HttpResponseStatus.BAD_REQUEST)
        val existing = Player.getById(player.id)
            ?: return@post request.response(HttpResponseStatus.BAD_REQUEST)
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
        request.response(HttpResponseStatus.OK)
    }
}