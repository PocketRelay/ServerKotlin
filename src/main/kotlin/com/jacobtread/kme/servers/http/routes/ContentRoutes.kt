package com.jacobtread.kme.servers.http.routes

import com.jacobtread.kme.servers.http.router.*

/**
 * routeContents Add the routing catching-all for the ME3 assets
 * this is used for sending images for the shop and related
 */
fun RoutingGroup.routeContents() {
    group("content") {
        everything {
            val path = param("*")
            val fileName = path.substringAfterLast('/')
            responseStatic(fileName, "public")
                .setHeader("Accept-Ranges", "bytes")
                .setHeader("ETag", "524416-1333666807000")
        }
    }
}