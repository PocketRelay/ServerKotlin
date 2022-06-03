package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.servers.http.HttpRequest

class GroupRoute(pattern: String) : Route(pattern), RoutingGroup {
    override val routes = ArrayList<RouteHandler>()

    override fun handle(start: Int, request: HttpRequest): RequestResponse? {
        if (!matchSimple(request, start, tokens.size)) {
            return null
        }
        val startIndex = start + tokens.size
        for (route in routes) {
            val response = route.handle(startIndex, request)
            if (response != null) return response
        }
        return null
    }

    override fun toString(): String {
        val builder = StringBuilder("Group(pattern=\"")
            .append(pattern)
            .append("\", routes=[")
        routes.joinTo(builder, ", ")
        return builder.append("])")
            .toString()
    }
}


