package com.example.fitnesstracker.ui.steps

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.StepPreferences
import com.example.fitnesstracker.data.repository.StepRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StepsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseProvider.getDatabase(application)

    private val repository = StepRepository(
        dao = database.stepEntryDao(),
        prefs = StepPreferences(application)
    )

    private val sensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val stepSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    /** false = Gerät hat keinen Schrittzähler-Sensor. */
    val isSensorAvailable: Boolean = stepSensor != null

    val entries = repository.observeEntries().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val stepGoal = repository.stepGoal

    private var isListening = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val total = event.values.firstOrNull()?.toLong() ?: return
            viewModelScope.launch {
                repository.recordSensorTotal(total, LocalDate.now().toEpochDay())
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    /** Registriert den Sensor. Ruft man es mehrfach auf, passiert nichts Zusätzliches. */
    fun startListening() {
        val sensor = stepSensor ?: return
        if (isListening) return

        isListening = sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun stopListening() {
        if (!isListening) return
        sensorManager.unregisterListener(listener)
        isListening = false
    }

    fun setStepGoal(goal: Int) {
        repository.setStepGoal(goal)
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }
}
