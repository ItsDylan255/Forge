package com.example.fitnesstracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.fitnesstracker.MainActivity
import com.example.fitnesstracker.R
import com.example.fitnesstracker.data.local.WorkoutPreferences
import com.example.fitnesstracker.domain.gym.WorkoutNotificationPlanner

/**
 * Hält während eines Trainings eine dauerhafte Benachrichtigung bereit: Trainingsdauer, bei einer
 * Pause die verbleibende Zeit mit Balken. Am Pausenende gibt es Signalton und Vibration, auch wenn
 * die App im Hintergrund ist oder der Bildschirm aus ist.
 */
class WorkoutService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null

    private var sessionName = ""
    private var startedAt = 0L
    private var restEndsAt = 0L
    private var restTotal = 0L
    private var restLabel = ""

    /** true, sobald für die aktuelle Pause das Signal kam (oder sie schon vorbei war). */
    private var restSignalled = true

    /** Aktualisiert den Balken jede Sekunde und löst am Ende das Signal aus. */
    private val tick = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (restEndsAt > now) {
                notifyNow(now)
                handler.postDelayed(this, (restEndsAt - now).coerceIn(50L, 1000L))
            } else {
                if (!restSignalled) {
                    restSignalled = true
                    signalRestOver()
                }
                releaseWakeLock()
                notifyNow(now)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Training", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Laufendes Training mit Pausen-Countdown"
                setShowBadge(false)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val now = System.currentTimeMillis()
        val newEndsAt = intent.getLongExtra(EXTRA_REST_ENDS_AT, 0L)

        // Die App beendet eine abgelaufene Pause im selben Moment, in dem der Dienst das Signal geben
        // wollte. Endete die alte Pause gerade erst (und wurde nicht vorher übersprungen), kommt das
        // Signal deshalb jetzt, genau einmal.
        if (restEndsAt > 0L && !restSignalled && newEndsAt != restEndsAt &&
            restEndsAt <= now + SIGNAL_TOLERANCE_MILLIS
        ) {
            signalRestOver()
        }
        // Eine bereits abgelaufene Pause (zum Beispiel beim erneuten Senden) meldet nichts mehr
        if (newEndsAt != restEndsAt) restSignalled = newEndsAt <= now

        sessionName = intent.getStringExtra(EXTRA_SESSION_NAME).orEmpty()
        startedAt = intent.getLongExtra(EXTRA_STARTED_AT, System.currentTimeMillis())
        restEndsAt = newEndsAt
        restTotal = intent.getLongExtra(EXTRA_REST_TOTAL, 0L)
        restLabel = intent.getStringExtra(EXTRA_REST_LABEL).orEmpty()

        // Eine neue oder übersprungene Pause ersetzt die alte
        handler.removeCallbacks(tick)
        releaseWakeLock()

        val notification = buildNotification(now)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (restEndsAt > now) {
            // Hält die CPU bis zum Ende der Pause wach, damit das Signal pünktlich kommt
            acquireWakeLock(restEndsAt - now + 5_000L)
            handler.postDelayed(tick, (restEndsAt - now).coerceIn(50L, 1000L))
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(now: Long) = WorkoutNotificationPlanner
        .plan(sessionName, startedAt, restEndsAt, restTotal, restLabel, now)
        .let { plan ->
            val open = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_workout)
                .setContentTitle(plan.title)
                .setContentText(plan.text)
                .setContentIntent(open)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setWhen(plan.chronometerBase)
                .setChronometerCountDown(plan.countDown)
                .apply { if (plan.progressMax > 0) setProgress(plan.progressMax, plan.progress, false) }
                .build()
        }

    private fun notifyNow(now: Long) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(now))
    }

    /** Signalton und Vibration, so wie in den Einstellungen gewählt. */
    private fun signalRestOver() {
        val prefs = WorkoutPreferences(this)
        RestSignal.play(this, prefs.isSoundEnabled(), prefs.isVibrationEnabled())
    }

    private fun acquireWakeLock(timeoutMillis: Long) {
        val powerManager = getSystemService(PowerManager::class.java) ?: return
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "forge:rest").apply {
            setReferenceCounted(false)
            acquire(timeoutMillis)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    companion object {
        const val EXTRA_SESSION_NAME = "session_name"
        const val EXTRA_STARTED_AT = "started_at"
        const val EXTRA_REST_LABEL = "rest_label"
        const val EXTRA_REST_ENDS_AT = "rest_ends_at"
        const val EXTRA_REST_TOTAL = "rest_total"

        private const val CHANNEL_ID = "workout"
        private const val NOTIFICATION_ID = 1001

        /** So knapp vor dem Ende gilt eine Pause als abgelaufen. */
        private const val SIGNAL_TOLERANCE_MILLIS = 400L
    }
}
