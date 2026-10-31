package com.example.fitnesstracker.domain.calories

import com.example.fitnesstracker.i18n.tr
import kotlin.math.roundToInt

/** Ergebnis einer Eingabeprüfung. */
sealed interface CalorieParse<out T> {
    data class Ok<T>(val value: T) : CalorieParse<T>
    data class Error(val message: String) : CalorieParse<Nothing>
}

/** Eingabeprüfung für das Profil (Komma/Punkt, Einheit als Suffix erlaubt). */
object CalorieInputParser {

    fun parseHeight(input: String): CalorieParse<Int> {
        val text = input.trim().lowercase().removeSuffix("cm").trim().replace(',', '.')
        val v = text.toDoubleOrNull()
            ?: return CalorieParse.Error(tr("Bitte eine Größe in cm eingeben.", "Please enter a height in cm."))
        val cm = v.roundToInt()
        return if (cm in 100..250) CalorieParse.Ok(cm)
        else CalorieParse.Error(tr("Größe muss zwischen 100 und 250 cm liegen.", "Height must be between 100 and 250 cm."))
    }

    fun parseAge(input: String): CalorieParse<Int> {
        val text = input.trim().lowercase().removeSuffix("jahre").removeSuffix("j").trim()
        val v = text.toIntOrNull()
            ?: return CalorieParse.Error(tr("Bitte ein Alter in ganzen Jahren eingeben.", "Please enter an age in whole years."))
        return if (v in 10..100) CalorieParse.Ok(v)
        else CalorieParse.Error(tr("Alter muss zwischen 10 und 100 Jahren liegen.", "Age must be between 10 and 100 years."))
    }

    private fun percent(input: String, name: String): CalorieParse<Int> {
        val v = input.trim().removeSuffix("%").trim().toIntOrNull()
        return if (v != null && v in 0..100) CalorieParse.Ok(v)
        else CalorieParse.Error(tr("$name: ganze Zahl von 0 bis 100 eingeben.", "$name: enter a whole number from 0 to 100."))
    }

    fun parseSplit(protein: String, carbs: String, fat: String): CalorieParse<MacroSplit> {
        val p = percent(protein, "Protein")
        if (p is CalorieParse.Error) return p
        val c = percent(carbs, tr("Kohlenhydrate", "Carbs"))
        if (c is CalorieParse.Error) return c
        val f = percent(fat, tr("Fett", "Fat"))
        if (f is CalorieParse.Error) return f
        val sum = (p as CalorieParse.Ok).value + (c as CalorieParse.Ok).value +
            (f as CalorieParse.Ok).value
        return if (sum == 100) CalorieParse.Ok(MacroSplit(p.value, c.value, f.value))
        else CalorieParse.Error(tr("Die Summe muss 100 % ergeben (aktuell $sum %).", "The total must be 100 % (currently $sum %)."))
    }
}
