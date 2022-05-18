package com.jacobtread.kme.utils

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * customThreadFactory Creates a thread factory which uses the
 * provided name for all child threads (replaces {ID} with a
 * unique ID for the thread) all threads created are daemon's
 *
 * @param name The name to use
 * @return The created thread factor
 */
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

fun nameThread(name: String) {
    Thread.currentThread().name = name
}