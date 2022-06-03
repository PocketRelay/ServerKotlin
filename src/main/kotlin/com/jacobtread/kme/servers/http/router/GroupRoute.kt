package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.servers.http.HttpRequest

/**
 * GroupRoute Handles the grouping of multiple routes that are matched
 * after the provided prefix.
 *
 * For example
 * group("/apple") {
 *
 * }
 * Will catch any requests that start with /apple and any routes defined
 * inside that group will be passed the remaining tokens
 *
 * Note: groups will consume the tokens they match, so they will not be
 * visible to child routes including child groups.
 *
 * @constructor Creates a new group route
 *
 * @param pattern The pattern to use for matching this group
 */
class GroupRoute(pattern: String) : Route(pattern), RoutingGroup {
    override val routes = ArrayList<RouteHandler>()

    override fun handle(start: Int, request: HttpRequest): RequestResponse? {
        val tokenCount = tokenCount
        if (!matchRange(request, start, tokenCount)) return null
        val startIndex = start + tokenCount
        for (route in routes) {
            val response = route.handle(startIndex, request)
            if (response != null) return response
        }
        return null
    }

}


