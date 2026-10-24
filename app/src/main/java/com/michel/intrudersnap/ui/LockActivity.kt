package com.michel.intrudersnap.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.michel.intrudersnap.R
import com.michel.intrudersnap.capture.IntruderCaptureService
import com.michel.intrudersnap.data.Prefs
import com.michel.intrudersnap.lock.AppLockState

/**
 * Écran de verrouillage par code PIN.
 *
 * Deux usages :
 *  - MODE_OPEN_DASHBOARD : l'utilisateur ouvre IntruderSnap lui-même pour
 *    accéder au tableau de bord.
 *  - MODE_PROTECT_APP : affiché par-dessus une application tierce protégée,
 *    déclenché par AppLockAccessibilityService.
 *
 * Après N échecs consécutifs (configurable), une capture photo est
 * déclenchée via IntruderCaptureService et l'utilisateur est renvoyé à
 * l'écran d'accueil du système.
 */
class LockActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private var failedAttempts = 0
    private var mode: String = MODE_OPEN_DASHBOARD
    private var targetPackage: String? = null
    private var targetLabel: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreenIfNeeded()
        setContentView(R.layout.activity_lock)

        prefs = Prefs(this)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_OPEN_DASHBOARD
        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        targetLabel = intent.getStringExtra(EXTRA_TARGET_LABEL)

        val labelView = findViewById<TextView>(R.id.textLockedAppLabel)
        labelView.text = targetLabel ?: getString(R.string.app_name)

        val editPin = findViewById<TextInputEditText>(R.id.editLockPin)
        val textError = findViewById<TextView>(R.id.textLockError)
        val textCaptureWarning = findViewById<TextView>(R.id.textCaptureWarning)
        val buttonUnlock = findViewById<MaterialButton>(R.id.buttonUnlock)

        // Si l'application n'est pas encore configurée (pas de PIN), on ne
        // devrait normalement jamais arriver ici en MODE_PROTECT_APP.
        if (!prefs.hasPin) {
            finish()
            return
        }

        buttonUnlock.setOnClickListener {
            val pin = editPin.text?.toString().orEmpty()
            if (prefs.verifyPin(pin)) {
                onUnlockSuccess()
            } else {
                failedAttempts++
                editPin.text?.clear()
                val max = prefs.maxAttempts
                textError.text = getString(R.string.lock_wrong_pin, failedAttempts, max)
                textError.visibility = TextView.VISIBLE

                if (failedAttempts < max) {
                    IntruderCaptureService.logAttempt(
                        this, targetPackage ?: packageName, targetLabel ?: getString(R.string.app_name), success = false
                    )
                }

                if (failedAttempts >= max) {
                    onCaptureThreshold()
                } else if (failedAttempts == max - 1) {
                    textCaptureWarning.visibility = TextView.VISIBLE
                }
            }
        }
    }

    private fun showOverLockScreenIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    private fun onUnlockSuccess() {
        val target = targetPackage
        if (mode == MODE_PROTECT_APP && !target.isNullOrBlank()) {
            AppLockState.setUnlocked(target)
        }
        IntruderCaptureService.logAttempt(
            this, target ?: packageName, targetLabel ?: getString(R.string.app_name), success = true
        )
        if (mode == MODE_OPEN_DASHBOARD) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        finish()
    }

    private fun onCaptureThreshold() {
        val target = targetPackage
        if (!target.isNullOrBlank()) {
            AppLockState.lock(target)
        }
        IntruderCaptureService.startCapture(
            this,
            target ?: packageName,
            targetLabel ?: getString(R.string.app_name)
        )
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        val target = targetPackage
        if (!target.isNullOrBlank()) {
            AppLockState.lock(target)
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        const val EXTRA_MODE = "mode"
        const val EXTRA_TARGET_PACKAGE = "target_package"
        const val EXTRA_TARGET_LABEL = "target_label"
        const val MODE_OPEN_DASHBOARD = "open_dashboard"
        const val MODE_PROTECT_APP = "protect_app"
    }
}
