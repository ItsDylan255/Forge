package com.example.fitnesstracker.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnesstracker.data.local.DatabaseProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Daten für die Startseite: Gewicht, Schritte und das letzte Fortschrittsfoto. Nur lesend. */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)

    val weightEntries = db.weightEntryDao().getAllEntries().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val weightGoal = db.weightGoalDao().observeGoal().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null
    )

    val stepEntries = db.stepEntryDao().observeAll().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    /** Tag des letzten Fortschrittsfotos, null wenn es noch keins gibt. */
    val lastPhotoDay = db.progressPhotoDao().observeAll()
        .map { photos -> photos.maxOfOrNull { it.dateEpochDay } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
