package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.WrappedRequest

open class GroupRoute(
    pattern: String,
) : TokenMatcher(), RoutingGroup {

    override val tokens = pattern
        .removePrefix("/")
        .removeSuffix("/")
        .split('/')
    override val routes = ArrayList<RequestMatcher>()

    override fun handle(config: Config, request: WrappedRequest): Boolean {
        for (route in routes) {
            if (!route.matches(config, request)) continue
            if (route.handle(config, request)) {
                return true
            }
        }
        return false
    }
}

inline fun Router.routerGroup(pattern: String, init: GroupRoute.() -> Unit) {
    val group = GroupRoute(pattern)
    group.init()
    routes.add(group)
}


