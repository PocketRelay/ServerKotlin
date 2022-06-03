package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.servers.http.HttpRequest

class BadRequestException : RuntimeException("Client request was invalid")

typealias RequestHandler = HttpRequest.() -> RequestResponse
