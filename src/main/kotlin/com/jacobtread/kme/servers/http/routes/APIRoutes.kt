package com.jacobtread.kme.servers.http.routes

import com.jacobtread.kme.database.Player
import com.jacobtread.kme.database.PlayerSettingsBase
import com.jacobtread.kme.servers.http.router.*
import io.netty.handler.codec.http.HttpResponseStatus.OK
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * routeGroupApi Adds the routing for the API routes
 * (/api) these are the routes for the panel api
 *
 */
fun RoutingGroup.routeGroupApi() {
    group("api") {
        routePlayersList()
        routePlayerSettings()
        routeUpdatePlayer()
    }
}

/**
 * PlayerSerial Serialization object for serializing/deserializing
 * players as JSON objects to be sent/received over HTTP
 *
 * @property id The id of the player
 * @property email The email of the player
 * @property displayName The display name of the player
 * @property settings The settings of the player
 * @constructor Create empty PlayerSerial
 */
@Serializable
data class PlayerSerial(
    val id: Int,
    val email: String,
    val displayName: String,
    val settings: PlayerSettingsBase,
)

/**
 * routePlayersList Adds the routing for the players list endpoint
 * GET (/api/players) which responds with a list of players based on the
 * provided limit and offset query parameters
 */
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

/**
 * routePlayerSettings Adds the routing for the player settings endpoints
 * this includes the GET (/api/playerSettings) and POST (/api/setPlayerSettings)
 * endpoints which retrieve and update player settings
 */
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

/**
 * routeUpdatePlayer Adds the routing for updating a player
 * POST (/api/updatePlayer) this takes a player serial and
 * updates the player in the database
 */
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