package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.servers.http.WrappedRequest

open class GroupRoute(
    pattern: String,
) : TokenMatcher(), RoutingGroup {
    private val pattern = pattern.removePrefix("/")
        .removeSuffix("/")
    override val tokens = this.pattern
        .split('/')
    override val routes = ArrayList<RequestMatcher>()

    override fun matches(start: Int, request: WrappedRequest): Boolean {
        if (start >= request.tokens.size) {
            return false
        }
        return matchInternal(request, start, tokens.size)
    }

    override fun handle(start: Int, request: WrappedRequest): Boolean {
        val startIndex = start + tokens.size
        for (route in routes) {
            if (!route.matches(startIndex, request)) continue
            if (route.handle(startIndex, request)) {
                return true
            }
        }
        return false
    }

    override fun toString(): String = "Group($pattern)"
}


