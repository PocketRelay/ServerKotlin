package com.jacobtread.kme.servers.http.routes

import com.jacobtread.kme.servers.http.router.RoutingGroup
import com.jacobtread.kme.servers.http.router.get
import com.jacobtread.kme.servers.http.router.responseStatic
import com.jacobtread.kme.servers.http.router.setHeader

/**
 * routeContents Add the routing catching-all for the ME3 assets
 * this is used for sending images for the shop and related
 */
fun RoutingGroup.routeContents() {
    get("content/:*") {
        val path = param("*")
        val fileName = path.substringAfterLast('/')
        responseStatic(fileName, "public")
            .setHeader("Accept-Ranges", "bytes")
            .setHeader("ETag", "524416-1333666807000")
    }
}