package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.servers.http.HttpRequest
import com.jacobtread.kme.utils.logging.Logger
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST
import io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR
import io.netty.handler.codec.http.HttpRequest as NettyHttpRequest

@Sharable
class Router : SimpleChannelInboundHandler<NettyHttpRequest>(), RoutingGroup {

    override val routes = ArrayList<RouteHandler>()

    override fun channelRead0(ctx: ChannelHandlerContext, msg: NettyHttpRequest) {
        Logger.debug("Http request for ${msg.uri()}")
        val request = HttpRequest(msg)
        val response = handleHttpRequest(request)
        ctx.writeAndFlush(response)
    }

    private fun handleHttpRequest(request: HttpRequest): RequestResponse {
        try {
            for (route in routes) {
                return route.handle(0, request) ?: continue
            }
            return responseStatic("404.html", "public")
        } catch (e: BadRequestException) {
            return response(BAD_REQUEST)
        } catch (e: Exception) {
            Logger.info("Exception occurred when handling http request", e)
            return response(INTERNAL_SERVER_ERROR)
        }
    }
}

inline fun createRouter(init: Router.() -> Unit): Router {
    val router = Router()
    router.init()
    return router
}

