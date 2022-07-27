package com.jacobtread.kme.tools

import java.time.Instant

/**
 * Uses the java Instant api to return the current
 * unix time in seconds.
 *
 * @return The current unix time in seconds
 */
fun unixTimeSeconds(): Long = Instant.now().epochSecond

/**
 * Calculates the time in seconds between the current
 * unix time in seconds and the provided unix time in
 * seconds.
 *
 * @see unixTimeSeconds for retrieving unix time in seconds
 * @param time The time to calculate the difference between
 * @return The difference in seconds between the current and provided times
 */
fun unixTimeSecondsSince(time: Long): Long = unixTimeSeconds() - time

/**
 * Caclulates the time in days between the current time
 * in unix seconds and the provided time in unix seconds
 *
 * @param time The time to calculate the difference between
 * @return The difference in days between in the current and provided times
 */
fun unixTimeDaysSince(time: Long): Long = (unixTimeSecondsSince(time) / 86400 /* 1 day = 86400 seconds */)
