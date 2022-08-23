package com.jacobtread.relay.database

/**
 * Interface for representing a database table which
 * can be created using a SQL string provided by [sql]
 */
internal interface Table {

    /**
     * Returns the SQL update string nessicary to create
     * the table
     *
     * @return The SQL string
     */
    fun sql(): String
}