package com.michel.intrudersnap.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Ne fait rien de "caché" : le service d'accessibilité (déjà autorisé
 * explicitement par l'utilisateur dans les réglages système) reste actif
 * après redémarrage comme n'importe quel service d'accessibilité Android.
 * Ce récepteur sert uniquement de point d'ancrage si une réinitialisation
 * d'état est nécessaire au démarrage ; aucune action réseau ou caméra n'est
 * déclenchée ici.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Rien à faire explicitement : le service d'accessibilité est relancé
        // par le système lui-même une fois réactivé par l'utilisateur.
    }
}
