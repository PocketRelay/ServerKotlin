package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.WrappedRequest

open class PathRoute(
    pattern: String,
    private val method: Router.HttpMethod,
    private val handler: RouteFunction,
) : TokenMatcher() {
    private val pattern = pattern
        .removePrefix("/")
        .removeSuffix("/")

    override val tokens = this.pattern
        .split('/')

    override fun matches(config: Config, start: Int, request: WrappedRequest): Boolean {
        if (method != Router.HttpMethod.ANY && method.value != request.method) return false
        return super.matches(config, start, request)
    }

    override fun handle(config: Config, request: WrappedRequest): Boolean {
        handler.handle(config, request)
        return true
    }

    override fun toString(): String = "Path($pattern)"
}
