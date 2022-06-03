package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.servers.http.HttpRequest

/**
 * Route Represents a route handler which matches requests against
 * patterns. This class provides a way for child classes to match
 * url patterns.
 *
 * @constructor Creates a new route
 * @param pattern The url pattern for this route leading and trailing slashes are omitted
 * (e.g "/apple/banana" and "apple/banana/" is equivalent to "apple/banana" this is to avoid weird parsing)
 * you can provide parameters for matching a route using : then the name of the parameter
 * (e.g /:path) they can be accessed on the request object using the param functions in the case of
 * /:path it can be accessed using param("path") you can catch any number of remaining tokens using
 * the catch-all parameter which is :*
 * Note: The catch-all parameter can only be used as the last parameter attempting to use
 * it anywhere else will result in it simply only matching one token
 */
abstract class Route(pattern: String) : RouteHandler {

    /**
     * patternTokens A list containing the individual tokens of the
     * route pattern the leading and trailing slashes are removed
     * before splitting. This is used when comparing to url tokens
     */
    private val patternTokens: List<String> = pattern
        .removePrefix("/")
        .removePrefix("/")
        .split('/')

    val tokenCount: Int get() = patternTokens.size

    /**
     * matchRange Matches the specified range of request tokens from the start index
     * to the end index. Will return true if the range matches or false if it doesn't
     *
     * @param request The http request used for its tokens and for
     * setting parameters that have been matched
     * @param startIndex The index of the request tokens to start matching
     * @param count The total number of tokens to match
     * @return True if all the tokens match otherwise false
     */
    fun matchRange(request: HttpRequest, startIndex: Int, count: Int): Boolean {
        val requestTokens = request.tokens

        // If we don't have enough tokens
        if ((requestTokens.size - startIndex) < count) return false

        for (i in 0 until count) {
            val token = patternTokens[i]
            val value = requestTokens[startIndex + i]
            if (token.startsWith(':')) { // If we are matching a parameter
                request.setParam(token.substring(1), value)
            } else if (token != value) {
                return false
            }
        }
        return true
    }

    /**
     * matchCatchall Matches for patterns where the last token is a
     * catch-all parameter and consumes the catchall tokens from the
     * request returning true if a catch-all was captured otherwise
     * return false if it wasn't able to capture one
     *
     * @param start The index of the request tokens to match from
     * @param request The request to match
     * @return Whether the tokens were matches as a catch-all
     */
    fun matchCatchall(start: Int, request: HttpRequest): Boolean {
        val requestTokens = request.tokens
        val tokenCount = patternTokens.size
        if (tokenCount > 0 && patternTokens.last() == ":*") {
            if (!matchRange(request, start, tokenCount - 1)) return false
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
        } else {
            return false
        }
    }
}