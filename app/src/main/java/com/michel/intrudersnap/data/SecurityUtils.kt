package com.michel.intrudersnap.data

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Le PIN n'est jamais stocké en clair : on stocke sel + hash SHA-256(sel + pin).
 */
object SecurityUtils {

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return hashed.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
        return hashPin(pin, salt) == expectedHash
    }
}
