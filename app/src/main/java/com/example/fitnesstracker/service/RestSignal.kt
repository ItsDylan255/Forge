package com.example.fitnesstracker.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Signal am Pausenende: zwei Töne und ein Vibrationsmuster.
 * Der Ton folgt der Medien-Lautstärke des Handys (bei Medien-Lautstärke 0 bleibt er stumm).
 * Die Vibration ist als Alarm gekennzeichnet, damit sie auch bei "Nicht stören" ankommt.
 */
object RestSignal {

    private val handler = Handler(Looper.getMainLooper())

    fun play(context: Context, sound: Boolean, vibration: Boolean) {
        if (sound) playTone()
        if (vibration) vibrate(context.applicationContext)
    }

    private fun playTone() {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 800)
            handler.postDelayed({ tone.release() }, 1_500L)
        } catch (_: RuntimeException) {
            // Kein Ton möglich (zum Beispiel Audio belegt): die Vibration bleibt
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(context: Context) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
        if (vibrator == null || !vibrator.hasVibrator()) return

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Zweimal kurz, mit Pause dazwischen. Als Alarm gekennzeichnet, damit sie nicht unterdrückt wird.
        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0L, 300L, 150L, 300L), -1),
            attributes
        )
    }
}
