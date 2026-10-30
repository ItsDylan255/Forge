package com.example.fitnesstracker.ui.steps

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.StepPreferences
import com.example.fitnesstracker.data.repository.StepRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/** Schritte-Screen: zeigt die Tageswerte. Gezählt wird vom StepCounterService, nicht hier. */
class StepsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DatabaseProvider.getDatabase(application)

    private val repository = StepRepository(
        entryDao = database.stepEntryDao(),
        sampleDao = database.stepSampleDao(),
        prefs = StepPreferences(application)
    )

    /** false = Gerät hat keinen Schrittzähler-Sensor. */
    val isSensorAvailable: Boolean =
        (application.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
            .getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    val entries = repository.observeEntries().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val stepGoal = repository.stepGoal

    fun setStepGoal(goal: Int) {
        repository.setStepGoal(goal)
    }
}
