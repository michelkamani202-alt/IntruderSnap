package com.michel.intrudersnap.ui

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.michel.intrudersnap.R
import com.michel.intrudersnap.data.Prefs

/**
 * Écran de consentement OBLIGATOIRE et non contournable : tant que
 * l'utilisateur n'a pas explicitement coché la case et appuyé sur
 * "J'accepte", l'application ne peut pas être configurée ni utilisée.
 * Accessible à nouveau à tout moment depuis le tableau de bord
 * ("Revoir le fonctionnement de la capture photo").
 */
class ConsentActivity : AppCompatActivity() {

    private var reviewOnly: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consent)

        reviewOnly = intent.getBooleanExtra(EXTRA_REVIEW_ONLY, false)

        val prefs = Prefs(this)
        val checkbox = findViewById<CheckBox>(R.id.checkboxConsent)
        val buttonAccept = findViewById<MaterialButton>(R.id.buttonAccept)

        if (reviewOnly) {
            // Simple relecture : déjà accepté précédemment, pas de blocage.
            checkbox.isChecked = true
            buttonAccept.isEnabled = true
            buttonAccept.setText(android.R.string.ok)
        }

        checkbox.setOnCheckedChangeListener { _, isChecked ->
            buttonAccept.isEnabled = isChecked
        }

        buttonAccept.setOnClickListener {
            prefs.consentAccepted = true
            if (reviewOnly) {
                finish()
            } else {
                val next = if (prefs.hasPin) {
                    Intent(this, LockActivity::class.java).apply {
                        putExtra(LockActivity.EXTRA_MODE, LockActivity.MODE_OPEN_DASHBOARD)
                    }
                } else {
                    Intent(this, SetupPinActivity::class.java)
                }
                startActivity(next)
                finish()
            }
        }
    }

    override fun onBackPressed() {
        if (reviewOnly) {
            super.onBackPressed()
        }
        // Sinon : le consentement est bloquant, retour désactivé tant qu'il
        // n'est pas donné (l'utilisateur peut quitter l'app via le bouton
        // Accueil du système, ce qui est son droit, mais ne peut pas
        // contourner cet écran pour utiliser l'app).
    }

    companion object {
        const val EXTRA_REVIEW_ONLY = "review_only"
    }
}
