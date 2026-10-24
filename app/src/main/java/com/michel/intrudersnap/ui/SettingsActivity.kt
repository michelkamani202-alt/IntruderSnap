package com.michel.intrudersnap.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import android.text.TextUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.michel.intrudersnap.R
import com.michel.intrudersnap.data.Prefs
import com.michel.intrudersnap.mail.EmailSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tableau de bord principal : état du service, PIN, configuration email,
 * choix des applications protégées, journal des tentatives.
 * Accessible uniquement après saisie du code PIN (voir LockActivity).
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    // Permission CAMERA : indispensable pour que la capture fonctionne. Sans elle,
    // CameraX échoue silencieusement et aucune photo/email n'est jamais produit.
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "Sans l'autorisation caméra, IntruderSnap ne pourra pas prendre de photo en cas d'échec de code.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Permission POST_NOTIFICATIONS (Android 13+) : nécessaire pour que la notification
    // permanente de transparence s'affiche réellement pendant la capture.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* pas bloquant si refusée : la capture fonctionne quand même */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)
        requestRequiredPermissions()

        findViewById<TextInputEditText>(R.id.editMaxAttempts).setText(prefs.maxAttempts.toString())
        findViewById<TextInputEditText>(R.id.editSmtpHost).setText(prefs.smtpHost)
        findViewById<TextInputEditText>(R.id.editSmtpPort).setText(prefs.smtpPort)
        findViewById<TextInputEditText>(R.id.editSmtpUser).setText(prefs.smtpUser)
        findViewById<TextInputEditText>(R.id.editSmtpPass).setText(prefs.smtpPass)
        findViewById<TextInputEditText>(R.id.editRecipient).setText(prefs.recipientEmail)

        findViewById<MaterialButton>(R.id.buttonChangePin).setOnClickListener {
            startActivity(Intent(this, SetupPinActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.buttonSaveEmail).setOnClickListener {
            saveEmailConfig()
        }

        findViewById<MaterialButton>(R.id.buttonTestEmail).setOnClickListener {
            saveEmailConfig()
            sendTestEmail()
        }

        findViewById<MaterialButton>(R.id.buttonChooseApps).setOnClickListener {
            startActivity(Intent(this, ProtectedAppsActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.buttonViewLog).setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.buttonClearLog).setOnClickListener {
            com.michel.intrudersnap.data.AttemptLogStore(this).clear()
            Toast.makeText(this, R.string.toast_config_saved, Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.buttonReviewConsent).setOnClickListener {
            startActivity(
                Intent(this, ConsentActivity::class.java).apply {
                    putExtra(ConsentActivity.EXTRA_REVIEW_ONLY, true)
                }
            )
        }

        findViewById<TextView>(R.id.textAccessibilityStatus).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<TextView>(R.id.textCameraStatus).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Si l'utilisateur a déjà refusé une fois, Android n'affiche plus la
                // boîte de dialogue standard : on l'envoie directement dans les
                // réglages de l'application pour qu'il l'active manuellement.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateAccessibilityStatus()
        updateCameraStatus()
    }

    private fun updateCameraStatus() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        findViewById<TextView>(R.id.textCameraStatus).text = getString(
            if (granted) R.string.status_camera_on else R.string.status_camera_off
        )
    }

    private fun requestRequiredPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun updateAccessibilityStatus() {
        val statusView = findViewById<TextView>(R.id.textAccessibilityStatus)
        val enabledServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val serviceName = "$packageName/${com.michel.intrudersnap.lock.AppLockAccessibilityService::class.java.name}"
        val isEnabled = enabledServices.split(":").any { it.equals(serviceName, ignoreCase = true) }

        statusView.text = if (isEnabled) {
            getString(R.string.status_accessibility_on)
        } else {
            getString(R.string.status_accessibility_off)
        }
    }

    private fun saveEmailConfig() {
        prefs.smtpHost = findViewById<TextInputEditText>(R.id.editSmtpHost).text?.toString()?.trim()
        prefs.smtpPort = findViewById<TextInputEditText>(R.id.editSmtpPort).text?.toString()?.trim()
        prefs.smtpUser = findViewById<TextInputEditText>(R.id.editSmtpUser).text?.toString()?.trim()
        // On retire les espaces et sauts de ligne invisibles qui peuvent se glisser lors
        // d'un copier-coller du mot de passe d'application (cause fréquente d'échec SMTP
        // "535 Username and Password not accepted" alors que le mot de passe est correct).
        prefs.smtpPass = findViewById<TextInputEditText>(R.id.editSmtpPass).text
            ?.toString()
            ?.replace(Regex("\\s"), "")
        prefs.recipientEmail = findViewById<TextInputEditText>(R.id.editRecipient).text?.toString()?.trim()

        val maxAttemptsText = findViewById<TextInputEditText>(R.id.editMaxAttempts).text?.toString()
        maxAttemptsText?.toIntOrNull()?.let { prefs.maxAttempts = it }

        Toast.makeText(this, R.string.toast_config_saved, Toast.LENGTH_SHORT).show()
    }

    private fun sendTestEmail() {
        if (!prefs.emailConfigured) return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { EmailSender.sendTestEmail(prefs) }
            result.onSuccess {
                Toast.makeText(this@SettingsActivity, R.string.toast_email_test_sent, Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(
                    this@SettingsActivity,
                    getString(R.string.toast_email_test_failed, it.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}