package com.example.fitnesstracker.domain.gym

data class SessionSummary(
    val setCount: Int,
    val exerciseCount: Int,
    /** Summe aus Gewicht \u00D7 Wiederholungen */
    val totalVolumeKg: Double,
    /** null, solange die Einheit noch l\u00E4uft */
    val durationMillis: Long?
)

data class SessionVolume(val sessionId: Long, val dateEpochDay: Long, val volumeKg: Double)

/** Volumen einer Kalenderwoche (Start = Montag). */
data class PeriodVolume(val startEpochDay: Long, val volumeKg: Double)

/**
 * Bestleistung einer Übung. Je nach [kind]:
 * REPS = schwerster Satz, TIME = längste Zeit ([repsAtBest] sind Sekunden, [bestWeightKg] das Zusatzgewicht),
 * ASSISTED = geringste Unterstützung.
 */
data class ExerciseBest(
    val exerciseId: Long,
    val exerciseName: String,
    val bestWeightKg: Double,
    val repsAtBest: Int,
    val dateEpochDay: Long,
    val kind: ExerciseKind = ExerciseKind.REPS
)

/** Bestwert eines Tages. Einheit je Art: kg, Sekunden (TIME) oder kg Unterstützung (ASSISTED). */
data class ProgressPoint(val dateEpochDay: Long, val maxWeightKg: Double)

/** Neue Bestleistung. [weightKg] und [previousKg] haben die Einheit der Art (kg, Sekunden oder kg Unterstützung). */
data class NewBest(
    val exerciseId: Long,
    val exerciseName: String,
    val weightKg: Double,
    val previousKg: Double,
    val kind: ExerciseKind = ExerciseKind.REPS
)

/** Ergebnis des letzten beendeten Trainings mit einer Übung (Sätze in Reihenfolge des Loggens). */
data class LastPerformance(
    val sessionId: Long,
    val dateEpochDay: Long,
    val sets: List<WorkoutSetData>
)

/** Empfehlung zur Gewichtsprogression (ändert nie automatisch etwas). */
enum class ProgressionHint { NONE, REDUCE, INCREASE }

object GymCalculator {

    fun volume(weightKg: Double, reps: Int): Double = weightKg * reps

    /**
     * Volumen eines Satzes je Art: REPS = Gewicht × Wdh., TIME = zählt nicht,
     * ASSISTED = (Körpergewicht − Unterstützung) × Wdh. (ohne Körpergewicht 0, nie negativ).
     */
    fun setVolume(set: WorkoutSetData): Double = when (set.kind) {
        ExerciseKind.REPS -> volume(set.weightKg, set.reps)
        ExerciseKind.TIME -> 0.0
        ExerciseKind.ASSISTED -> {
            val bodyWeight = set.bodyWeightKg
            if (bodyWeight == null) 0.0 else (bodyWeight - set.weightKg).coerceAtLeast(0.0) * set.reps
        }
    }

    /** Vergleich "besser" je Art: schwerer, länger oder weniger Unterstützung. Das Größte ist das Beste. */
    private fun betterOrder(kind: ExerciseKind): Comparator<WorkoutSetData> = when (kind) {
        ExerciseKind.REPS -> compareBy<WorkoutSetData>({ it.weightKg }, { it.reps })
        ExerciseKind.TIME -> compareBy<WorkoutSetData>({ it.reps }, { it.weightKg })
        ExerciseKind.ASSISTED -> compareBy<WorkoutSetData>({ -it.weightKg }, { it.reps })
    }

    /** Messwert eines Satzes in der Einheit der Art: kg, Sekunden oder kg Unterstützung. */
    private fun valueOf(set: WorkoutSetData): Double = when (set.kind) {
        ExerciseKind.TIME -> set.reps.toDouble()
        else -> set.weightKg
    }

    /** true, wenn [current] besser als [previous] ist (bei ASSISTED: weniger Unterstützung). */
    private fun isBetter(kind: ExerciseKind, current: Double, previous: Double): Boolean =
        if (kind == ExerciseKind.ASSISTED) current < previous else current > previous

    fun summarize(session: WorkoutSessionData, sets: List<WorkoutSetData>): SessionSummary {
        val own = sets.filter { it.sessionId == session.id }
        return SessionSummary(
            setCount = own.size,
            exerciseCount = own.map { it.exerciseId }.distinct().size,
            totalVolumeKg = own.sumOf { setVolume(it) },
            durationMillis = session.endedAt?.let { (it - session.startedAt).coerceAtLeast(0L) }
        )
    }

    /** Die letzten [limit] Einheiten, chronologisch aufsteigend, mit ihrem Volumen. */
    fun sessionVolumes(
        sessions: List<WorkoutSessionData>,
        sets: List<WorkoutSetData>,
        limit: Int
    ): List<SessionVolume> {
        val bySession = sets.groupBy { it.sessionId }
        return sessions
            .sortedBy { it.startedAt }
            .map { session ->
                SessionVolume(
                    sessionId = session.id,
                    dateEpochDay = session.dateEpochDay,
                    volumeKg = bySession[session.id].orEmpty().sumOf { setVolume(it) }
                )
            }
            .takeLast(limit)
    }

    /** Montag der Woche, in der [epochDay] liegt. (1.1.1970 war ein Donnerstag.) */
    fun weekStart(epochDay: Long): Long = epochDay - Math.floorMod(epochDay + 3L, 7L)

