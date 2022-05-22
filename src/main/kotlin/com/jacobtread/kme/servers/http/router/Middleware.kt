package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.WrappedRequest

abstract class Middleware : RequestMatcher {
    override fun matches(config: Config, start: Int, request: WrappedRequest): Boolean {
        return true
    }
}