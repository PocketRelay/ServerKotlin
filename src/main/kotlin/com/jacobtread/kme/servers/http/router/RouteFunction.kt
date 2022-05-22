package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.WrappedRequest

fun interface RouteFunction {

    fun handle(config: Config, request: WrappedRequest)

}