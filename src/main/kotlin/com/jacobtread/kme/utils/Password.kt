package com.jacobtread.kme.utils

import java.security.SecureRandom
import java.util.*
import java.util.regex.Pattern
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * hashPassword Hashes the password using the provided
 * HASH_ALGORITHM
 *
 * @param password The password to hash
 * @return The hashed password
 */
fun hashPassword(password: String): String {
    // The cost of the hashing process 1 < HASH_COST = total iterations
    val hashCost = 16
    val random = SecureRandom()
    val salt = ByteArray(16)
    random.nextBytes(salt)
    val hash = hashValue(password.toCharArray(), salt, hashCost)
    val hashSalt = salt + hash
    val encoded = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(hashSalt);
    val prefix = "$31$"
    return "$prefix$hashCost\$$encoded"
}

private fun hashValue(password: CharArray, salt: ByteArray, cost: Int): ByteArray {
    val spec = PBEKeySpec(password, salt, 1 shl cost, 128)
    val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
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
    val hashPattern = Pattern.compile("\\$31\\$(\\d\\d?)\\$(.{43})")
    val hashMatcher = hashPattern.matcher(hashed)
    if (!hashMatcher.matches()) return false
    val cost = hashMatcher.group(1).toIntOrNull() ?: 16
    val contents = Base64.getUrlDecoder().decode(hashMatcher.group(2))
    val salt = contents.copyOfRange(0, 16)
    val hash = contents.copyOfRange(16, contents.size)
    val newHash = hashValue(password.toCharArray(), salt, cost)
    return hash.contentEquals(newHash)
}
