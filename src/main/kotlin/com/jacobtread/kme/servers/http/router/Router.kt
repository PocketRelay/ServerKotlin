package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.servers.http.HttpRequest
import com.jacobtread.kme.utils.logging.Logger
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST
import io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR
import io.netty.handler.codec.http.HttpRequest as NettyHttpRequest

/**
 * Router A netty channel inbound handler for handling the routing
 * of HTTP requests to different specified routes
 *
 * Note: Marked as sharable so that the same router can be added to multiple
 * channels to prevent the unnecessarily large amount of allocations that would
 * occur from recreating the router for every single channel
 *
 * @constructor Create empty Router
 */
@Sharable
class Router : SimpleChannelInboundHandler<NettyHttpRequest>(), RoutingGroup {
    /**
     * routes The list of routes for this router used for
     * determining how to handle incoming http requests
     */
    override val routes = ArrayList<RouteHandler>()

    /**
     * handlerAdded When the router handler is added it also
     * needs to add the HttpRequest decoder and HttpResponse
     * encoder along with the HttpObjectAggregator to
     * aggregate the decoded body contents
     *
     * @param ctx The channel handler context
     */
    override fun handlerAdded(ctx: ChannelHandlerContext) {
        // Pipeline order = Decode -> Aggregator -> Encode
        val channel = ctx.channel()
        channel.pipeline()
            .addFirst(HttpObjectAggregator(1024 * 8))
            .addFirst(HttpRequestDecoder())
            .addLast(HttpResponseEncoder())
    }

    /**
     * channelRead0 Handles reading the raw netty http requests
     * and wrapping them with the HttpRequest before attempting
     * to handle them with the handlers finishes up by writing
     * the response back to the client
     *
     * @param ctx The channel handler context for the channel
     * @param msg The http request message
     */
    override fun channelRead0(ctx: ChannelHandlerContext, msg: NettyHttpRequest) {
        Logger.debug("Http request for ${msg.uri()}")
        val request = HttpRequest(msg) // Create a request wrapper
        val response = handleHttpRequest(request) // Handle the response
        // Write the response and flush
        ctx.writeAndFlush(response)
    }

    /**
     * handleHttpRequest Handles the response to a request. Goes through all
     * the routes attempting to get a response falling back on 404.html if
     * the response wasn't handled or BAD_REQUEST / INTERNAL_SERVER_ERROR
     * if an exception was thrown while trying to handle the request
     *
     * @param request The request to handle
     * @return The response to the request
     */
    private fun handleHttpRequest(request: HttpRequest): HttpResponse {
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

/**
 * createRouter Helper function for creating and
 * initializing a router with its routes
 *
 * @param init The initializer function
 * @receiver The router created for initializing
 * @return The initialized routing
 */
inline fun createRouter(init: Router.() -> Unit): Router {
    val router = Router()
    router.init()
    return router
}

