package com.jacobtread.kme.exceptions

/**
 * NotAuthenticatedException Exception thrown when the player gets accessed from
 * a player session that is not authenticated
 *
 * @constructor Create empty NotAuthenticatedException
 */
class NotAuthenticatedException : RuntimeException("Not authenticated")