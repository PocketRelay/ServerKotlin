package com.jacobtread.kme.servers.http.router

import io.netty.handler.codec.http.HttpMethod

interface RoutingGroup {

    val routes: ArrayList<RouteHandler>

    fun route(pattern: String, method: HttpMethod?, handler: RequestHandler) {
        routes.add(PathRoute(pattern, method, handler))
    }

    fun route(pattern: String, handler: RequestHandler) = route(pattern, null, handler)
    fun get(pattern: String, handler: RequestHandler) = route(pattern, HttpMethod.GET, handler)
    fun post(pattern: String, handler: RequestHandler) = route(pattern, HttpMethod.POST, handler)
    fun put(pattern: String, handler: RequestHandler) = route(pattern, HttpMethod.PUT, handler)
    fun delete(pattern: String, handler: RequestHandler) = route(pattern, HttpMethod.DELETE, handler)
    fun everything(handler: RequestHandler) = route(":*", null, handler)
}


inline fun RoutingGroup.group(pattern: String, init: GroupRoute.() -> Unit) {
    val group = GroupRoute(pattern)
    group.init()
    routes.add(group)
}