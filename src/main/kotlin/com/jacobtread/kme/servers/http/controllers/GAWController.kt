package com.jacobtread.kme.servers.http.controllers

import com.jacobtread.kme.Config
import com.jacobtread.kme.database.Player
import com.jacobtread.kme.servers.http.WrappedRequest
import com.jacobtread.kme.servers.http.router.GroupRoute
import io.netty.handler.codec.http.HttpResponseStatus
import org.jetbrains.exposed.sql.transactions.transaction

object GAWController : GroupRoute("/wal/masseffect-gaw-pc/") {

    init {
        get("authentication/sharedTokenLogin", GAWController::handleAuthentication)
        get("galaxyatwar/getRatings/:id", GAWController::handleRatings)
        get("galaxyatwar/increaseRatings/:id", GAWController::handleIncreaseRatings)
    }

    private fun handleAuthentication(config: Config, request: WrappedRequest) {
        val playerId = request.queryInt("auth", 16)
        val player = transaction { Player.findById(playerId) } ?: return request.response(HttpResponseStatus.BAD_REQUEST)

        @Suppress("SpellCheckingInspection")
        request.xml("fulllogin") {
        }
    }

    private fun handleRatings(config: Config, request: WrappedRequest) {

    }

    private fun handleIncreaseRatings(config: Config, request: WrappedRequest) {

    }

}
