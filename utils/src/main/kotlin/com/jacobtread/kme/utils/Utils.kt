package com.jacobtread.kme.utils

import kotlinx.serialization.KSerializer
import java.time.Instant

fun unixTimeSeconds(): Long = Instant.now().epochSecond