package com.jacobtread.kme.blaze.debug

interface BlazeLoggingOutput {
    fun debug(text: String)

    fun warn(text: String)
    fun warn(text: String, cause: Throwable)

    fun error(text: String)
    fun error(text: String, cause: Throwable)
}