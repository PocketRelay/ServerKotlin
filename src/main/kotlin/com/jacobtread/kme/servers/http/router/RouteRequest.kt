package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.servers.http.WrappedRequest

typealias RequestHandler = WrappedRequest.() -> RequestResponse
