package com.michel.intrudersnap.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogResult { SUCCESS, FAILURE, CAPTURE_SENT, CAPTURE_FAILED }

data class LogEntry(
    val timestampMillis: Long,
    val appPackage: String,
    val appLabel: String,
    val result: LogResult
) {
    fun formattedTime(): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestampMillis))
}

/**
 * Journal local des tentatives de déverrouillage, stocké en clair dans le
 * stockage privé de l'application (non accessible aux autres apps), au
 * format JSON. Consultable et effaçable à tout moment depuis le tableau
 * de bord — IntruderSnap ne garde rien de caché à son propriétaire.
 */
class AttemptLogStore(private val context: Context) {

    private val file: File get() = File(context.filesDir, "attempt_log.json")

    @Synchronized
    fun append(entry: LogEntry) {
        val all = readAll().toMutableList()
        all.add(0, entry) // plus récent en premier
        // On garde un historique raisonnable (500 dernières entrées)
        val trimmed = all.take(500)
        val array = JSONArray()
        trimmed.forEach { array.put(it.toJson()) }
        file.writeText(array.toString())
    }

    @Synchronized
    fun readAll(): List<LogEntry> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { i -> fromJson(array.getJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun clear() {
        if (file.exists()) file.delete()
    }

    private fun LogEntry.toJson(): JSONObject = JSONObject().apply {
        put("t", timestampMillis)
        put("pkg", appPackage)
        put("label", appLabel)
        put("result", result.name)
    }

    private fun fromJson(json: JSONObject): LogEntry = LogEntry(
        timestampMillis = json.getLong("t"),
        appPackage = json.getString("pkg"),
        appLabel = json.getString("label"),
        result = LogResult.valueOf(json.getString("result"))
    )
}
