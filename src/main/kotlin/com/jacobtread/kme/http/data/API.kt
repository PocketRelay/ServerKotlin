package com.jacobtread.kme.http.data

import com.jacobtread.kme.Environment
import com.jacobtread.kme.utils.generateRandomString
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * State holder for the API routes. This stores the tokens
 * for authenticating api requests as well as handling token
 * expiry.
 */
object API {

    /**
     * Constant for storing the total milliseconds a token
     * is allowed to be used for until it is considered invalid.
     *
     * Currently, 6 Hours
     */
    private const val EXPIRY_TIME = 1000 * 60 * 60 * 6

    /**
     * Map storing the tokens mapped to the time that they will
     * expire at. Should only be accessed through [tokensLock]
     */
    private val tokens = HashMap<String, Long>()

    /**
     * Lock for reading and writing to the [tokens] map safely
     * when multiple threads are attempting to access it
     */
    private val tokensLock = ReentrantReadWriteLock()

    /**
     * Checks the provided to token to see if it exists in
     * the [tokens] map and whether it is expired or not.
     *
     * Will remove expired tokens
     *
     * @param token The token to check
     * @return Whether the token is valid (exists and is not expired)
     */
    fun checkToken(token: String): Boolean {
        tokensLock.read {
            val currentTime = System.currentTimeMillis()
            val expiryTime = tokens[token]
            return when {
                expiryTime == null -> false
                expiryTime <= currentTime -> {
                    tokensLock.write { tokens.remove(token) }
                    false
                }

                else -> true
            }
        }
    }

    /**
     * Creates a new unique token and inserts it into the
     * [tokens] map returning the generated token
     *
     * @return The generated token
     */
    fun createToken(): String {
        var token: String
        do {
            token = generateRandomString(64)
        } while (checkToken(token))
        val expiryTime = System.currentTimeMillis() + EXPIRY_TIME
        tokensLock.write { tokens.put(token, expiryTime) }
        return token
    }

    fun isCredentials(username: String, password: String): Boolean {
        return username == Environment.apiUsername && password == Environment.apiPassword
    }

}