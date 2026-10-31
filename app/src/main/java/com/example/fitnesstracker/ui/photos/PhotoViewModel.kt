package com.example.fitnesstracker.ui.photos

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.data.local.PhotoReminderPreferences
import com.example.fitnesstracker.data.local.ProgressPhoto
import com.example.fitnesstracker.data.local.WeightEntry
import com.example.fitnesstracker.data.photos.PhotoStorage
import com.example.fitnesstracker.domain.calories.CalorieCalculator
import com.example.fitnesstracker.domain.calories.CalorieGoalInput
import com.example.fitnesstracker.domain.calories.toCalorieProfile
import com.example.fitnesstracker.domain.photos.PhotoDue
import com.example.fitnesstracker.domain.photos.PhotoReminderCalculator
import com.example.fitnesstracker.domain.photos.PhotoStatsCalculator
import com.example.fitnesstracker.i18n.tr
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Fortschrittsfotos: Liste, Hinzufügen (mit Momentaufnahme der Werte) und Löschen. */
class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val dao = db.progressPhotoDao()
    private val reminderPrefs = PhotoReminderPreferences(application)

    val photos: StateFlow<List<ProgressPhoto>> = dao.observeAll().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    /** true, solange für heute schon ein Gewicht eingetragen ist. */
    val hasWeightToday: StateFlow<Boolean> = db.weightEntryDao().getAllEntries()
        .map { list -> list.any { it.dateEpochDay == LocalDate.now().toEpochDay() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    /** Nächster Foto-Termin laut Einstellungen, null wenn keine Erinnerung eingestellt ist. */
    fun due(lastPhotoDay: Long?): PhotoDue? = PhotoReminderCalculator.status(
        reminderPrefs.getSchedule(), lastPhotoDay, reminderPrefs.getAnchorDay(), LocalDate.now().toEpochDay()
    )

    /** Heutiges Gewicht speichern (vor dem Foto verlangt). */
    fun saveTodayWeight(weightKg: Double, onSaved: () -> Unit) {
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            val weights = db.weightEntryDao()
            val existing = weights.getEntryForDate(today)
            if (existing == null) {
                weights.insertEntry(WeightEntry(weightKg = weightKg, dateEpochDay = today))
            } else {
                weights.updateEntry(existing.copy(weightKg = weightKg, updatedAt = System.currentTimeMillis()))
            }
            onSaved()
        }
    }

    /** Bild übernehmen und mit den Werten von heute speichern. */
    fun addPhoto(source: Uri, onDone: () -> Unit = {}) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val fileName = withContext(Dispatchers.IO) { PhotoStorage.import(app, source) }
                dao.insert(snapshot(fileName))
                onDone()
            } catch (e: Exception) {
                Log.w(TAG, "Foto konnte nicht gespeichert werden", e)
                _error.value = tr("Das Bild konnte nicht gespeichert werden.", "The photo couldn't be saved.")
            } finally {
                _saving.value = false
            }
        }
    }

    fun delete(photo: ProgressPhoto) {
        viewModelScope.launch {
            dao.delete(photo)
            withContext(Dispatchers.IO) { PhotoStorage.delete(getApplication(), photo.fileName) }
        }
    }

    private suspend fun snapshot(fileName: String): ProgressPhoto {
        val today = LocalDate.now().toEpochDay()
        val weights = db.weightEntryDao().getAllEntries().first()
        val goal = db.weightGoalDao().observeGoal().first()
        val sessions = db.gymDao().observeSessions().first().filter { it.endedAt != null }
        val steps = db.stepEntryDao().observeAll().first()
        val food = db.foodDao().observeLog().first()
        val profile = db.userProfileDao().observeProfile().first()

        val latestWeight = weights.filter { it.dateEpochDay <= today }.maxByOrNull { it.dateEpochDay }?.weightKg
        val kcalTarget = profile?.toCalorieProfile()?.let { p ->
            latestWeight?.let { current ->
                val input = goal?.let { CalorieGoalInput(it.startWeightKg, it.targetWeightKg, it.targetDateEpochDay) }
                CalorieCalculator.calculate(p, current, input, today).targetKcal
            }
        }

        val stats = PhotoStatsCalculator.snapshot(
            today = today,
            weights = weights.associate { it.dateEpochDay to it.weightKg },
            goalWeightKg = goal?.targetWeightKg,
            workoutDays = sessions.map { it.dateEpochDay },
            steps = steps.associate { it.dateEpochDay to it.steps },
            kcalByDay = food.groupBy { it.dateEpochDay }.mapValues { (_, entries) -> entries.sumOf { it.calories } },
            kcalTarget = kcalTarget
        )

        return ProgressPhoto(
            fileName = fileName,
            dateEpochDay = today,
            weightKg = stats.weightKg,
            goalWeightKg = stats.goalWeightKg,
            workoutsTotal = stats.workoutsTotal,
            workouts30Days = stats.workouts30Days,
            avgSteps7Days = stats.avgSteps7Days,
            avgKcal7Days = stats.avgKcal7Days,
            kcalTarget = stats.kcalTarget
        )
    }

    private companion object {
        const val TAG = "PhotoViewModel"
    }
}
