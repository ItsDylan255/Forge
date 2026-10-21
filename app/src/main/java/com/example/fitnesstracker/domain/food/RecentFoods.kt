package com.example.fitnesstracker.domain.food

/** Ein Eintrag im Tagebuch, nur mit dem, was für "zuletzt verwendet" nötig ist. foodId null = Mahlzeit. */
data class LoggedFoodRef(val foodId: Long?, val dateEpochDay: Long, val entryId: Long)

object RecentFoods {

    /**
     * Die zuletzt eingetragenen Lebensmittel, das neueste zuerst, jedes nur einmal.
     * Eingetragene Mahlzeiten (ohne Lebensmittel) zählen nicht.
     */
    fun ids(entries: List<LoggedFoodRef>, limit: Int): List<Long> =
        entries
            .filter { it.foodId != null }
            .sortedWith(
                compareByDescending<LoggedFoodRef> { it.dateEpochDay }.thenByDescending { it.entryId }
            )
            .mapNotNull { it.foodId }
            .distinct()
            .take(limit)
}
