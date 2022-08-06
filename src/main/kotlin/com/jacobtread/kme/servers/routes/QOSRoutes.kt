package com.jacobtread.kme.servers.routes

import com.jacobtread.kme.utils.responseXml
import com.jacobtread.netty.http.router.RoutingGroup

fun RoutingGroup.routeQOS() {
    get("/qos/qos") {
        responseXml("qos") {
            textNode("numprobes", 0)
            textNode("qosport", 17499) // This is a port
            textNode("probesize", 0)
            textNode("qosip", 2733913518) // This is a encoded ip address
            textNode("requestid", 1)
            textNode("reqsecret", 0)
        }
    }
}