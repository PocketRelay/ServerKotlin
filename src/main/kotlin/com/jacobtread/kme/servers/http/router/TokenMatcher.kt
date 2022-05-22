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
        val tokenCount = tokens.size
        if (tokenCount > 0 && tokens.last() == ":*") {
            if (!matchInternal(request, start, tokenCount - 1)) return false
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
            return matchInternal(request, start, tokenCount)
        }
        return false
    }
}