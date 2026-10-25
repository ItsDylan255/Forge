package com.example.fitnesstracker.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Ein gelesener Wert des Schrittzählers: Rohwert seit Gerätestart und der Zeitstempel des Sensors. */
data class StepReading(val total: Long, val timestampNanos: Long)

/** Liest einmalig den Schrittzähler. Klappt zuverlässig nur, solange die App sichtbar ist oder ein Dienst läuft. */
object StepSensorReader {

    private const val TIMEOUT_MILLIS = 8_000L

    /** Ohne die Berechtigung "Körperliche Aktivität" liefert der Sensor keine Werte. */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED

    suspend fun readTotal(context: Context): Long? = readEvent(context)?.total

    /** Wartet kurz auf den aktuellen Wert. Null, wenn kein Sensor da ist oder keiner kommt. */
    suspend fun readEvent(context: Context): StepReading? {
        val manager = context.getSystemService(SensorManager::class.java) ?: return null
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null

        return withTimeoutOrNull(TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        manager.unregisterListener(this)
                        val total = event.values.firstOrNull()?.toLong()
                        if (continuation.isActive) {
                            continuation.resume(total?.let { StepReading(it, event.timestamp) })
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }

                val registered = manager.registerListener(
                    listener, sensor, SensorManager.SENSOR_DELAY_NORMAL
                )
                if (!registered && continuation.isActive) continuation.resume(null)

                continuation.invokeOnCancellation { manager.unregisterListener(listener) }
            }
        }
    }
}
