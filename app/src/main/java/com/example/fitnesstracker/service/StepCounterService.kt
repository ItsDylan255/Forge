package com.example.fitnesstracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.fitnesstracker.MainActivity
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.StepPreferences
import com.example.fitnesstracker.data.repository.StepRepository
import com.example.fitnesstracker.domain.steps.SampleData
import com.example.fitnesstracker.domain.steps.StepCalculator
import com.example.fitnesstracker.domain.steps.StepFormat
import com.example.fitnesstracker.domain.steps.StepSamples
import com.example.fitnesstracker.i18n.tr
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

/**
 * Zählt Schritte im Hintergrund. Android liefert den Schrittzähler zuverlässig nur an Apps, die sichtbar
 * sind oder einen Vordergrund-Dienst haben. Der Dienst hält ihn deshalb dauerhaft angemeldet und zeigt
 * eine kleine, stumme Benachrichtigung mit den Schritten von heute.
 *
 * Der Dienst speichert nur Messwerte (Zeitpunkt, Rohwert, Gerätestart). Die Tageswerte berechnet das
 * StepRepository daraus.
 */
class StepCounterService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var sensorManager: SensorManager
    private lateinit var repository: StepRepository
    private lateinit var prefs: StepPreferences

    private var listening = false
    private var notificationJob = false

    /** Der neueste, noch nicht gespeicherte Wert. */
    private var pending: SampleData? = null
    private var lastStoreAt = 0L

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val total = event.values.firstOrNull()?.toLong() ?: return
            val nowMillis = System.currentTimeMillis()
            val elapsedNanos = SystemClock.elapsedRealtimeNanos()

            // Der Zeitstempel des Sensors sagt, wann die Schritte gegangen wurden, auch wenn Android
            // den Wert erst später liefert
            pending = SampleData(
                timeMillis = StepSamples.eventTime(event.timestamp, elapsedNanos, nowMillis),
                total = total,
                bootMillis = StepSamples.bootMillis(elapsedNanos, nowMillis)
            )

            // Gespeichert wird höchstens alle 30 Sekunden, der letzte Wert kommt nach der Pause
            val wait = (StepSamples.MIN_INTERVAL_CHANGED_MILLIS - (SystemClock.elapsedRealtime() - lastStoreAt))
                .coerceAtLeast(0L)
            handler.removeCallbacks(storePending)
            handler.postDelayed(storePending, wait)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val storePending = Runnable {
        val sample = pending ?: return@Runnable
        pending = null
        lastStoreAt = SystemClock.elapsedRealtime()
        scope.launch { repository.recordSample(sample) }
    }

    /**
     * Alle 30 Minuten den Sensor neu anmelden. Das liefert sofort den aktuellen Wert, auch wenn sich
     * nichts geändert hat. So ist auch eine Zeit ohne Schritte (zum Beispiel die Nacht) belegt.
     */
    private val heartbeat = object : Runnable {
        override fun run() {
            unregister()
            register()
            handler.postDelayed(this, HEARTBEAT_MILLIS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        running = true
        // Hält auch die Kachel auf der Uhr aktuell, wenn die App geschlossen ist
        LiveSync.start(this)

        sensorManager = getSystemService(SensorManager::class.java)
        prefs = StepPreferences(this)
        val database = DatabaseProvider.getDatabase(this)
        repository = StepRepository(database.stepEntryDao(), database.stepSampleDao(), prefs)

        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, tr("Schritte", "Steps"), NotificationManager.IMPORTANCE_MIN).apply {
                description = tr("Zählt deine Schritte im Hintergrund", "Counts your steps in the background")
                setShowBadge(false)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification(todaySteps = 0)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        register()

        handler.removeCallbacks(heartbeat)
        handler.postDelayed(heartbeat, HEARTBEAT_MILLIS)

        // Die Benachrichtigung zeigt die Schritte von heute und wird höchstens alle 3 Sekunden aktualisiert
        if (!notificationJob) {
            notificationJob = true
            scope.launch {
                repository.observeEntries().conflate().collect { entries ->
                    val today = LocalDate.now().toEpochDay()
                    val steps = entries.firstOrNull { it.dateEpochDay == today }?.steps ?: 0
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, buildNotification(steps))
                    delay(3_000L)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        unregister()
        // Einen noch nicht gespeicherten Wert nicht verlieren
        pending?.let { sample -> scope.launch { repository.recordSample(sample) } }
        running = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun register() {
        if (listening) return
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        listening = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun unregister() {
        if (!listening) return
        sensorManager.unregisterListener(listener)
        listening = false
    }

    private fun buildNotification(todaySteps: Int): Notification {
        val progress = StepCalculator.goalProgress(todaySteps, prefs.getStepGoal())
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentTitle(tr("${StepFormat.steps(progress.steps)} Schritte heute", "${StepFormat.steps(progress.steps)} steps today"))
            .setContentText(tr("Ziel ${StepFormat.steps(progress.goal)} \u00B7 ${StepFormat.percent(progress.percent)}", "Goal ${StepFormat.steps(progress.goal)} · ${StepFormat.percent(progress.percent)}"))
            .setProgress(100, progress.percent.coerceIn(0, 100), false)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "steps"
        private const val NOTIFICATION_ID = 1002
        private const val HEARTBEAT_MILLIS = 30 * 60 * 1000L

        /** true, solange der Dienst läuft (für die Diagnose in den Einstellungen). */
        @Volatile
        var running: Boolean = false
            private set
    }
}
