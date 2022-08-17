package com.jacobtread.kme.http.routes

import com.jacobtread.kme.Environment
import com.jacobtread.kme.http.middleware.AuthMiddleware
import com.jacobtread.kme.http.contentJson
import com.jacobtread.kme.http.data.API
import com.jacobtread.kme.http.responseError
import com.jacobtread.kme.http.responseJson
import com.jacobtread.kme.utils.generateRandomString
import com.jacobtread.netty.http.responseText
import com.jacobtread.netty.http.router.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.put
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

fun RoutingGroup.routePanel() {
    group("api") {
        routePanelAuth()

        middlewareGroup(AuthMiddleware) {
            get("test") {
                responseText("hi")
            }
        }

    }
}

@Serializable
data class PanelAuthObject(val username: String, val password: String)

fun RoutingGroup.routePanelAuth() {
    post("auth") {
        val authObject = contentJson<PanelAuthObject>()

        if (authObject.username == Environment.panelUsername
            && authObject.password == Environment.panelPassword
        ) {
            responseJson {
                put("token", API.createToken())
            }
        } else {
            responseError("Incorrect credentials")
        }
    }
}