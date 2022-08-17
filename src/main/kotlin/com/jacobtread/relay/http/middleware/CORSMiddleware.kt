package com.jacobtread.relay.http.middleware

import com.jacobtread.netty.http.HttpRequest
import com.jacobtread.netty.http.HttpResponse
import com.jacobtread.netty.http.middleware.Middleware
import com.jacobtread.netty.http.response
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus

/**
 * Middleware for allowing browsers by providing CORS
 * headers on OPTIONS requests
 */
object CORSMiddleware : Middleware {
    override fun handleRequest(request: HttpRequest): HttpResponse? {
        if (request.method == HttpMethod.OPTIONS) {
            val response = response(HttpResponseStatus.NO_CONTENT)
            val headers = response.headers()
            headers.set("Access-Control-Allow-Origin", "*")
            headers.set("Access-Control-Allow-Methods", "*")
            headers.set("Access-Control-Allow-Headers", "*")
            return response
        }
        return null
    }
}