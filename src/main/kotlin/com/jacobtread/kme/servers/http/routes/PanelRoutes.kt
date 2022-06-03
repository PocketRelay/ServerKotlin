package com.jacobtread.kme.servers.http.routes

import com.jacobtread.kme.servers.http.router.RoutingGroup
import com.jacobtread.kme.servers.http.router.everything
import com.jacobtread.kme.servers.http.router.group
import com.jacobtread.kme.servers.http.router.responseStatic

/**
 * routeGroupPanel Adds the route grouping for panel routes
 * this includes the /panel/api route group and the fallback
 * routing group which handles the static assets
 */
fun RoutingGroup.routeGroupPanel() {
    group("panel") {
        routeGroupApi() // Add api routing group
        routePanelFallback() // Add fallback routing
    }
}

/**
 * routePanelFallback Handles the fallback routing for /panel this will
 * redirect any static assets to the correct file and fall back onto
 * the index.html file if the requested file doesn't exist
 */
private fun RoutingGroup.routePanelFallback() {
    everything {// Catchall for static assets falling back on index.html
        val path = param("*")
        responseStatic(path, "panel", "index.html", "panel")
    }
}