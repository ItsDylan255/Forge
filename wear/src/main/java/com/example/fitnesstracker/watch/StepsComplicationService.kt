package com.example.fitnesstracker.watch

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import java.time.LocalDate

/** Komplikation fürs Zifferblatt: heutige Schritte als Ring (oder als kurzer Text). */
class StepsComplicationService : SuspendingComplicationDataSourceService() {

    override fun onCreate() {
        super.onCreate()
        WatchLang.init(this)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val today = TodayStore(this).load()?.takeIf { it.dateEpochDay == LocalDate.now().toEpochDay() }
        return build(request.complicationType, today?.steps ?: 0, today?.stepGoal ?: 10_000)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? = build(type, 6_500, 10_000)

    private fun build(type: ComplicationType, steps: Int, goal: Int): ComplicationData? {
        val text = PlainComplicationText.Builder(shortSteps(steps)).build()
        val description = PlainComplicationText.Builder(tr("$steps von $goal Schritten", "$steps of $goal steps")).build()
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_stat_steps)).build()

        return when (type) {
            ComplicationType.RANGED_VALUE ->
                RangedValueComplicationData.Builder(
                    value = steps.coerceAtMost(goal).toFloat(),
                    min = 0f,
                    max = goal.coerceAtLeast(1).toFloat(),
                    contentDescription = description
                )
                    .setText(text)
                    .setMonochromaticImage(icon)
                    .build()
            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(text, description)
                    .setMonochromaticImage(icon)
                    .build()
            else -> null
        }
    }

    /** Kurz genug fürs Zifferblatt: 8432 -> "8,4k", 950 -> "950". */
    private fun shortSteps(steps: Int): String =
        if (steps >= 1000) String.format(WatchLang.locale, "%.1fk", steps / 1000.0) else steps.toString()
}
