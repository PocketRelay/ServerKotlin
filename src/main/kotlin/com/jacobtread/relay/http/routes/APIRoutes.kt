package com.jacobtread.relay.http.routes

import com.jacobtread.netty.http.*
import com.jacobtread.netty.http.router.RoutingGroup
import com.jacobtread.netty.http.router.group
import com.jacobtread.netty.http.router.middlewareGroup
import com.jacobtread.relay.data.Constants
import com.jacobtread.relay.database.tables.PlayerCharactersTable
import com.jacobtread.relay.database.tables.PlayerClassesTable
import com.jacobtread.relay.database.tables.PlayersTable
import com.jacobtread.relay.game.Game
import com.jacobtread.relay.http.contentJson
import com.jacobtread.relay.http.data.*
import com.jacobtread.relay.http.middleware.AuthMiddleware
import com.jacobtread.relay.http.middleware.CORSMiddleware
import com.jacobtread.relay.http.responseJson
import com.jacobtread.relay.utils.logging.Logger
import io.netty.handler.codec.http.HttpResponseStatus
import kotlinx.serialization.json.put
import java.util.concurrent.ExecutionException

fun RoutingGroup.routeApi() {
    group("api") {
        middleware(CORSMiddleware)

        routeStatus()
        routeCheckToken()
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
    val version = Constants.RELAY_VERSION
    get("status") {
        responseJson {
            put("identity", "KME_SERVER")
            put("version", version)
        }
    }
}

private fun RoutingGroup.routeCheckToken() {
    post("checkToken") {
        val request = contentJson<CheckTokenRequest>()
        responseJson {
            put("valid", API.checkToken(request.token))
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
            val players = PlayersTable.getList(offset, count)
                .get()
            responseJson(players)
        } catch (e: ExecutionException) {
            Logger.error("Error while retrieving players", e)
            throwServerError()
        }
    }
}


private fun RoutingGroup.routePlayer() {
    get("players/:id") {
        val id = paramInt("id")
        try {
            val player = PlayersTable.getByID(id)
                .get() ?: throw HttpException(HttpResponseStatus.NOT_FOUND)
            val characters = PlayerCharactersTable.getByPlayer(player)
                .get()
            val classes = PlayerClassesTable.getByPlayer(player)
                .get()
            val charactersSerial = characters.map {
                CharacterSerializable(it.index, it.kitName, it.name, it.deployed)
            }
            val playerData = FullPlayerData(player, classes, charactersSerial)
            responseJson(playerData)
        } catch (e: ExecutionException) {
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
            val player = PlayersTable.getByID(id)
                .get() ?: throw HttpException(HttpResponseStatus.NOT_FOUND)
            player.displayName = playerUpdate.displayName
            player.credits = playerUpdate.credits
            player.inventory = playerUpdate.inventory
            player.csReward = playerUpdate.csReward
            PlayersTable.setPlayerFully(player)
            response(HttpResponseStatus.OK)
        } catch (e: ExecutionException) {
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