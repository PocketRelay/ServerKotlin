package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.WrappedRequest

open class GroupRoute(
    pattern: String,
) : TokenMatcher(), RoutingGroup {
    private val pattern = pattern.removePrefix("/")
        .removeSuffix("/")
    override val tokens = this.pattern
        .split('/')
    override val routes = ArrayList<RequestMatcher>()

    override fun matches(config: Config, start: Int, request: WrappedRequest): Boolean {
        return matchInternal(request, start, tokens.size)
    }

    override fun handle(config: Config, request: WrappedRequest): Boolean {
        val startIndex = tokens.size
        for (route in routes) {
            if (!route.matches(config, startIndex, request)) continue
            if (route.handle(config, request)) {
                return true
            }
        }
        return false
    }

    override fun toString(): String = "Group($pattern)"
}


