package com.jacobtread.kme.servers.http.router

interface RoutingGroup {
    val routes: MutableList<RequestMatcher>

    fun any(pattern: String, handler: RouteFunction) = routes.add(PathRoute(pattern, Router.HttpMethod.ANY, handler))
    fun get(pattern: String, handler: RouteFunction) = routes.add(PathRoute(pattern, Router.HttpMethod.GET, handler))
    fun post(pattern: String, handler: RouteFunction) = routes.add(PathRoute(pattern, Router.HttpMethod.POST, handler))
    fun put(pattern: String, handler: RouteFunction) = routes.add(PathRoute(pattern, Router.HttpMethod.PUT, handler))
    fun delete(pattern: String, handler: RouteFunction) = routes.add(PathRoute(pattern, Router.HttpMethod.DELETE, handler))
    fun group(group: GroupRoute) = routes.add(group)
    fun middleware(middleware: Middleware) = routes.add(middleware)
}