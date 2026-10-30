package com.example.fitnesstracker.ui.chart

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Seitliches Wischen über ein Zeitdiagramm. Der Inhalt folgt dem Finger: eine Fensterbreite
 * entspricht [visibleDays] Tagen. Nach rechts wischen zeigt ältere Tage.
 * [onPan] bekommt die Verschiebung des Fensterendes in Tagen (negativ = zurück in die Vergangenheit),
 * das Begrenzen auf den Datenbereich übernimmt der Aufrufer.
 */
@Composable
fun Modifier.chartPan(enabled: Boolean, visibleDays: Int, onPan: (deltaDays: Long) -> Unit): Modifier {
    if (!enabled || visibleDays <= 0) return this
    val currentOnPan by rememberUpdatedState(onPan)

    return pointerInput(visibleDays) {
        var carry = 0f
        detectHorizontalDragGestures(
            onDragEnd = { carry = 0f },
            onDragCancel = { carry = 0f }
        ) { change, dragAmount ->
            change.consume()
            val pxPerDay = size.width / visibleDays.toFloat()
            if (pxPerDay <= 0f) return@detectHorizontalDragGestures

            carry += dragAmount
            val days = (carry / pxPerDay).toLong()
            if (days != 0L) {
                carry -= days * pxPerDay
                currentOnPan(-days)
            }
        }
    }
}
