package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.servers.http.HttpRequest

sealed interface RouteHandler {

    /**
     * handle Handles the logic of this request handler if the return
     * result of this handler is null then the next handler will handle
     * the request
     *
     * @param start The portion index to start at
     * @param request The request to handle
     * @return The request response or null
     */
    fun handle(start: Int, request: HttpRequest): RequestResponse?

}