package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.servers.http.WrappedRequest
import com.jacobtread.kme.servers.http.exceptions.InvalidParamException
import com.jacobtread.kme.servers.http.exceptions.InvalidQueryException
import com.jacobtread.kme.utils.logging.Logger
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST
import io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR

@Sharable
class Router : SimpleChannelInboundHandler<HttpRequest>(), RoutingGroup {

    override val routes = ArrayList<RequestMatcher>()

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        Logger.debug("Http request for ${msg.uri()}")
        val request = WrappedRequest(msg)
        val response = handleHttpRequest(request)
        ctx.writeAndFlush(response)
    }

    private fun handleHttpRequest(request: WrappedRequest): RequestResponse {
        var response: RequestResponse?
        try {
            for (route in routes) {
                if (!route.matches(0, request)) continue
                response = route.handle(0, request)
                if (response != null) return response
            }
            return responseStatic("404.html", "public")
        } catch (e: Exception) {
            val reason = if (e !is InvalidParamException && e !is InvalidQueryException) {
                Logger.info("Exception occurred when handling http request", e)
                INTERNAL_SERVER_ERROR
            } else {
                BAD_REQUEST
            }
            return response(reason)
        }
    }
}

inline fun createRouter(init: Router.() -> Unit): Router {
    val router = Router()
    router.init()
    return router
}

