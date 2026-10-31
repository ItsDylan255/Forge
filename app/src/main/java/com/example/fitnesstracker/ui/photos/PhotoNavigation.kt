package com.example.fitnesstracker.ui.photos

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Wunsch von außen (Startseite, Benachrichtigung), im Gym-Tab direkt die Fotos zu öffnen. */
object PhotoNavigation {

    private val _requested = MutableStateFlow(false)
    val requested: StateFlow<Boolean> = _requested.asStateFlow()

    fun request() {
        _requested.value = true
    }

    fun consume() {
        _requested.value = false
    }
}
