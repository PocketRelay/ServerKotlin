package com.jacobtread.kme.servers.http.routes

import com.jacobtread.kme.database.byId
import com.jacobtread.kme.database.entities.PlayerEntity
import com.jacobtread.kme.servers.http.router.*
import io.netty.handler.codec.http.HttpResponseStatus.OK

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
 * routePlayersList Adds the routing for the players list endpoint
 * GET (/api/players) which responds with a list of players based on the
 * provided limit and offset query parameters
 */
private fun RoutingGroup.routePlayersList() {
    get("players") {
        val limit = queryInt("limit", default = 10)
        val offset = queryInt("offset", default = 0)
        val playerList = PlayerEntity.createSerialList(offset, limit)
        responseJson(playerList)
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
        val playerEntity = PlayerEntity.byId(playerId) ?: throw BadRequestException()
        responseJson(playerEntity.createSettingsMap())
    }
    post("setPlayerSettings") {
        val playerId = queryInt("id")
        val playerEntity = PlayerEntity.byId(playerId) ?: throw BadRequestException()
        val settings = contentJson<Map<String, String>>()
        settings.forEach { playerEntity.setSetting(it.key, it.value) }
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
        val serial = contentJson<PlayerEntity.Serial>()
        val existing = PlayerEntity.byId(serial.id) ?: throw BadRequestException()
        existing.applySerialUpdate(serial)
        response(OK)
    }
}