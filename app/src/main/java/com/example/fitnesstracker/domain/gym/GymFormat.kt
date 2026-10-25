package com.example.fitnesstracker.domain.gym

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object GymFormat {

    private val locale: Locale = Locale.GERMANY
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", locale)
    private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)

    /** Zahl ohne unnötige Nachkommastellen: 80 -> "80", 82.5 -> "82,5". Auch zum Vorausfüllen. */
    fun number(value: Double): String {
        if (!value.isFinite()) return ""
        return BigDecimal(value.toString())
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
            .replace('.', ',')
    }

    fun kg(weightKg: Double): String = "${number(weightKg)} kg"

    /** Gesamtvolumen, gerundet, mit Tausenderpunkt: "12.450 kg". */
    fun volume(volumeKg: Double): String =
        NumberFormat.getIntegerInstance(locale).format(Math.round(volumeKg)) + " kg"

    /** Kurzform für Diagramm-Achsen: 850 -> "850", 12450 -> "12,5k". */
    fun compact(value: Double): String =
        if (value >= 1000.0) String.format(locale, "%.1fk", value / 1000.0)
        else Math.round(value).toString()

    /** "80 kg × 10" – bei 0 kg: "Körpergewicht × 15". */
    fun setText(weightKg: Double, reps: Int): String =
        if (weightKg == 0.0) "Körpergewicht × $reps" else "${kg(weightKg)} × $reps"

    fun repsRange(min: Int, max: Int): String = if (min == max) "$min" else "$min–$max"

    /** Pause einer Übung: 90 -> "1:30", 0 -> "Aus". */
    fun restLabel(seconds: Int): String =
        if (seconds <= 0) "Aus" else String.format(locale, "%d:%02d", seconds / 60, seconds % 60)

    /** "4 × 8–12" */
    fun plan(sets: Int, repsMin: Int, repsMax: Int): String = "$sets × ${repsRange(repsMin, repsMax)}"

    /** Laufende Uhr: "05:32" oder "1:05:32". */
    fun timer(millis: Long): String {
        val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format(locale, "%d:%02d:%02d", hours, minutes, seconds)
        else String.format(locale, "%02d:%02d", minutes, seconds)
    }

    /** Dauer einer beendeten Einheit: "45 Min", "1 Std 05 Min" oder "< 1 Min". */
    fun duration(millis: Long): String {
        val totalMinutes = (millis / 60_000L).coerceAtLeast(0L)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> String.format(locale, "%d Std %02d Min", hours, minutes)
            totalMinutes < 1 -> "< 1 Min"
            else -> "$totalMinutes Min"
        }
    }

    // ---------- Übungsarten ----------

    fun kindLabel(kind: ExerciseKind): String = when (kind) {
        ExerciseKind.REPS -> "Wiederholungen"
        ExerciseKind.TIME -> "Zeit"
        ExerciseKind.ASSISTED -> "Assistiert"
    }

    /** Sekunden: 45 -> "45 Sek.", 90 -> "1:30 Min", 60 -> "1:00 Min". */
    fun secondsLabel(seconds: Int): String =
        if (seconds < 60) "$seconds Sek."
        else String.format(locale, "%d:%02d Min", seconds / 60, seconds % 60)

    /** Bereich je Art: "8–12", bei Zeitübungen "30–60 Sek.". */
    fun repsRange(kind: ExerciseKind, min: Int, max: Int): String =
        if (kind == ExerciseKind.TIME) {
            if (min == max) secondsLabel(min) else "$min–$max Sek."
        } else {
            repsRange(min, max)
        }

    /** "4 × 8–12" bzw. "3 × 30–60 Sek." */
    fun plan(kind: ExerciseKind, sets: Int, repsMin: Int, repsMax: Int): String =
        "$sets × ${repsRange(kind, repsMin, repsMax)}"

    /**
     * Ein Satz je Art: "80 kg × 10", "45 Sek." (mit Zusatzgewicht "45 Sek. · +10 kg"),
     * "20 kg Unterstützung × 8" bzw. "Ohne Unterstützung × 8".
     */
    fun setText(kind: ExerciseKind, weightKg: Double, reps: Int): String = when (kind) {
        ExerciseKind.REPS -> setText(weightKg, reps)
        ExerciseKind.TIME ->
            if (weightKg == 0.0) secondsLabel(reps) else "${secondsLabel(reps)} · +${kg(weightKg)}"
        ExerciseKind.ASSISTED ->
            if (weightKg == 0.0) "Ohne Unterstützung × $reps" else "${kg(weightKg)} Unterstützung × $reps"
    }

    /** Ein Messwert in der Einheit der Art: "80 kg", "45 Sek." oder "20 kg Unterstützung". */
    fun metric(kind: ExerciseKind, value: Double): String = when (kind) {
        ExerciseKind.REPS -> kg(value)
        ExerciseKind.TIME -> secondsLabel(Math.round(value).toInt())
        ExerciseKind.ASSISTED -> "${kg(value)} Unterstützung"
    }

    /** Dezenter Hinweis zur Progression. Null bei [ProgressionHint.NONE]. */
    fun progressionText(kind: ExerciseKind, hint: ProgressionHint, repsMin: Int, repsMax: Int): String? =
        when (hint) {
            ProgressionHint.NONE -> null
            ProgressionHint.REDUCE -> when (kind) {
                ExerciseKind.REPS ->
                    "\u2193 Letztes Mal unter $repsMin Wdh. in mindestens einem Satz: diesmal Gewicht reduzieren."
                ExerciseKind.ASSISTED ->
                    "\u2193 Letztes Mal unter $repsMin Wdh. in mindestens einem Satz: diesmal Unterstützung erhöhen."
                ExerciseKind.TIME ->
                    "\u2193 Letztes Mal unter $repsMin Sek. in mindestens einem Satz: diesmal leichter machen."
            }
            ProgressionHint.INCREASE -> when (kind) {
                ExerciseKind.REPS ->
                    "\u2191 Letztes Mal alle Sätze bei $repsMax Wdh. oder mehr: Gewicht erhöhen möglich."
                ExerciseKind.ASSISTED ->
                    "\u2191 Letztes Mal alle Sätze bei $repsMax Wdh. oder mehr: Unterstützung verringern möglich."
                ExerciseKind.TIME ->
                    "\u2191 Letztes Mal alle Sätze bei $repsMax Sek. oder mehr: Steigern möglich (Zusatzgewicht oder schwerere Variante)."
            }
        }

    // ---------- Gesamt-Verlauf einer Übung ----------

    /** Überschrift des Verlaufs je Messgröße. */
    fun totalLabel(metric: TotalMetric): String = when (metric) {
        TotalMetric.VOLUME -> "GESAMTVOLUMEN PRO TRAINING"
        TotalMetric.REPS -> "WIEDERHOLUNGEN GESAMT PRO TRAINING"
        TotalMetric.TIME -> "GESAMTZEIT PRO TRAINING"
    }

    /** Summe eines Trainings: "1.240 kg", "24 Wdh." oder "2:30 Min". */
    fun total(metric: TotalMetric, value: Double): String = when (metric) {
        TotalMetric.VOLUME -> volume(value)
        TotalMetric.REPS -> "${Math.round(value)} Wdh."
        TotalMetric.TIME -> secondsLabel(Math.round(value).toInt())
    }

    /** Veränderung zum Training davor: "+120 kg · +10 %", "−80 kg · −8 %". Ohne Vergleichswert kein Prozent. */
    fun totalDelta(metric: TotalMetric, delta: Double, previous: Double): String {
        val sign = when {
            delta > 0 -> "+"
            delta < 0 -> "\u2212"
            else -> "\u00B1"
        }
        val percent = if (previous > 0.0) {
            " \u00B7 $sign${Math.round(abs(delta) / previous * 100.0)} %"
        } else {
            ""
        }
        return "$sign${total(metric, abs(delta))}$percent"
    }

    fun date(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dateFormatter)

    /** Uhrzeit ("14:35") eines Zeitstempels in der lokalen Zeitzone. */
    fun clock(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(clockFormatter)
}
