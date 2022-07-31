package com.jacobtread.kme.servers

import com.jacobtread.kme.Environment
import com.jacobtread.kme.servers.routes.routeContents
import com.jacobtread.kme.servers.routes.routeGroupGAW
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.netty.http.HttpEventHandler
import com.jacobtread.netty.http.HttpRequest
import com.jacobtread.netty.http.HttpResponse
import com.jacobtread.netty.http.router.createRouter
import io.netty.channel.EventLoopGroup
import java.io.IOException

fun startHttpServer(
    bossGroup: EventLoopGroup,
    workerGroup: EventLoopGroup,
) {
    try {
        val httpRouter = createRouter {
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

                override fun onResponseSent(response: HttpResponse) {
                    Logger.logIfDebug {
                        "[HTTP] [RESPONSE] Status [${response.status()}]"
                    }
                }
            }
            routeGroupGAW()
            routeContents()
        }

        httpRouter.startHttpServer(
            Environment.httpPort,
            bossGroup,
            workerGroup
        ).addListener {
            Logger.info("Started HTTP Server on port ${Environment.httpPort}")
        }
    } catch (e: IOException) {
        Logger.error("Exception when starting HTTP server", e)
    }
}