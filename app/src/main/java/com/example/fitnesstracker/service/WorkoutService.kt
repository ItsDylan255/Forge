package com.example.fitnesstracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.fitnesstracker.MainActivity
import com.example.fitnesstracker.R
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

    /** Aktualisiert den Balken jede Sekunde und löst am Ende das Signal aus. */
    private val tick = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (restEndsAt > now) {
                notifyNow(now)
                handler.postDelayed(this, (restEndsAt - now).coerceIn(50L, 1000L))
            } else {
                signalRestOver()
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

        sessionName = intent.getStringExtra(EXTRA_SESSION_NAME).orEmpty()
        startedAt = intent.getLongExtra(EXTRA_STARTED_AT, System.currentTimeMillis())
        restEndsAt = intent.getLongExtra(EXTRA_REST_ENDS_AT, 0L)
        restTotal = intent.getLongExtra(EXTRA_REST_TOTAL, 0L)
        restLabel = intent.getStringExtra(EXTRA_REST_LABEL).orEmpty()

        // Eine neue oder übersprungene Pause ersetzt die alte, ohne Signal
        handler.removeCallbacks(tick)
        releaseWakeLock()

        val now = System.currentTimeMillis()
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

    /** Doppelter Signalton und kurze Vibration. Der Ton folgt der Benachrichtigungs-Lautstärke. */
    private fun signalRestOver() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 700)
            handler.postDelayed({ tone.release() }, 1_200L)
        } catch (_: RuntimeException) {
            // Kein Ton möglich (zum Beispiel Audio belegt): die Vibration bleibt
        }

        getSystemService(Vibrator::class.java)?.vibrate(
            VibrationEffect.createOneShot(500L, VibrationEffect.DEFAULT_AMPLITUDE)
        )
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
    }
}
