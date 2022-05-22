package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.WrappedRequest

abstract class TokenMatcher: RequestMatcher{
    abstract val tokens: List<String>

    private fun matchInternal(requestTokens: List<String>, endIndex: Int, params: MutableMap<String, String>): Boolean {
        for (i in 0 until endIndex) {
            val token = tokens[i]
            if (token.startsWith(':')) {
                params[token.substring(1)] = requestTokens[i]
            } else if (token != requestTokens[i]) {
                return false
            }
        }
        return true
    }

    override fun matches(config: Config, request: WrappedRequest): Boolean {
        val requestTokens = request.tokens
        if (requestTokens.size == tokens.size) {
            return matchInternal(requestTokens, tokens.size, request.params)
        }
        if (tokens.isNotEmpty() && tokens.last() == "*") {
            if (!matchInternal(requestTokens, tokens.size - 1, request.params)) return false
            val builder = StringBuilder(requestTokens[tokens.lastIndex])
            for (i in tokens.size until requestTokens.size) {
                builder.append('/')
                    .append(requestTokens[i])
            }
            request.params["*"] = builder.toString()
            return true
        }
        return false
    }
}