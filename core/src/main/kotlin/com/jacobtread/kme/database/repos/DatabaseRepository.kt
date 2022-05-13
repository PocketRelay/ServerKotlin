package com.jacobtread.kme.database.repos

/**
 * DatabaseRepository Represents a repository that stores a specific
 * set of data and provides ways for creating and querying it using
 * different database types
 *
 * @constructor Create empty DatabaseRepository
 */
abstract class DatabaseRepository {

    /**
     * init Initializes the repository within the database.
     * Usually for creating tables to store data within
     */
    abstract fun init()
}
