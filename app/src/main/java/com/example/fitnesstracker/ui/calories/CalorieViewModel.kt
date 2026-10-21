package com.example.fitnesstracker.ui.calories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.domain.calories.CalorieCalculator
import com.example.fitnesstracker.domain.calories.CalorieGoalInput
import com.example.fitnesstracker.domain.calories.CalorieProfile
import com.example.fitnesstracker.domain.calories.CalorieTarget
import com.example.fitnesstracker.domain.calories.toCalorieProfile
import com.example.fitnesstracker.domain.calories.toEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Kalorienziel: kombiniert Profil, neuestes Gewicht und Gewichtsziel.
 * Neue Gewichtseinträge lösen automatisch eine Neuberechnung aus.
 */
class CalorieViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val profileDao = db.userProfileDao()

    /** Gespeichertes Profil (null = noch keines angelegt). */
    val profile: StateFlow<CalorieProfile?> = profileDao.observeProfile()
        .map { it?.toCalorieProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Berechnetes Ziel. Null, wenn Profil oder Gewichtseintrag fehlt. */
    val target: StateFlow<CalorieTarget?> = combine(
        profileDao.observeProfile(),
        db.weightEntryDao().getAllEntries(),
        db.weightGoalDao().observeGoal()
    ) { entity, entries, goal ->
        val p = entity?.toCalorieProfile()
        val current = entries.lastOrNull()?.weightKg
        if (p == null || current == null) {
            null
        } else {
            val input = goal?.let {
                CalorieGoalInput(it.startWeightKg, it.targetWeightKg, it.targetDateEpochDay)
            }
            CalorieCalculator.calculate(p, current, input, LocalDate.now().toEpochDay())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Speichert das Profil und erhält dabei das Erstellungsdatum. */
    fun saveProfile(profile: CalorieProfile) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = profileDao.observeProfile().first()
            profileDao.saveProfile(profile.toEntity(existing?.createdAt ?: now, now))
        }
    }
}
