package com.michel.intrudersnap.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.michel.intrudersnap.data.Prefs

/**
 * Point d'entrée de l'application : route vers le bon écran selon l'état
 * de configuration (consentement -> PIN -> déverrouillage -> tableau de bord).
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)

        val next = when {
            !prefs.consentAccepted -> Intent(this, ConsentActivity::class.java)
            !prefs.hasPin -> Intent(this, SetupPinActivity::class.java)
            else -> Intent(this, LockActivity::class.java).apply {
                putExtra(LockActivity.EXTRA_MODE, LockActivity.MODE_OPEN_DASHBOARD)
            }
        }
        startActivity(next)
        finish()
    }
}
