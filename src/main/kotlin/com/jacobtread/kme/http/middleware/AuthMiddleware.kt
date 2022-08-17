package com.jacobtread.kme.http.middleware

import com.jacobtread.kme.http.data.API
import com.jacobtread.kme.http.responseError
import com.jacobtread.netty.http.HttpRequest
import com.jacobtread.netty.http.HttpResponse
import com.jacobtread.netty.http.router.Middleware
import io.netty.handler.codec.http.HttpResponseStatus

/**
 * Token based authentication middleware. Checks the
 * request headers for the X-Token header which should
 * contain a token that can be checked by [API.checkToken]
 * if this token is valid the request can continue otherwise
 * it is given an error message
 */
object AuthMiddleware : Middleware {
    override fun handleRequest(request: HttpRequest): HttpResponse? {
        val headers = request.headers
        val token = headers["X-Token"]
        if (token == null || !API.checkToken(token)) {
            return responseError(
                "Invalid authentication",
                HttpResponseStatus.UNAUTHORIZED
            )
        }
        return null
    }
}