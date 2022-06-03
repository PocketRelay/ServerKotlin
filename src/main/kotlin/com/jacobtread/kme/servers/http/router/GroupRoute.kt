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

    override fun handle(start: Int, request: WrappedRequest): RequestResponse? {
        val startIndex = start + tokens.size
        for (route in routes) {
            if (!route.matches(startIndex, request)) continue
            val response = route.handle(startIndex, request)
            if (response != null) return response
        }
        return null
    }

    override fun toString(): String = "Group($pattern)"
}


