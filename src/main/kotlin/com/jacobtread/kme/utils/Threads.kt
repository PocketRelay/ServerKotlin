package com.jacobtread.kme.utils

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

fun customThreadFactory(name: String): ThreadFactory {
    return object : ThreadFactory {
        val ID = AtomicInteger(0)
        override fun newThread(r: Runnable): Thread {
            val thread = Thread(r)
            thread.name = name.replace("{ID}", ID.getAndIncrement().toString())
            thread.isDaemon = true
            return thread
        }
    }
}