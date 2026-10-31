package com.example.fitnesstracker.wear

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.example.fitnesstracker.domain.wear.WatchToday
import com.example.fitnesstracker.domain.wear.WatchWorkoutState
import com.example.fitnesstracker.i18n.L10n
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Verbindung zur Uhr über die Wearable Data Layer (Bluetooth, über Google Play-Dienste).
 * Das Handy schickt den Stand, die Uhr zeigt ihn nur an. Ohne Uhr oder ohne Play-Dienste
 * passiert einfach nichts.
 *
 * Die Pfade und Schlüssel müssen mit `WearProtocol` im Modul `wear` übereinstimmen.
 */
object WearBridge {

    const val PATH_WORKOUT = "/forge/workout"
    const val PATH_TODAY = "/forge/today"
    const val PATH_REST_EXTEND = "/forge/rest/extend"
    const val PATH_REST_SKIP = "/forge/rest/skip"
    const val PATH_REST_OVER = "/forge/rest/over"

    /** So viele Sekunden verlängert „+30 s“ auf der Uhr die Pause. */
    const val EXTEND_SECONDS = 30

    private const val WATCH_URI = "forge://workout"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** [state] null = kein Training läuft. */
    suspend fun sendWorkout(context: Context, state: WatchWorkoutState?) {
        put(context, PATH_WORKOUT) {
            putBoolean("active", state != null)
            putString("lang", L10n.languageCode)
            if (state == null) return@put
            putLong("sessionId", state.sessionId)
            putString("routine", state.routineName)
            putLong("startedAt", state.startedAt)
            putString("exercise", state.exerciseName.orEmpty())
            putString("set", state.setText.orEmpty())
            putString("target", state.targetText.orEmpty())
            putString("last", state.lastText.orEmpty())
            putString("progress", state.progressText.orEmpty())
            putBoolean("superset", state.superset)
            putString("record", state.recordText.orEmpty())
            putString("restLabel", state.rest?.label.orEmpty())
            putLong("restEndsAt", state.rest?.endsAtMillis ?: 0L)
            putLong("restTotal", state.rest?.totalMillis ?: 0L)
        }
    }

    suspend fun sendToday(context: Context, today: WatchToday) {
        put(context, PATH_TODAY) {
            putLong("date", today.dateEpochDay)
            putString("lang", L10n.languageCode)
            putInt("steps", today.steps)
            putInt("stepGoal", today.stepGoal)
            putDouble("weight", today.weightKg ?: -1.0)
            putInt("kcal", today.kcal ?: -1)
            putInt("kcalTarget", today.kcalTarget ?: -1)
        }
    }

    /** Meldet der Uhr das Pausenende, damit sie am Handgelenk vibriert. */
    fun sendRestOver(context: Context) {
        val app = context.applicationContext
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(app).connectedNodes.await()
                val messages = Wearable.getMessageClient(app)
                nodes.forEach { messages.sendMessage(it.id, PATH_REST_OVER, ByteArray(0)).await() }
            } catch (_: Exception) {
                // Keine Uhr verbunden
            }
        }
    }

    /** Öffnet die Trainingsansicht auf allen verbundenen Uhren. */
    fun openOnWatch(context: Context) {
        val app = context.applicationContext
        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(app).connectedNodes.await()
                if (nodes.isEmpty()) return@launch
                val helper = RemoteActivityHelper(app, Executors.newSingleThreadExecutor())
                val intent = Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(Uri.parse(WATCH_URI))
                nodes.forEach { helper.startRemoteActivity(intent, it.id) }
            } catch (_: Exception) {
                // Keine Uhr oder keine Forge-App auf der Uhr
            }
        }
    }

    private suspend fun put(context: Context, path: String, fill: com.google.android.gms.wearable.DataMap.() -> Unit) {
        try {
            val request = PutDataMapRequest.create(path).apply { dataMap.fill() }
                .asPutDataRequest()
                .setUrgent()
            Wearable.getDataClient(context.applicationContext).putDataItem(request).await()
        } catch (_: Exception) {
            // Keine Play-Dienste oder die Data Layer ist nicht erreichbar: die App läuft normal weiter
        }
    }
}
