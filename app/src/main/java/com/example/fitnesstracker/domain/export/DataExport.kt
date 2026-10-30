package com.example.fitnesstracker.domain.export

import com.example.fitnesstracker.domain.gym.ExerciseKind
import com.example.fitnesstracker.domain.gym.GymCalculator
import com.example.fitnesstracker.domain.gym.SetSide
import com.example.fitnesstracker.domain.gym.WorkoutSessionData
import com.example.fitnesstracker.domain.gym.WorkoutSetData
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Ein Eintrag im Ernährungstagebuch für den Export. */
data class FoodExportRow(
    val dateEpochDay: Long,
    val name: String,
    val grams: Double,
    val servingNote: String?,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

/** Eine beendete Trainingseinheit für den Export. */
data class SessionExportRow(
    val id: Long,
    val routineName: String,
    val dateEpochDay: Long,
    val startedAt: Long,
    val endedAt: Long,
    val planName: String? = null
)

/** Alle Daten, die exportiert werden. Unabhängig von Room, damit sich der Export testen lässt. */
data class ExportData(
    val weights: List<Pair<Long, Double>>,
    val steps: List<Pair<Long, Int>>,
    val food: List<FoodExportRow>,
    val sessions: List<SessionExportRow>,
    val sets: List<WorkoutSetData>
)

/**
 * Baut die Dateien des Exports: CSV nach RFC 4180 (Komma, Punkt als Dezimaltrennzeichen, Datum als
 * JJJJ-MM-TT), damit Excel, Google Sheets, Python oder R sie direkt lesen. Die Spaltennamen sind deutsch
 * und tragen die Einheit, damit die Dateien auch ohne Programm verständlich sind.
 */
object DataExport {

    /** Dateiname → Inhalt, in der Reihenfolge, in der sie im Archiv liegen sollen. */
    fun files(data: ExportData, zone: ZoneId, exportedOn: LocalDate): Map<String, String> = linkedMapOf(
        "LIESMICH.txt" to readme(exportedOn),
        "tage.csv" to daysCsv(data),
        "gewicht.csv" to weightCsv(data.weights),
        "schritte.csv" to stepsCsv(data.steps),
        "ernaehrung.csv" to foodCsv(data.food),
        "training_einheiten.csv" to sessionsCsv(data.sessions, data.sets, zone),
        "training_saetze.csv" to setsCsv(data.sessions, data.sets)
    )

    fun fileName(exportedOn: LocalDate): String = "forge-export-$exportedOn.zip"

    /** Eine Zeile pro Tag mit Gewicht, Schritten, Ernährung und Training. Leere Zelle = kein Wert. */
    fun daysCsv(data: ExportData): String {
        val weightByDay = data.weights.toMap()
        val stepsByDay = data.steps.toMap()
        val foodByDay = data.food.groupBy { it.dateEpochDay }
        val sessionsByDay = data.sessions.groupBy { it.dateEpochDay }
        val setsBySession = data.sets.groupBy { it.sessionId }

        val days = (weightByDay.keys + stepsByDay.keys + foodByDay.keys + sessionsByDay.keys).sorted()

        val rows = days.map { day ->
            val food = foodByDay[day].orEmpty()
            val sessions = sessionsByDay[day].orEmpty()
            val sets = sessions.flatMap { setsBySession[it.id].orEmpty() }
            listOf(
                date(day),
                weekday(day),
                weightByDay[day]?.let { number(it, 2) },
                stepsByDay[day],
                food.takeIf { it.isNotEmpty() }?.let { number(it.sumOf { f -> f.calories }, 0) },
                food.takeIf { it.isNotEmpty() }?.let { number(it.sumOf { f -> f.protein }, 1) },
                food.takeIf { it.isNotEmpty() }?.let { number(it.sumOf { f -> f.carbs }, 1) },
                food.takeIf { it.isNotEmpty() }?.let { number(it.sumOf { f -> f.fat }, 1) },
                sessions.size,
                sessions.joinToString(" + ") { it.routineName }.ifEmpty { null },
                sets.size.takeIf { sessions.isNotEmpty() },
                sessions.takeIf { it.isNotEmpty() }?.let {
                    number(it.sumOf { s -> GymCalculator.summarize(s.toData(), sets).totalVolumeKg }, 0)
                }
            )
        }

        return table(
            listOf(
                "datum", "wochentag", "gewicht_kg", "schritte", "kalorien_kcal", "protein_g",
                "kohlenhydrate_g", "fett_g", "trainings", "routinen", "saetze", "volumen_kg"
            ),
            rows
        )
    }

    fun weightCsv(weights: List<Pair<Long, Double>>): String =
        table(
            listOf("datum", "gewicht_kg"),
            weights.sortedBy { it.first }.map { (day, kg) -> listOf(date(day), number(kg, 2)) }
        )

    fun stepsCsv(steps: List<Pair<Long, Int>>): String =
        table(
            listOf("datum", "schritte"),
            steps.sortedBy { it.first }.map { (day, count) -> listOf(date(day), count) }
        )

    fun foodCsv(food: List<FoodExportRow>): String =
        table(
            listOf("datum", "lebensmittel", "menge_g", "portion", "kalorien_kcal", "protein_g", "kohlenhydrate_g", "fett_g"),
            food.sortedBy { it.dateEpochDay }.map {
                listOf(
                    date(it.dateEpochDay), it.name, number(it.grams, 1), it.servingNote,
                    number(it.calories, 0), number(it.protein, 1), number(it.carbs, 1), number(it.fat, 1)
                )
            }
        )

    fun sessionsCsv(sessions: List<SessionExportRow>, sets: List<WorkoutSetData>, zone: ZoneId): String =
        table(
            listOf("einheit_id", "datum", "routine", "plan", "start", "ende", "dauer_min", "uebungen", "saetze", "volumen_kg"),
            sessions.sortedBy { it.startedAt }.map { s ->
                val summary = GymCalculator.summarize(s.toData(), sets)
                listOf(
                    s.id, date(s.dateEpochDay), s.routineName, s.planName,
                    clock(s.startedAt, zone), clock(s.endedAt, zone),
                    summary.durationMillis?.let { it / 60_000 },
                    summary.exerciseCount, summary.setCount, number(summary.totalVolumeKg, 0)
                )
            }
        )

    /** Ein Satz pro Zeile. Bei Zeitübungen stehen die Sekunden in „sekunden“, sonst die Wiederholungen. */
    fun setsCsv(sessions: List<SessionExportRow>, sets: List<WorkoutSetData>): String {
        val byId = sessions.associateBy { it.id }
        val rows = sets
            .filter { it.sessionId in byId }
            .sortedWith(compareBy({ byId.getValue(it.sessionId).startedAt }, { it.order }))
            .map { set ->
                val session = byId.getValue(set.sessionId)
                val isTime = set.kind == ExerciseKind.TIME
                listOf(
                    session.id, date(session.dateEpochDay), session.routineName, set.exerciseName,
                    kindLabel(set.kind), sideLabel(set.side),
                    number(set.weightKg, 2),
                    if (isTime) null else set.reps,
                    if (isTime) set.reps else null,
                    set.bodyWeightKg?.let { number(it, 2) }
                )
            }

        return table(
            listOf(
                "einheit_id", "datum", "routine", "uebung", "art", "seite",
                "gewicht_kg", "wiederholungen", "sekunden", "koerpergewicht_kg"
            ),
            rows
        )
    }

    fun readme(exportedOn: LocalDate): String = """
        Forge – Datenexport vom ${exportedOn.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}

        Alle Dateien sind CSV (UTF-8, Komma als Trennzeichen, Punkt als Dezimalzeichen,
        Datum als JJJJ-MM-TT). Leere Zellen bedeuten: kein Wert an diesem Tag.

        tage.csv               Ein Tag pro Zeile: Gewicht, Schritte, Ernährung, Training.
                               Der beste Einstieg für Auswertungen und Diagramme.
        gewicht.csv            Alle Gewichtsmessungen.
        schritte.csv           Schritte pro Tag.
        ernaehrung.csv         Jeder Eintrag im Ernährungstagebuch mit Nährwerten.
        training_einheiten.csv Jede beendete Trainingseinheit mit Dauer und Volumen.
        training_saetze.csv    Jeder geloggte Satz. Verbindung zu den Einheiten über einheit_id.

        Volumen = Gewicht × Wiederholungen (Zeitübungen zählen nicht, bei assistierten Übungen
        zählt das bewegte Körpergewicht).

        Excel mit deutscher Spracheinstellung: Daten > Aus Text/CSV, dann Trennzeichen „Komma“
        und Dateiursprung „UTF-8“ wählen.
        Python: pandas.read_csv("tage.csv", parse_dates=["datum"])
    """.trimIndent() + "\n"

    // ---------- CSV ----------

    fun table(header: List<String>, rows: List<List<Any?>>): String =
        buildString {
            append(row(header))
            rows.forEach { append(row(it)) }
        }

    /** Eine CSV-Zeile mit CRLF. Felder mit Komma, Anführungszeichen oder Zeilenumbruch werden gequotet. */
    fun row(values: List<Any?>): String =
        values.joinToString(",") { escape(it?.toString() ?: "") } + "\r\n"

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    /** Zahl mit höchstens [decimals] Nachkommastellen, ohne überflüssige Nullen ("80.5", nicht "80.50"). */
    fun number(value: Double, decimals: Int): String =
        BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    private fun date(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).toString()

    private fun weekday(epochDay: Long): String =
        when (LocalDate.ofEpochDay(epochDay).dayOfWeek.value) {
            1 -> "Mo"; 2 -> "Di"; 3 -> "Mi"; 4 -> "Do"; 5 -> "Fr"; 6 -> "Sa"; else -> "So"
        }

    private fun clock(millis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))

    private fun kindLabel(kind: ExerciseKind): String = when (kind) {
        ExerciseKind.REPS -> "Wiederholungen"
        ExerciseKind.TIME -> "Zeit"
        ExerciseKind.ASSISTED -> "Assistiert"
    }

    private fun sideLabel(side: SetSide): String? = when (side) {
        SetSide.BOTH -> null
        SetSide.RIGHT -> "rechts"
        SetSide.LEFT -> "links"
    }

    private fun SessionExportRow.toData() = WorkoutSessionData(id, dateEpochDay, startedAt, endedAt)
}
