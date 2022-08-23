package com.jacobtread.relay.data.cache

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object Cache {

    private val entries = HashMap<String, CacheEntry<*>>()
    private val lock = ReentrantReadWriteLock()

    fun <T> get(marker: CacheMarker<T>): T {
        val value = lock.read { entries[marker.key] }
        if (value == null || value.isExpired()) {
            val newEntry = createEntry(marker)
            lock.write {
                entries[marker.key] = newEntry
            }
            return newEntry.value
        }
        @Suppress("UNCHECKED_CAST")
        return value.value as T
    }

    fun <T> regenerate(marker: CacheMarker<T>): T {
        return lock.write {
            val newEntry = createEntry(marker)
            entries[marker.key] = newEntry
            newEntry.value
        }
    }

    fun <T> getEntry(marker: CacheMarker<T>): CacheEntry<T>? {
        @Suppress("UNCHECKED_CAST")
        return lock.read { entries[marker.key] as CacheEntry<T>? }
    }

    fun <T> createMarker(
        key: String,
        creator: CacheCreator<T>,
        expiryTime: Long = 1,
        timeUnit: TimeUnit = TimeUnit.MINUTES,
    ): CacheMarker<T> {
        return CacheMarker(key, creator, expiryTime, timeUnit)
    }

    private fun <T> createEntry(marker: CacheMarker<T>): CacheEntry<T> {
        val duration = marker.timeUnit.toMillis(marker.expiryTime)
        val expiryTime = System.currentTimeMillis() + duration
        val value = marker.creator.createItem()
        return CacheEntry(value, expiryTime)
    }

    data class CacheEntry<T>(val value: T, val expiryTime: Long) {
        fun isExpired(): Boolean {
            val currentTime = System.currentTimeMillis()
            return currentTime > expiryTime
        }
    }
}