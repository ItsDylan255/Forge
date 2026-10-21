package com.example.fitnesstracker.service

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.StepPreferences
import com.example.fitnesstracker.data.repository.StepRepository
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Liest den Schrittzähler im Hintergrund (alle ca. 15 Minuten), auch wenn die App geschlossen ist.
 * Dadurch landen die Schritte bei dem Tag, an dem sie gegangen wurden, und nicht erst beim
 * nächsten Öffnen des Schritte-Screens auf "heute".
 */
class StepSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext

        // Ohne die Berechtigung "Körperliche Aktivität" liefert der Sensor keine Werte
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return Result.success()

        val total = readStepTotal(context) ?: return Result.success()

        val repository = StepRepository(
            dao = DatabaseProvider.getDatabase(context).stepEntryDao(),
            prefs = StepPreferences(context)
        )
        repository.recordSensorTotal(total, LocalDate.now().toEpochDay())

        return Result.success()
    }

    /** Wartet kurz auf den aktuellen Rohwert des Sensors. Null, wenn keiner kommt. */
    private suspend fun readStepTotal(context: Context): Long? {
        val manager = context.getSystemService(SensorManager::class.java) ?: return null
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null

        return withTimeoutOrNull(SENSOR_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        manager.unregisterListener(this)
                        if (continuation.isActive) {
                            continuation.resume(event.values.firstOrNull()?.toLong())
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

    private companion object {
        const val SENSOR_TIMEOUT_MILLIS = 10_000L
    }
}
