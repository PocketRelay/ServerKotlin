package com.jacobtread.kme.servers.http.routes

import com.jacobtread.kme.servers.http.router.RoutingGroup

/**
 * routeContents Add the routing catching-all for the ME3 assets
 * this is used for sending images for the shop and related
 */
fun RoutingGroup.routeContents() {
    get("content/:*") { request ->
        val path = request.param("*")
        val fileName = path.substringAfterLast('/')
        request.setHeader("Accept-Ranges", "bytes")
        request.setHeader("ETag", "524416-1333666807000")
        request.static(fileName, "public")
    }
}