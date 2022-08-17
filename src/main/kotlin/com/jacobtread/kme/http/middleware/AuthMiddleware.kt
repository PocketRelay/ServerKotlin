package com.jacobtread.kme.http.middleware

import com.jacobtread.kme.http.data.API
import com.jacobtread.netty.http.HttpRequest
import com.jacobtread.netty.http.HttpResponse
import com.jacobtread.netty.http.middleware.GuardMiddleware
import com.jacobtread.netty.http.response
import io.netty.handler.codec.http.HttpResponseStatus

/**
 * Token based authentication middleware. Checks the
 * request headers for the X-Token header which should
 * contain a token that can be checked by [API.checkToken]
 * if this token is valid the request can continue otherwise
 * it is given an error message
 */
object AuthMiddleware : GuardMiddleware() {
    override fun isAllowed(request: HttpRequest): Boolean {
        val headers = request.headers
        val token = headers["X-Token"]
        return token != null && API.checkToken(token)
    }

    override fun createErrorResponse(request: HttpRequest): HttpResponse {
        return response(HttpResponseStatus.UNAUTHORIZED)
    }
}
