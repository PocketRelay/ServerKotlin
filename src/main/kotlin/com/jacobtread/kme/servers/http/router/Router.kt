package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.WrappedRequest
import com.jacobtread.kme.servers.http.exceptions.InvalidParamException
import com.jacobtread.kme.servers.http.exceptions.InvalidQueryException
import com.jacobtread.kme.utils.logging.Logger
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpMethod as NettyHttpMethod

class Router(val config: Config) : SimpleChannelInboundHandler<HttpRequest>(), RoutingGroup {
    enum class HttpMethod(val value: NettyHttpMethod?) {
        ANY(null),
        GET(NettyHttpMethod.GET),
        POST(NettyHttpMethod.POST),
        PUT(NettyHttpMethod.PUT),
        DELETE(NettyHttpMethod.DELETE)
    }

    override val routes = ArrayList<RequestMatcher>()

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        val request = WrappedRequest(msg)
        try {
            for (route in routes) {
                if (!route.matches(config, request)) continue
                if (route.handle(config, request)) break
            }
        } catch (e: Exception) {
            if (e !is InvalidParamException && e !is InvalidQueryException) {
                Logger.info("Exception occurred when handling http request", e)
                request.response(HttpResponseStatus.BAD_REQUEST)
            } else {
                request.response(HttpResponseStatus.INTERNAL_SERVER_ERROR)
            }
        }
        val response = request.createResponse()
        ctx.writeAndFlush(response)
    }
}

inline fun router(config: Config, init: Router.() -> Unit): Router {
    val router = Router(config)
    router.init()
    return router
}

