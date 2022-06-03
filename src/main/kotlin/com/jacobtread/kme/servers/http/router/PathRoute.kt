package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.servers.http.WrappedRequest
import io.netty.handler.codec.http.HttpMethod

open class PathRoute(
    pattern: String,
    private val method: HttpMethod?,
    private val handler: RequestHandler,
) : TokenMatcher() {
    private val pattern = pattern
        .removePrefix("/")
        .removeSuffix("/")

    override val tokens = this.pattern
        .split('/')

    override fun matches(start: Int, request: WrappedRequest): Boolean {
        if (method != null && method != request.method) return false
        return super.matches(start, request)
    }

    override fun handle(start: Int, request: WrappedRequest): RequestResponse? = request.handler()
    override fun toString(): String = "Path($pattern)"
}
