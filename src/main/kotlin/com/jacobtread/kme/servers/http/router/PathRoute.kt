package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.servers.http.HttpRequest
import io.netty.handler.codec.http.HttpMethod

class PathRoute(
    pattern: String,
    private val method: HttpMethod?,
    private val handler: RequestHandler,
) : Route(pattern) {

    override fun handle(start: Int, request: HttpRequest): RequestResponse? {
        if (method != null && method != request.method) return null
        if (!matches(start, request)) return null
        return request.handler()
    }

    override fun toString(): String {
        val builder = StringBuilder("Path(pattern=\"")
            .append(pattern)
            .append("\"")
        if (method != null) {
            builder.append(", ")
            builder.append(method.name())
        }
        return builder
            .append(')')
            .toString()
    }
}
