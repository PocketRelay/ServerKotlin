package com.jacobtread.kme.database

import java.sql.Driver

/**
 * Wrapper for SQL drivers that are loaded at runtime.
 * Attempting to register the drivers without this wrapper
 * causes them not to be registered correctly
 *
 * @param driver The drivers that this is wrapping
 */
class RuntimeDriver(private val driver: Driver) : Driver by driver