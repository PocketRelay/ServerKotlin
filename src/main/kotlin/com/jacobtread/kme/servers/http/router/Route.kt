package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.servers.http.HttpRequest

abstract class Route(pattern: String) : RouteHandler {
    val pattern: String = pattern
        .removePrefix("/")
        .removeSuffix("/")
    val tokens: List<String> = this.pattern
        .split('/')

    private fun matchWithParameters(request: HttpRequest, startIndex: Int, endIndex: Int): Boolean {
        val requestTokens = request.tokens
        for (i in 0 until endIndex) {
            val token = tokens[i]
            val value = requestTokens[startIndex + i]
            if (token.startsWith(':')) {
                request.setParam(token.substring(1), value)
            } else if (token != value) {
                return false
            }
        }
        return true
    }

    fun matchSimple(request: HttpRequest, startIndex: Int, endIndex: Int): Boolean {
        val requestTokens = request.tokens
        if (startIndex >= requestTokens.size) return false
        for (i in 0 until endIndex) {
            val token = tokens[i]
            val value = requestTokens[startIndex + i]
            if (token != value) {
                return false
            }
        }
        return true
    }

    fun matches(start: Int, request: HttpRequest): Boolean {
        val requestTokens = request.tokens
        val tokenCount = tokens.size
        if (tokenCount > 0 && tokens.last() == ":*") {
            if (!matchWithParameters(request, start, tokenCount - 1)) return false
            val index = start + tokenCount - 1
            val builder = StringBuilder()
            for (i in index until requestTokens.size) {
                builder.append(requestTokens[i])
                if (i < requestTokens.size - 1) {
                    builder.append('/')
                }
            }
            request.setParam("*", builder.toString())
            return true
        }
        if ((requestTokens.size - start) == tokenCount) {
            return matchWithParameters(request, start, tokenCount)
        }
        return false
    }
}