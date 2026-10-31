package com.example.fitnesstracker.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Der laufende Pausen-Countdown. Die Restzeit ergibt sich aus [endsAtMillis].
 */
data class RestTimer(
    val exerciseName: String,
    val endsAtMillis: Long,
    val totalMillis: Long
)

/**
 * Hält die Pause für den ganzen Prozess, nicht nur für einen Bildschirm. So läuft sie beim Wechsel
 * zwischen Tabs weiter, und die Uhr kann sie verlängern oder überspringen, auch wenn die App gerade
 * nicht offen ist.
 */
object RestTimerStore {

    private val _timer = MutableStateFlow<RestTimer?>(null)

    /** null = gerade keine Pause. */
    val timer: StateFlow<RestTimer?> = _timer.asStateFlow()

    /** Startet den Countdown. Ein bereits laufender wird ersetzt. 0 Sekunden = keine Pause. */
    fun start(exerciseName: String, seconds: Int, now: Long = System.currentTimeMillis()) {
        if (seconds <= 0) {
            _timer.value = null
            return
        }
        val total = seconds * 1000L
        _timer.value = RestTimer(exerciseName, now + total, total)
    }

    /** Verlängert die laufende Pause. Ist sie schon abgelaufen, beginnt die Verlängerung jetzt. */
    fun extend(seconds: Int, now: Long = System.currentTimeMillis()) {
        _timer.update { timer ->
            timer?.let {
                val extra = seconds * 1000L
                it.copy(
                    endsAtMillis = maxOf(it.endsAtMillis, now) + extra,
                    totalMillis = it.totalMillis + extra + maxOf(0L, now - it.endsAtMillis)
                )
            }
        }
    }

    /** Beendet die Pause (vorzeitig oder nach Ablauf). */
    fun skip() {
        _timer.value = null
    }
}
