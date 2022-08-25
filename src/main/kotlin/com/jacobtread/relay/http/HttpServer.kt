package com.jacobtread.relay.http

import com.jacobtread.netty.http.HttpEventHandler
import com.jacobtread.netty.http.HttpRequest
import com.jacobtread.netty.http.HttpResponse
import com.jacobtread.netty.http.router.createHttpServer
import com.jacobtread.relay.Environment
import com.jacobtread.relay.http.routes.routeApi
import com.jacobtread.relay.http.routes.routeContents
import com.jacobtread.relay.http.routes.routeGroupGAW
import com.jacobtread.relay.http.routes.routeQOS
import com.jacobtread.relay.utils.logging.Logger
import io.netty.channel.EventLoopGroup
import java.io.IOException
import java.util.concurrent.CompletableFuture as Future

fun startHttpServer(
    bossGroup: EventLoopGroup,
    workerGroup: EventLoopGroup,
): Future<Void> {
    val startupFuture = Future<Void>()
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

                @Suppress("EmptyFunctionBlock")
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
        }.addListener {
            Logger.info("Started HTTP server on port ${Environment.httpPort}")
            startupFuture.complete(null)
        }
    } catch (e: IOException) {
        val reason = e.message ?: e.javaClass.simpleName
        Logger.fatal("Unable to start HTTP server: $reason")
    }
    return startupFuture
}
