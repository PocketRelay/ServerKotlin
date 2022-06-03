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
        return if (matchCatchall(start, request)
            || matchRange(request, start, tokenCount)
        ) {
            request.handler()
        } else {
            null
        }

    }
}
