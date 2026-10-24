package com.michel.intrudersnap

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class IntruderSnapApp : Application() {

    companion object {
        const val CHANNEL_CAPTURE = "intrudersnap_capture"
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_CAPTURE,
                getString(R.string.notif_channel_capture),
                // IMPORTANCE_LOW : pas de son, discret, mais service de premier plan visible
                // conformément aux règles Android (un service en premier plan DOIT notifier
                // l'utilisateur — IntruderSnap ne cache jamais sa propre présence sur l'appareil).
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
