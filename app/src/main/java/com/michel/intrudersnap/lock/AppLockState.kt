package com.michel.intrudersnap.lock

import java.util.concurrent.ConcurrentHashMap

/**
 * Maintient l'état des applications temporairement déverrouillées pendant
 * la session courante de l'utilisateur.
 */
object AppLockState {

    private val unlockedPackages = ConcurrentHashMap<String, Long>()

    // Un déverrouillage est valide pendant 30 minutes au maximum
    private const val UNLOCK_TIMEOUT_MS = 30 * 60 * 1000L

    fun isUnlocked(packageName: String): Boolean {
        val unlockTime = unlockedPackages[packageName] ?: return false
        if (System.currentTimeMillis() - unlockTime > UNLOCK_TIMEOUT_MS) {
            unlockedPackages.remove(packageName)
            return false
        }
        return true
    }

    fun setUnlocked(packageName: String) {
        unlockedPackages[packageName] = System.currentTimeMillis()
    }

    fun lock(packageName: String) {
        unlockedPackages.remove(packageName)
    }

    fun lockAll() {
        unlockedPackages.clear()
    }
}
