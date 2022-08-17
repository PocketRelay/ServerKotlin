package com.jacobtread.kme.http.routes

import com.jacobtread.kme.http.contentJson
import com.jacobtread.kme.http.data.API
import com.jacobtread.kme.http.data.AuthRequest
import com.jacobtread.kme.http.data.AuthResponse
import com.jacobtread.kme.http.middleware.AuthMiddleware
import com.jacobtread.kme.http.middleware.CORSMiddleware
import com.jacobtread.kme.http.responseJson
import com.jacobtread.netty.http.responseText
import com.jacobtread.netty.http.router.RoutingGroup
import com.jacobtread.netty.http.router.group
import com.jacobtread.netty.http.router.middlewareGroup

fun RoutingGroup.routeApi() {
    group("api") {
        middleware(CORSMiddleware)

        routeAuth()

        middlewareGroup(AuthMiddleware) {
            get("test") {
                responseText("hi")
            }
        }
    }
}

private fun RoutingGroup.routeAuth() {
    post("auth") {
        println("Auth request")
        val authRequest = contentJson<AuthRequest>()
        val authResponse = if (API.isCredentials(authRequest.username, authRequest.password)) {
            AuthResponse(true, API.createToken())
        } else {
            AuthResponse(false, "")
        }
        responseJson(authResponse)
    }
}

