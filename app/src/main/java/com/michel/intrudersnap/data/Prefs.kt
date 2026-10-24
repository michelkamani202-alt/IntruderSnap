package com.michel.intrudersnap.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stockage chiffré de la configuration (PIN hashé, identifiants SMTP, préférences).
 * Rien n'est jamais stocké en clair sur le disque : EncryptedSharedPreferences
 * s'appuie sur le Android Keystore.
 */
class Prefs(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "intrudersnap_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // --- Consentement ---
    var consentAccepted: Boolean
        get() = prefs.getBoolean(KEY_CONSENT, false)
        set(value) = prefs.edit().putBoolean(KEY_CONSENT, value).apply()

    // --- PIN (jamais stocké en clair : sel + hash SHA-256, voir SecurityUtils) ---
    var pinSalt: String?
        get() = prefs.getString(KEY_PIN_SALT, null)
        set(value) = prefs.edit().putString(KEY_PIN_SALT, value).apply()

    var pinHash: String?
        get() = prefs.getString(KEY_PIN_HASH, null)
        set(value) = prefs.edit().putString(KEY_PIN_HASH, value).apply()

    val hasPin: Boolean get() = pinHash != null && pinSalt != null

    fun setPin(pin: String) {
        val salt = SecurityUtils.generateSalt()
        pinSalt = salt
        pinHash = SecurityUtils.hashPin(pin, salt)
    }

    fun verifyPin(pin: String): Boolean {
        val salt = pinSalt ?: return false
        val hash = pinHash ?: return false
        return SecurityUtils.verifyPin(pin, salt, hash)
    }

    // --- Seuil d'échecs avant capture ---
    var maxAttempts: Int
        get() = prefs.getInt(KEY_MAX_ATTEMPTS, 3)
        set(value) = prefs.edit().putInt(KEY_MAX_ATTEMPTS, value.coerceIn(1, 20)).apply()

    // --- Configuration SMTP ---
    var smtpHost: String?
        get() = prefs.getString(KEY_SMTP_HOST, null)
        set(value) = prefs.edit().putString(KEY_SMTP_HOST, value).apply()

    var smtpPort: String?
        get() = prefs.getString(KEY_SMTP_PORT, "587")
        set(value) = prefs.edit().putString(KEY_SMTP_PORT, value).apply()

    var smtpUser: String?
        get() = prefs.getString(KEY_SMTP_USER, null)
        set(value) = prefs.edit().putString(KEY_SMTP_USER, value).apply()

    var smtpPass: String?
        get() = prefs.getString(KEY_SMTP_PASS, null)
        set(value) = prefs.edit().putString(KEY_SMTP_PASS, value).apply()

    var recipientEmail: String?
        get() = prefs.getString(KEY_RECIPIENT, null)
        set(value) = prefs.edit().putString(KEY_RECIPIENT, value).apply()

    val emailConfigured: Boolean
        get() = !smtpHost.isNullOrBlank() && !smtpUser.isNullOrBlank() &&
            !smtpPass.isNullOrBlank() && !recipientEmail.isNullOrBlank()

    // --- Applications protégées (ensemble de packages) ---
    var protectedPackages: Set<String>
        get() = prefs.getStringSet(KEY_PROTECTED_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_PROTECTED_APPS, value).apply()

    fun setAppProtected(packageName: String, protected: Boolean) {
        val current = protectedPackages.toMutableSet()
        if (protected) current.add(packageName) else current.remove(packageName)
        protectedPackages = current
    }

    companion object {
        private const val KEY_CONSENT = "consent_accepted"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_MAX_ATTEMPTS = "max_attempts"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_SMTP_USER = "smtp_user"
        private const val KEY_SMTP_PASS = "smtp_pass"
        private const val KEY_RECIPIENT = "recipient_email"
        private const val KEY_PROTECTED_APPS = "protected_apps"
    }
}
