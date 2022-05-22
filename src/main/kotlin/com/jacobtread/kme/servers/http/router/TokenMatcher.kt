package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.WrappedRequest

abstract class TokenMatcher : RequestMatcher {
    abstract val tokens: List<String>

    fun matchInternal(request: WrappedRequest, startIndex: Int, endIndex: Int): Boolean {
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

    override fun matches(config: Config, start: Int, request: WrappedRequest): Boolean {
        val requestTokens = request.tokens
        if (requestTokens.size - start == tokens.size) {
            return matchInternal(request, start, tokens.size)
        }
        if (tokens.isNotEmpty() && tokens.last() == "*") {
            if (!matchInternal(request, start, tokens.size - 1)) return false
            val builder = StringBuilder(requestTokens[start + tokens.lastIndex])
            for (i in tokens.size until requestTokens.size) {
                builder.append('/')
                    .append(requestTokens[start + i])
            }
            request.setParam("*", builder.toString())
            return true
        }
        return false
    }
}