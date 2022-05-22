package com.jacobtread.kme.servers.http.router

import com.jacobtread.kme.Config
import com.jacobtread.kme.servers.http.WrappedRequest

interface RequestMatcher {

    /**
     * matches Whether this request matches the matcher
     * and should be passed onto handle
     *
     * @param config The config for the server
     * @param request The request to match
     * @return Whether to should be handled by this matcher
     */
    fun matches(config: Config, request: WrappedRequest): Boolean

    /**
     * handle Handles the logic of this request matcher
     *
     * @param config The config for the server
     * @param request The request to handle
     * @return Whether this matcher consumed the value if this returns
     * true then no further matchers will be processed
     */
    fun handle(config: Config, request: WrappedRequest): Boolean

}