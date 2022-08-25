package com.jacobtread.relay.http.routes

import com.jacobtread.relay.http.responseXml
import com.jacobtread.netty.http.router.RoutingGroup

fun RoutingGroup.routeQOS() {
    get("/qos/qos") {
        responseXml("qos") {
            textNode("numprobes", 0)
            textNode("qosport", 17499) // This is a port
            textNode("probesize", 0)
            // 162.244.53.174
            textNode("qosip", 2733913518)
            textNode("requestid", 1)
            textNode("reqsecret", 0)
        }
    }
}
