package com.jacobtread.relay.data.cache

import java.util.concurrent.TimeUnit

data class CacheMarker<T>(
    val key: String,
    val creator: CacheCreator<T>,
    val expiryTime: Long,
    val timeUnit: TimeUnit
)