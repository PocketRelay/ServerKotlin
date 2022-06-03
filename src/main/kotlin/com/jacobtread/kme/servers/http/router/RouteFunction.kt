package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.WrappedRequest

/**
 * RouteFunction Functional interface that represents a function called to
 * handle an HTTP request. This function is provided the request object
 *
 * @constructor Create empty RouteFunction
 */
fun interface RouteFunction {

    fun handle(request: WrappedRequest)

}