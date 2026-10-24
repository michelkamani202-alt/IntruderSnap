package com.michel.intrudersnap.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.michel.intrudersnap.R
import com.michel.intrudersnap.data.Prefs

/**
 * Création ou modification du code PIN. Le PIN est haché (SHA-256 + sel)
 * avant stockage, voir SecurityUtils / Prefs.
 */
class SetupPinActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_pin)

        val prefs = Prefs(this)
        val editPin = findViewById<TextInputEditText>(R.id.editPin)
        val editPinConfirm = findViewById<TextInputEditText>(R.id.editPinConfirm)
        val textError = findViewById<TextView>(R.id.textPinError)
        val buttonSave = findViewById<MaterialButton>(R.id.buttonSavePin)

        buttonSave.setOnClickListener {
            val pin = editPin.text?.toString().orEmpty()
            val confirm = editPinConfirm.text?.toString().orEmpty()

            when {
                pin.length < 4 || pin.length > 8 -> {
                    showError(textError, getString(R.string.setup_pin_error_length))
                }
                pin != confirm -> {
                    showError(textError, getString(R.string.setup_pin_error_mismatch))
                }
                else -> {
                    prefs.setPin(pin)
                    startActivity(
                        Intent(this, LockActivity::class.java).apply {
                            putExtra(LockActivity.EXTRA_MODE, LockActivity.MODE_OPEN_DASHBOARD)
                        }
                    )
                    finish()
                }
            }
        }
    }

    private fun showError(view: TextView, message: String) {
        view.text = message
        view.visibility = TextView.VISIBLE
    }
}
