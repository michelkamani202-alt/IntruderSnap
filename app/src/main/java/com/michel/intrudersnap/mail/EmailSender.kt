package com.michel.intrudersnap.mail

import android.util.Log
import com.michel.intrudersnap.data.Prefs
import java.io.File
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/**
 * Envoi des alertes par email via SMTP, avec les identifiants configurés par
 * l'utilisateur lui-même dans le tableau de bord (voir Prefs — chiffrés sur
 * l'appareil, jamais transmis ailleurs qu'au serveur SMTP choisi).
 *
 * Doit être appelé depuis un thread d'arrière-plan (jamais le thread UI).
 */
object EmailSender {

    private const val TAG = "EmailSender"

    private fun buildSession(prefs: Prefs): Session {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", prefs.smtpHost)
            put("mail.smtp.port", prefs.smtpPort ?: "587")
            put("mail.smtp.ssl.trust", prefs.smtpHost)
            put("mail.smtp.connectiontimeout", "15000")
            put("mail.smtp.timeout", "15000")
        }
        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication =
                PasswordAuthentication(prefs.smtpUser, prefs.smtpPass)
        })
    }

    /** Envoie la photo capturée après échec de déverrouillage. */
    fun sendCaptureAlert(prefs: Prefs, photoFile: File, appLabel: String, whenText: String): Boolean {
        return try {
            val session = buildSession(prefs)
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(prefs.smtpUser))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(prefs.recipientEmail))
                subject = "IntruderSnap — tentative de déverrouillage refusée"
            }

            val textPart = MimeBodyPart().apply {
                setText(
                    "Une tentative de déverrouillage a échoué le nombre maximal de fois autorisé " +
                        "sur \"$appLabel\" le $whenText.\n\n" +
                        "Une photo a été prise automatiquement avec la caméra avant et est jointe " +
                        "à cet email.\n\n" +
                        "— Envoyé automatiquement par IntruderSnap, installé sur votre appareil."
                )
            }

            val attachmentPart = MimeBodyPart().apply {
                attachFile(photoFile)
            }

            val multipart = MimeMultipart().apply {
                addBodyPart(textPart)
                addBodyPart(attachmentPart)
            }

            message.setContent(multipart)
            Transport.send(message)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Échec de l'envoi de l'alerte", e)
            false
        }
    }

    /** Envoie un email de test sans pièce jointe, pour valider la configuration SMTP. */
    fun sendTestEmail(prefs: Prefs): Result<Unit> {
        return try {
            val session = buildSession(prefs)
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(prefs.smtpUser))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(prefs.recipientEmail))
                subject = "IntruderSnap — email de test"
                setText(
                    "Ceci est un email de test envoyé depuis IntruderSnap.\n" +
                        "Si vous le recevez, la configuration SMTP est correcte."
                )
            }
            Transport.send(message)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Échec de l'email de test", e)
            Result.failure(e)
        }
    }
}
