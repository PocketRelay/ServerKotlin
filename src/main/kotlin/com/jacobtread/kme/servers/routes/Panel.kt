package com.jacobtread.kme.servers.routes

import com.jacobtread.kme.Environment
import com.jacobtread.kme.utils.contentJson
import com.jacobtread.kme.utils.responseJson
import com.jacobtread.netty.http.HttpRequest
import com.jacobtread.netty.http.HttpResponse
import com.jacobtread.netty.http.responseText
import com.jacobtread.netty.http.router.Middleware
import com.jacobtread.netty.http.router.RoutingGroup
import com.jacobtread.netty.http.router.group
import io.netty.handler.codec.http.HttpResponseStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.put
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object Panel {

    private const val EXPIRY_TIME = 1000 * 60 * 60 * 12
    private val tokens = HashMap<String, Long>()
    private val tokensLock = ReentrantReadWriteLock()

    fun isValidToken(token: String): Boolean {
        return tokensLock.read {
            val currentTime = System.currentTimeMillis()
            val insertTime = tokens[token]
            if (insertTime == null) {
                false
            } else if (currentTime - insertTime >= EXPIRY_TIME) {
                tokensLock.write { tokens.remove(token) }
                false
            } else {
                true
            }
        }
    }

    fun createToken(): String {
        while (true) {
            val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPSQRSTUVWXYZ0123456789-"
            val builder = StringBuilder()
            repeat(64) { builder.append(chars.random()) }
            val token = builder.toString()
            if (tokensLock.read { !tokens.contains(token) }) {
                val currentTime = System.currentTimeMillis()
                tokensLock.write { tokens.put(token, currentTime) }
                return token
            }
        }
    }
}

private fun responseError(
    message: String,
    status: HttpResponseStatus = HttpResponseStatus.BAD_REQUEST,
): HttpResponse {
    return responseJson(status) {
        put("message", message)
    }
}

fun RoutingGroup.routePanel() {
    group("api") {
        routePanelAuth()
    }

    group("api") {
        addMiddleware(AuthMiddleware)

        get("test") {
            responseText("hi")
        }
    }
}

private object AuthMiddleware : Middleware {
    override fun handleRequest(request: HttpRequest): HttpResponse? {
        val headers = request.headers
        val token = headers["X-Token"]
        if (token != null) {
            if (Panel.isValidToken(token)) {
                return null
            }
        }
        return responseError("Invalid authentication", HttpResponseStatus.UNAUTHORIZED)
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
                put("token", Panel.createToken())
            }
        } else {
            responseError("Incorrect credentials")
        }
    }
}