package com.jacobtread.kme.servers.http.router

import io.netty.handler.codec.http.HttpMethod

interface RoutingGroup {
    val routes: MutableList<RouteHandler>

    operator fun RouteHandler.unaryPlus() {
        routes.add(this)
    }
}


fun RoutingGroup.route(pattern: String, handler: RequestHandler) {
    routes.add(PathRoute(pattern, null, handler))
}

fun RoutingGroup.get(pattern: String, handler: RequestHandler) {
    routes.add(PathRoute(pattern, HttpMethod.GET, handler))
}

fun RoutingGroup.post(pattern: String, handler: RequestHandler) {
    routes.add(PathRoute(pattern, HttpMethod.POST, handler))
}

fun RoutingGroup.put(pattern: String, handler: RequestHandler) {
    routes.add(PathRoute(pattern, HttpMethod.PUT, handler))
}

fun RoutingGroup.delete(pattern: String, handler: RequestHandler) {
    routes.add(PathRoute(pattern, HttpMethod.DELETE, handler))
}

inline fun RoutingGroup.group(pattern: String, init: GroupRoute.() -> Unit) {
    val group = GroupRoute(pattern)
    group.init()
    routes.add(group)
}