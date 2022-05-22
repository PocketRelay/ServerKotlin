package com.jacobtread.kme.servers.http.controllers

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.WrappedRequest
import com.jacobtread.kme.servers.http.router.GroupRoute

object GAWController : GroupRoute("/wal/masseffect-gaw-pc/") {

    init {
        get("authentication/sharedTokenLogin", GAWController::handleAuthentication)
        get("galaxyatwar/getRatings/:id", GAWController::handleRatings)
        get("galaxyatwar/increaseRatings/:id", GAWController::handleIncreaseRatings)
    }

    private fun handleAuthentication(config: Config, request: WrappedRequest) {

    }

    private fun handleRatings(config: Config, request: WrappedRequest) {

    }

    private fun handleIncreaseRatings(config: Config, request: WrappedRequest) {

    }

}
