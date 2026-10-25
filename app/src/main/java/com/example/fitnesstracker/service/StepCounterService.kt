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
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.fitnesstracker.MainActivity
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.StepPreferences
import com.example.fitnesstracker.data.repository.StepRepository
import com.example.fitnesstracker.domain.steps.StepCalculator
import com.example.fitnesstracker.domain.steps.StepDayBoundary
import com.example.fitnesstracker.domain.steps.StepFormat
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

/**
 * Zählt Schritte im Hintergrund. Android liefert Sensorwerte zuverlässig nur an Apps, die sichtbar
 * sind oder einen Vordergrund-Dienst haben. Der Dienst hält deshalb den Schrittzähler dauerhaft
 * angemeldet und zeigt eine kleine, stumme Benachrichtigung mit den Schritten von heute.
 */
class StepCounterService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var sensorManager: SensorManager
    private lateinit var repository: StepRepository
    private lateinit var prefs: StepPreferences

    private var listening = false

    /** Bis zu diesem Zeitpunkt zählen neue Werte noch zum Vortag (Nachtmessung um Mitternacht). */
    @Volatile
    private var midnightUntilMillis = 0L

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val total = event.values.firstOrNull()?.toLong() ?: return
            val nowMillis = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()

            // Der Zeitstempel des Sensors sagt, wann die Schritte gegangen wurden, auch wenn Android
            // den Wert erst später liefert. In der Nachtmessung gilt der Vortag.
            val day = if (nowMillis <= midnightUntilMillis) {
                LocalDate.now(zone).toEpochDay() - 1
            } else {
                StepDayBoundary.dayOfEvent(event.timestamp, SystemClock.elapsedRealtimeNanos(), nowMillis, zone)
            }

            scope.launch { repository.recordSensorTotal(total, day, "Dienst") }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    override fun onCreate() {
        super.onCreate()
        running = true

        sensorManager = getSystemService(SensorManager::class.java)
        prefs = StepPreferences(this)
        repository = StepRepository(
            dao = DatabaseProvider.getDatabase(this).stepEntryDao(),
            prefs = prefs
        )

        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Schritte", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Zählt deine Schritte im Hintergrund"
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

        if (intent?.action == ACTION_MIDNIGHT) {
            // Neu anmelden liefert sofort den aktuellen Wert, der noch zum Vortag zählt
            midnightUntilMillis = System.currentTimeMillis() + MIDNIGHT_WINDOW_MILLIS
            unregister()
        }
        register()

        // Die Benachrichtigung zeigt die Schritte von heute und wird höchstens alle 3 Sekunden aktualisiert
        scope.launch {
            repository.observeEntries().conflate().collect { entries ->
                val today = LocalDate.now().toEpochDay()
                val steps = entries.firstOrNull { it.dateEpochDay == today }?.steps ?: 0
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification(steps))
                delay(3_000L)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        unregister()
        scope.cancel()
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
            .setContentTitle("${StepFormat.steps(progress.steps)} Schritte heute")
            .setContentText("Ziel ${StepFormat.steps(progress.goal)} \u00B7 ${StepFormat.percent(progress.percent)}")
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
        const val ACTION_MIDNIGHT = "com.example.fitnesstracker.STEP_MIDNIGHT"

        private const val CHANNEL_ID = "steps"
        private const val NOTIFICATION_ID = 1002
        private const val MIDNIGHT_WINDOW_MILLIS = 8_000L

        /** true, solange der Dienst läuft (für die Diagnose in den Einstellungen). */
        @Volatile
        var running: Boolean = false
            private set
    }
}
