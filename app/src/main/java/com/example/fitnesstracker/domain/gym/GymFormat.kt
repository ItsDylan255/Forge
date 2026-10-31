package com.example.fitnesstracker.domain.gym

import com.example.fitnesstracker.i18n.L10n
import com.example.fitnesstracker.i18n.tr
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

    private val locale: Locale get() = L10n.locale
    private val dateFormatter get() = DateTimeFormatter.ofPattern(tr("dd.MM.yyyy", "dd/MM/yyyy"), locale)
    private val clockFormatter get() = DateTimeFormatter.ofPattern("HH:mm", locale)

    /** Zahl ohne unnötige Nachkommastellen: 80 -> "80", 82.5 -> "82,5". Auch zum Vorausfüllen. */
    fun number(value: Double): String {
        if (!value.isFinite()) return ""
        return BigDecimal(value.toString())
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
            .let { if (L10n.isEnglish) it else it.replace('.', ',') }
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
        if (weightKg == 0.0) tr("Körpergewicht × $reps", "Bodyweight × $reps") else "${kg(weightKg)} × $reps"

    fun repsRange(min: Int, max: Int): String = if (min == max) "$min" else "$min–$max"

    /** Pause einer Übung: 90 -> "1:30", 0 -> "Aus". */
    fun restLabel(seconds: Int): String =
        if (seconds <= 0) tr("Aus", "Off") else String.format(locale, "%d:%02d", seconds / 60, seconds % 60)

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
            hours > 0 -> String.format(locale, tr("%d Std %02d Min", "%d h %02d min"), hours, minutes)
            totalMinutes < 1 -> tr("< 1 Min", "< 1 min")
            else -> tr("$totalMinutes Min", "$totalMinutes min")
        }
    }

    // ---------- Übungsarten ----------

    fun kindLabel(kind: ExerciseKind): String = when (kind) {
        ExerciseKind.REPS -> tr("Wiederholungen", "Reps")
        ExerciseKind.TIME -> tr("Zeit", "Time")
        ExerciseKind.ASSISTED -> tr("Assistiert", "Assisted")
    }

    /** Sekunden: 45 -> "45 Sek.", 90 -> "1:30 Min", 60 -> "1:00 Min". */
    fun secondsLabel(seconds: Int): String =
        if (seconds < 60) tr("$seconds Sek.", "$seconds s")
        else String.format(locale, tr("%d:%02d Min", "%d:%02d min"), seconds / 60, seconds % 60)

    /** Bereich je Art: "8–12", bei Zeitübungen "30–60 Sek.". */
    fun repsRange(kind: ExerciseKind, min: Int, max: Int): String =
        if (kind == ExerciseKind.TIME) {
            if (min == max) secondsLabel(min) else tr("$min–$max Sek.", "$min–$max s")
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
            if (weightKg == 0.0) tr("Ohne Unterstützung × $reps", "No assistance × $reps") else tr("${kg(weightKg)} Unterstützung × $reps", "${kg(weightKg)} assistance × $reps")
    }

    /** Ein Messwert in der Einheit der Art: "80 kg", "45 Sek." oder "20 kg Unterstützung". */
    fun metric(kind: ExerciseKind, value: Double): String = when (kind) {
        ExerciseKind.REPS -> kg(value)
        ExerciseKind.TIME -> secondsLabel(Math.round(value).toInt())
        ExerciseKind.ASSISTED -> tr("${kg(value)} Unterstützung", "${kg(value)} assistance")
    }

    /** Dezenter Hinweis zur Progression. Null bei [ProgressionHint.NONE]. */
    fun progressionText(kind: ExerciseKind, hint: ProgressionHint, repsMin: Int, repsMax: Int): String? =
        when (hint) {
            ProgressionHint.NONE -> null
            ProgressionHint.REDUCE -> when (kind) {
                ExerciseKind.REPS ->
                    tr("\u2193 Letztes Mal unter $repsMin Wdh. in mindestens einem Satz: diesmal Gewicht reduzieren.", "↓ Last time below $repsMin reps in at least one set: lower the weight this time.")
                ExerciseKind.ASSISTED ->
                    tr("\u2193 Letztes Mal unter $repsMin Wdh. in mindestens einem Satz: diesmal Unterstützung erhöhen.", "↓ Last time below $repsMin reps in at least one set: add more assistance this time.")
                ExerciseKind.TIME ->
                    tr("\u2193 Letztes Mal unter $repsMin Sek. in mindestens einem Satz: diesmal leichter machen.", "↓ Last time below $repsMin s in at least one set: make it easier this time.")
            }
            ProgressionHint.INCREASE -> when (kind) {
                ExerciseKind.REPS ->
                    tr("\u2191 Letztes Mal alle Sätze bei $repsMax Wdh. oder mehr: Gewicht erhöhen möglich.", "↑ Last time all sets at $repsMax reps or more: you can increase the weight.")
                ExerciseKind.ASSISTED ->
                    tr("\u2191 Letztes Mal alle Sätze bei $repsMax Wdh. oder mehr: Unterstützung verringern möglich.", "↑ Last time all sets at $repsMax reps or more: you can reduce the assistance.")
                ExerciseKind.TIME ->
                    tr("\u2191 Letztes Mal alle Sätze bei $repsMax Sek. oder mehr: Steigern möglich (Zusatzgewicht oder schwerere Variante).", "↑ Last time all sets at $repsMax s or more: you can progress (added weight or a harder variation).")
            }
        }

    // ---------- Gesamt-Verlauf einer Übung ----------

    /** Überschrift des Verlaufs je Messgröße. */
    fun totalLabel(metric: TotalMetric): String = when (metric) {
        TotalMetric.VOLUME -> tr("GESAMTVOLUMEN PRO TRAINING", "TOTAL VOLUME PER WORKOUT")
        TotalMetric.REPS -> tr("WIEDERHOLUNGEN GESAMT PRO TRAINING", "TOTAL REPS PER WORKOUT")
        TotalMetric.TIME -> tr("GESAMTZEIT PRO TRAINING", "TOTAL TIME PER WORKOUT")
    }

    /** Summe eines Trainings: "1.240 kg", "24 Wdh." oder "2:30 Min". */
    fun total(metric: TotalMetric, value: Double): String = when (metric) {
        TotalMetric.VOLUME -> volume(value)
        TotalMetric.REPS -> tr("${Math.round(value)} Wdh.", "${Math.round(value)} reps")
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
