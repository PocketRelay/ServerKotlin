package com.jacobtread.kme.servers

import com.jacobtread.kme.Environment
import com.jacobtread.kme.http.routes.routeApi
import com.jacobtread.kme.http.routes.routeContents
import com.jacobtread.kme.http.routes.routeGroupGAW
import com.jacobtread.kme.http.routes.routeQOS
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.netty.http.HttpEventHandler
import com.jacobtread.netty.http.HttpRequest
import com.jacobtread.netty.http.HttpResponse
import com.jacobtread.netty.http.router.createHttpServer
import io.netty.channel.EventLoopGroup
import java.io.IOException

fun startHttpServer(
    bossGroup: EventLoopGroup,
    workerGroup: EventLoopGroup,
) {
    try {
        createHttpServer(Environment.httpPort, bossGroup, workerGroup) {
            eventHandler = object : HttpEventHandler {
                override fun onExceptionHandled(cause: Exception) {
                    Logger.warn("Exception occurred when handling http request", cause)
                }

                override fun onRequestReceived(request: HttpRequest) {
                    Logger.logIfDebug {
                        val url = request.reconstructUrl()
                        "[HTTP] [${request.method.name()}] Request to $url"
                    }
                }

                override fun onResponsePreSend(response: HttpResponse) {
                }

                override fun onResponseSent(response: HttpResponse) {
                    Logger.logIfDebug {
                        "[HTTP] [RESPONSE] Status [${response.status()}]"
                    }
                }
            }
            routeGroupGAW()
            routeContents()
            routeQOS()
            if (Environment.apiEnabled) {
                routeApi()
            }
        }.sync()
        Logger.info("Started HTTP server on port ${Environment.httpPort}")
    } catch (e: IOException) {
        val reason = e.message ?: e.javaClass.simpleName
        Logger.fatal("Unable to start HTTP server: $reason")
    }
}
