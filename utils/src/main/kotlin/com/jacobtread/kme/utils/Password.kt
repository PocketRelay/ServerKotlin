package com.jacobtread.kme.utils

import java.security.SecureRandom
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// The cost of the hashing process 1 < HASH_COST = total iterations
private const val HASH_COST = 16
// The length of the hash key
private const val HASH_SIZE = 128
// The hashing algorithm to use
private const val HASH_ALGORITHM = "PBKDF2WithHmacSHA512"
// The prefix to append to the hashed password
private const val HASH_PREFIX = "$31$"

/**
 * hashPassword Hashes the password using the provided
 * HASH_ALGORITHM
 *
 * @param password The password to hash
 * @return The hashed password
 */
fun hashPassword(password: String): String {
    val random = SecureRandom()
    val salt = ByteArray(16)
    random.nextBytes(salt)
    val spec = PBEKeySpec(password.toCharArray(), salt, 1 shl HASH_COST, HASH_SIZE)
    val keyFactory = SecretKeyFactory.getInstance(HASH_ALGORITHM)
    val hash = keyFactory.generateSecret(spec).encoded
    val encoded = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(hash);
    return "$HASH_PREFIX$HASH_COST\$$encoded"
}

/**
 * comparePasswordHash Compares two password hashes by hashing the
 * provided value and comparing it to the hashed value
 *
 * @param password The non hashed password
 * @param hashed The hashed password
 * @return Whether the passwords match
 */
fun comparePasswordHash(password: String, hashed: String): Boolean {
    return hashPassword(password) == hashed
}