package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.WrappedRequest

open class PathRoute(
    pattern: String,
    private val method: Router.HttpMethod,
    private val handler: RouteFunction,
) : TokenMatcher() {

    override val tokens = pattern
        .removePrefix("/")
        .removeSuffix("/")
        .split('/')

    override fun matches(config: Config, request: WrappedRequest): Boolean {
        if (method != Router.HttpMethod.ANY && method.value != request.method) return false
        return super.matches(config, request)
    }

    override fun handle(config: Config, request: WrappedRequest): Boolean {
        handler.handle(config, request)
        return true
    }
}
