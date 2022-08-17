package com.jacobtread.kme.http.routes

import com.jacobtread.kme.Environment
import com.jacobtread.kme.data.Constants
import com.jacobtread.kme.exceptions.DatabaseException
import com.jacobtread.kme.game.Game
import com.jacobtread.kme.http.contentJson
import com.jacobtread.kme.http.data.*
import com.jacobtread.kme.http.middleware.AuthMiddleware
import com.jacobtread.kme.http.middleware.CORSMiddleware
import com.jacobtread.kme.http.responseJson
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.netty.http.*
import com.jacobtread.netty.http.router.RoutingGroup
import com.jacobtread.netty.http.router.group
import com.jacobtread.netty.http.router.middlewareGroup
import io.netty.handler.codec.http.HttpResponseStatus
import kotlinx.serialization.json.put

fun RoutingGroup.routeApi() {
    group("api") {
        middleware(CORSMiddleware)

        routeStatus()
        routeAuth()

        middlewareGroup(AuthMiddleware) {
            routePlayers()
            routePlayer()
            routeUpdatePlayer()

            routeGames()
            routeGame()
        }
    }
}

private fun RoutingGroup.routeStatus() {
    val version = Constants.KME_VERSION
    get("status") {
        responseJson {
            put("identity", "KME_SERVER")
            put("version", version)
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
                ?: throw HttpException(HttpResponseStatus.NOT_FOUND)
            val characters = database.getPlayerCharacters(player)
            val classes = database.getPlayerClasses(player)
            val charactersSerial = characters.map {
                CharacterSerializable(it.index, it.kitName, it.name, it.deployed)
            }
            val playerData = FullPlayerData(player, classes, charactersSerial)
            responseJson(playerData)
        } catch (e: DatabaseException) {
            Logger.error("Error while retrieving player", e)
            throwServerError()
        }
    }
}

private fun RoutingGroup.routeUpdatePlayer() {
    put("players/:id") {
        val id = paramInt("id")
        val playerUpdate = contentJson<PlayerUpdate>()
        try {
            val database = Environment.database
            val player = database.getPlayerById(id)
                ?: throw HttpException(HttpResponseStatus.NOT_FOUND)
            player.displayName = playerUpdate.displayName
            player.credits = playerUpdate.credits
            player.inventory = playerUpdate.inventory
            player.csReward = playerUpdate.csReward
            Environment.database.updatePlayerFully(player)
            response(HttpResponseStatus.OK)
        } catch (e: DatabaseException) {
            Logger.error("Error while updating player", e)
            throwServerError()
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