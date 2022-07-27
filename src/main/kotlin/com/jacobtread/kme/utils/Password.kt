package com.jacobtread.kme.utils

import java.security.SecureRandom
import java.util.*
import java.util.regex.Pattern
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

private val HASH_PATTERN = Pattern.compile("\\$31\\$(\\d\\d?)\\$(.{43})")

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
    val hash = hashValue(password.toCharArray(), salt, HASH_COST)
    val hashSalt = salt + hash
    val encoded = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(hashSalt);
    return "$HASH_PREFIX$HASH_COST\$$encoded"
}

private fun hashValue(password: CharArray, salt: ByteArray, cost: Int): ByteArray {
    val spec = PBEKeySpec(password, salt, 1 shl cost, HASH_SIZE)
    val keyFactory = SecretKeyFactory.getInstance(HASH_ALGORITHM)
    return keyFactory.generateSecret(spec).encoded
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
    val hashMatcher = HASH_PATTERN.matcher(hashed)
    if (!hashMatcher.matches()) return false
    val cost = hashMatcher.group(1).toIntOrNull() ?: HASH_COST
    val contents = Base64.getUrlDecoder().decode(hashMatcher.group(2))
    val salt = contents.copyOfRange(0, 16)
    val hash = contents.copyOfRange(16, contents.size)
    val newHash = hashValue(password.toCharArray(), salt, cost)
    return hash.contentEquals(newHash)
}