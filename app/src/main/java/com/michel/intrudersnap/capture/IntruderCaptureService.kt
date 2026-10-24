package com.michel.intrudersnap.capture

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.michel.intrudersnap.IntruderSnapApp
import com.michel.intrudersnap.R
import com.michel.intrudersnap.data.AttemptLogStore
import com.michel.intrudersnap.data.LogEntry
import com.michel.intrudersnap.data.LogResult
import com.michel.intrudersnap.data.Prefs
import com.michel.intrudersnap.mail.EmailSender
import com.michel.intrudersnap.ui.SettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Service de premier plan qui effectue la capture photo puis l'envoi email
 * lorsqu'un intrus a dépassé le nombre d'échecs de PIN autorisé.
 *
 * Conformément aux règles Android (et par souci de transparence : voir
 * l'écran de consentement), un service de premier plan DOIT afficher une
 * notification visible tant qu'il tourne — IntruderSnap ne fonctionne
 * jamais en silence complet pendant la capture.
 */
class IntruderCaptureService : LifecycleService() {

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val pkg = intent?.getStringExtra(EXTRA_PACKAGE) ?: packageName
        val label = intent?.getStringExtra(EXTRA_LABEL) ?: getString(R.string.app_name)

        startForegroundNotification()
        runCapture(pkg, label)

        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, SettingsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification = NotificationCompat.Builder(this, IntruderSnapApp.CHANNEL_CAPTURE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notif_capture_title))
            .setContentText(getString(R.string.notif_capture_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()

        // ServiceCompat.startForeground gère elle-même les différences entre
        // versions d'Android (le type "camera" n'existe qu'à partir de l'API 29,
        // ServiceCompat l'ignore proprement sur les versions plus anciennes).
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } catch (e: Exception) {
            android.util.Log.e("IntruderCaptureService", "startForeground failed", e)
        }
    }

    private fun runCapture(pkg: String, label: String) {
        val capturesDir = File(cacheDir, "captures").apply { mkdirs() }
        val fileName = "intrus_${System.currentTimeMillis()}.jpg"
        val outputFile = File(capturesDir, fileName)

        CameraCapture.capture(this, this, outputFile) { success, file ->
            lifecycleScope.launch {
                val prefs = Prefs(this@IntruderCaptureService)
                val timeText = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

                var sent = false
                if (success && file != null && prefs.emailConfigured) {
                    sent = withContext(Dispatchers.IO) {
                        EmailSender.sendCaptureAlert(prefs, file, label, timeText)
                    }
                }

                AttemptLogStore(this@IntruderCaptureService).append(
                    LogEntry(
                        timestampMillis = System.currentTimeMillis(),
                        appPackage = pkg,
                        appLabel = label,
                        result = if (sent) LogResult.CAPTURE_SENT else LogResult.CAPTURE_FAILED
                    )
                )

                // La photo locale n'a plus d'utilité une fois envoyée (ou si
                // l'envoi a échoué et ne sera pas retenté) : on la supprime
                // pour ne rien laisser traîner sur l'appareil.
                file?.delete()

                stopSelf()
            }
        }
    }

    companion object {
        private const val NOTIF_ID = 42
        private const val EXTRA_PACKAGE = "extra_package"
        private const val EXTRA_LABEL = "extra_label"

        /** Démarre la capture + l'envoi d'alerte (seuil d'échecs atteint). */
        fun startCapture(context: Context, pkg: String, label: String) {
            val intent = Intent(context, IntruderCaptureService::class.java).apply {
                putExtra(EXTRA_PACKAGE, pkg)
                putExtra(EXTRA_LABEL, label)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Enregistre une tentative réussie ou un simple échec (sans capture) dans le journal. */
        fun logAttempt(context: Context, pkg: String, label: String, success: Boolean) {
            AttemptLogStore(context).append(
                LogEntry(
                    timestampMillis = System.currentTimeMillis(),
                    appPackage = pkg,
                    appLabel = label,
                    result = if (success) LogResult.SUCCESS else LogResult.FAILURE
                )
            )
        }
    }
}
