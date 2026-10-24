package com.michel.intrudersnap.lock

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.michel.intrudersnap.data.Prefs
import com.michel.intrudersnap.ui.LockActivity

/**
 * Détecte quand une application protégée par l'utilisateur passe au premier
 * plan et affiche aussitôt l'écran de verrouillage IntruderSnap par-dessus.
 *
 * Ce service NE LIT PAS le contenu des fenêtres : seul le nom du package de
 * l'application au premier plan est utilisé (événement TYPE_WINDOW_STATE_CHANGED).
 */
class AppLockAccessibilityService : AccessibilityService() {

    private var screenReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    AppLockState.lockAll()
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        screenReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) {}
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        // Ignore nos propres écrans
        if (pkg == packageName) return

        val prefs = Prefs(this)
        if (!prefs.hasPin) return

        if (pkg in prefs.protectedPackages) {
            if (!AppLockState.isUnlocked(pkg)) {
                val label = resolveAppLabel(pkg)
                val lockIntent = Intent(this, LockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(LockActivity.EXTRA_MODE, LockActivity.MODE_PROTECT_APP)
                    putExtra(LockActivity.EXTRA_TARGET_PACKAGE, pkg)
                    putExtra(LockActivity.EXTRA_TARGET_LABEL, label)
                }
                startActivity(lockIntent)
            }
        }
    }

    private fun resolveAppLabel(pkg: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    override fun onInterrupt() {
        // Rien à nettoyer
    }
}
