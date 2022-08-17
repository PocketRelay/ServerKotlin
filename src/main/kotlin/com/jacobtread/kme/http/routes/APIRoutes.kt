package com.jacobtread.kme.http.routes

import com.jacobtread.kme.Environment
import com.jacobtread.kme.exceptions.DatabaseException
import com.jacobtread.kme.game.Game
import com.jacobtread.kme.http.contentJson
import com.jacobtread.kme.http.data.API
import com.jacobtread.kme.http.data.AuthRequest
import com.jacobtread.kme.http.data.AuthResponse
import com.jacobtread.kme.http.data.GameSerializable
import com.jacobtread.kme.http.middleware.AuthMiddleware
import com.jacobtread.kme.http.middleware.CORSMiddleware
import com.jacobtread.kme.http.responseJson
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.netty.http.httpNotFound
import com.jacobtread.netty.http.router.RoutingGroup
import com.jacobtread.netty.http.router.group
import com.jacobtread.netty.http.router.middlewareGroup
import com.jacobtread.netty.http.throwBadRequest
import com.jacobtread.netty.http.throwServerError

fun RoutingGroup.routeApi() {
    group("api") {
        middleware(CORSMiddleware)

        routeAuth()

        middlewareGroup(AuthMiddleware) {
            routePlayers()
            routePlayer()

            routeGames()
            routeGame()
        }
    }
}

private fun RoutingGroup.routeAuth() {
    post("auth") {
        val authRequest = contentJson<AuthRequest>()
        val authResponse = if (API.isCredentials(authRequest.username, authRequest.password)) {
            AuthResponse(true, API.createToken())
        } else {
            AuthResponse(false, "")
        }
        responseJson(authResponse)
    }
}

private fun RoutingGroup.routePlayers() {
    get("players") {
        val offset = queryInt("offset", default = 0)
        val count = queryInt("count", default = 10)
        try {
            val database = Environment.database
            val players = database.getPlayers(offset, count)
            responseJson(players)
        } catch (e: DatabaseException) {
            Logger.error("Error while retrieving players", e)
            throwServerError()
        }
    }
}

private fun RoutingGroup.routePlayer() {
    get("players/:id") {
        val id = paramInt("id")
        try {
            val database = Environment.database
            val player = database.getPlayerById(id)
            responseJson(player)
        } catch (e: DatabaseException) {
            Logger.error("Error while retrieving player", e)
            httpNotFound()
        }
    }
}

private fun RoutingGroup.routeGames() {
    get("games") {
        val games = ArrayList<GameSerializable>()
        Game.forEachGame { games.add(it.createGameSerial()) }
        responseJson(games)
    }
}

private fun RoutingGroup.routeGame() {
    get("games/:id") {
        val id = param("id")
            .toULongOrNull() ?: throwBadRequest()
        val game = Game.getById(id)
        if (game != null) {
            responseJson(game.createGameSerial())
        } else {
            httpNotFound()
        }
    }
}