    /** Genau [weeks] Wochen bis einschlie\u00DFlich der aktuellen, \u00E4lteste zuerst. Leere Wochen = 0. */
    fun weeklyVolumes(
        sessions: List<WorkoutSessionData>,
        sets: List<WorkoutSetData>,
        weeks: Int,
        today: Long
    ): List<PeriodVolume> {
        val currentStart = weekStart(today)
        val volumeBySession = sets.groupBy { it.sessionId }
            .mapValues { (_, list) -> list.sumOf { setVolume(it) } }

        val volumeByWeek = sessions
            .groupBy { weekStart(it.dateEpochDay) }
            .mapValues { (_, list) -> list.sumOf { volumeBySession[it.id] ?: 0.0 } }

        return (0 until weeks).map { index ->
            val start = currentStart - 7L * (weeks - 1 - index)
            PeriodVolume(start, volumeByWeek[start] ?: 0.0)
        }
    }

    /**
     * Bestleistung pro Übung je Art: schwerster Satz (bei Gleichstand mehr Wiederholungen),
     * längste Zeit oder geringste Unterstützung. Gezählt werden nur Sätze der zuletzt benutzten Art.
     */
    fun personalBests(
        sessions: List<WorkoutSessionData>,
        sets: List<WorkoutSetData>
    ): List<ExerciseBest> {
        val dateBySession = sessions.associate { it.id to it.dateEpochDay }

        return sets
            .filter { it.sessionId in dateBySession }
            .sortedBy { it.order }
            .groupBy { it.exerciseId }
            .map { (exerciseId, list) ->
                val kind = list.last().kind
                val best = list.filter { it.kind == kind }.maxWith(betterOrder(kind))
                ExerciseBest(
                    exerciseId = exerciseId,
                    exerciseName = list.last().exerciseName,
                    bestWeightKg = best.weightKg,
                    repsAtBest = best.reps,
                    dateEpochDay = dateBySession.getValue(best.sessionId),
                    kind = kind
                )
            }
            .sortedBy { it.exerciseName.lowercase() }
    }

    /** Bestwert pro Trainingstag für eine Übung (je Art), chronologisch aufsteigend. */
    fun exerciseProgress(
        exerciseId: Long,
        sessions: List<WorkoutSessionData>,
        sets: List<WorkoutSetData>
    ): List<ProgressPoint> {
        val dateBySession = sessions.associate { it.id to it.dateEpochDay }
        val own = sets.filter { it.exerciseId == exerciseId && it.sessionId in dateBySession }
        val kind = own.maxByOrNull { it.order }?.kind ?: return emptyList()

        return own
            .filter { it.kind == kind }
            .groupBy { dateBySession.getValue(it.sessionId) }
            .map { (date, list) -> ProgressPoint(date, valueOf(list.maxWith(betterOrder(kind)))) }
            .sortedBy { it.dateEpochDay }
    }

    /**
     * Übungen, bei denen der Bestwert in dieser Einheit besser war als in allen früheren
     * (schwerer, länger oder weniger Unterstützung). Gibt es noch keine frühere Einheit mit der
     * Übung, zählt es nicht als neue Bestleistung.
     */
    fun newBestsIn(
        sessionId: Long,
        sessions: List<WorkoutSessionData>,
        sets: List<WorkoutSetData>
    ): List<NewBest> {
        val current = sessions.firstOrNull { it.id == sessionId } ?: return emptyList()
        val earlierIds = sessions
            .filter { it.id != sessionId && it.startedAt < current.startedAt }
            .map { it.id }
            .toSet()

        return sets
            .filter { it.sessionId == sessionId }
            .sortedBy { it.order }
            .groupBy { it.exerciseId }
            .mapNotNull { (exerciseId, list) ->
                val kind = list.last().kind
                val currentValue = valueOf(list.filter { it.kind == kind }.maxWith(betterOrder(kind)))
                val previousValue = sets
                    .filter { it.exerciseId == exerciseId && it.sessionId in earlierIds && it.kind == kind }
                    .maxWithOrNull(betterOrder(kind))
                    ?.let { valueOf(it) }

                if (previousValue != null && isBetter(kind, currentValue, previousValue)) {
                    NewBest(exerciseId, list.last().exerciseName, currentValue, previousValue, kind)
                } else {
                    null
                }
            }
    }

    /**
     * Letztes beendetes Training (ohne [currentSessionId]) mit dieser Übung, sonst null.
     * Laufende Einheiten zählen nicht.
     */
    fun lastPerformance(
        exerciseId: Long,
        currentSessionId: Long,
        sessions: List<WorkoutSessionData>,
        sets: List<WorkoutSetData>
    ): LastPerformance? {
        val finished = sessions
            .filter { it.endedAt != null && it.id != currentSessionId }
            .sortedByDescending { it.startedAt }

        for (session in finished) {
            val own = sets
                .filter { it.sessionId == session.id && it.exerciseId == exerciseId }
                .sortedBy { it.order }
            if (own.isNotEmpty()) return LastPerformance(session.id, session.dateEpochDay, own)
        }
        return null
    }

    /**
     * Hinweis aus dem letzten Training:
     * REDUCE = mindestens ein Satz unter [repsMin]; INCREASE = mindestens [plannedSets] Sätze
     * und alle bei [repsMax] oder mehr; sonst NONE.
     */
    fun progressionHint(
        plannedSets: Int,
        repsMin: Int,
        repsMax: Int,
        lastSets: List<WorkoutSetData>
    ): ProgressionHint {
        if (lastSets.isEmpty()) return ProgressionHint.NONE
        if (lastSets.any { it.reps < repsMin }) return ProgressionHint.REDUCE
        if (lastSets.size >= plannedSets && lastSets.all { it.reps >= repsMax }) {
            return ProgressionHint.INCREASE
        }
        return ProgressionHint.NONE
    }
}
