package com.miadfm.podcasts.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinSecurityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vault_auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_SALT = "pin_salt_b64"
        private const val KEY_PIN_HASH = "pin_hash_b64"
        private const val ITERATION_COUNT = 10000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 16
    }

    fun isPinSet(): Boolean {
        return prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)
    }

    fun setPin(pin: String): Boolean {
        if (pin.length !in 4..8 || !pin.all { it.isDigit() }) {
            return false
        }
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        val hash = hashPin(pin, salt)

        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_PIN_SALT, saltB64)
            .putString(KEY_PIN_HASH, hashB64)
            .apply()
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val saltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val expectedHashB64 = prefs.getString(KEY_PIN_HASH, null) ?: return false

        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expectedHash = Base64.decode(expectedHashB64, Base64.NO_WRAP)

        val computedHash = hashPin(pin, salt)

        // Constant time comparison to prevent timing attacks
        return java.security.MessageDigest.isEqual(expectedHash, computedHash)
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }
}
