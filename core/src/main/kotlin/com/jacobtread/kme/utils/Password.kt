package com.jacobtread.kme.utils

import java.security.SecureRandom
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private const val HASH_COST = 16
private const val HASH_SIZE = 128
private const val HASH_ALGORITHM = "PBKDF2WithHmacSHA512"
private const val HASH_PREFIX = "$31$"

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

fun comparePasswordHash(password: String, hashed: String): Boolean {
    return hashPassword(password) == hashed
}