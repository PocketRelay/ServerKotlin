package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.WrappedRequest
import com.jacobtread.kme.servers.http.exceptions.InvalidParamException
import com.jacobtread.kme.servers.http.exceptions.InvalidQueryException
import com.jacobtread.kme.utils.logging.Logger
import com.mysql.cj.log.Log
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpMethod as NettyHttpMethod

@Sharable
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
            var handled = false
            for (route in routes) {
                if (!route.matches(config, 0, request)) continue
                if (route.handle(config, 0, request)) {
                    handled = true
                    break
                }
            }
            if (!handled) {
                request.static("404.html", "public")
            }
        } catch (e: Exception) {
            if (e !is InvalidParamException && e !is InvalidQueryException) {
                request.response(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                Logger.info("Exception occurred when handling http request", e)
            } else {
                request.response(HttpResponseStatus.BAD_REQUEST)
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

