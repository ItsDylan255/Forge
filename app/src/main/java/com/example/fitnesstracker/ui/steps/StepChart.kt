package com.example.fitnesstracker.ui.steps

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.domain.steps.StepBar
import com.example.fitnesstracker.domain.steps.StepFormat
import com.example.fitnesstracker.ui.theme.ForgeColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Balkendiagramm: ein Balken pro Tag.
 * - Balken, die das Tagesziel erreichen, sind voll eingefärbt, die anderen gedimmt.
 * - Tagesziel als gestrichelte Linie.
 * - Bei bis zu 7 Balken stehen Wochentag und Wert direkt am Balken.
 */
@Composable
fun StepChart(
    bars: List<StepBar>,
    goal: Int?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val barColor = ForgeColors.ChartLine
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline

    val axisStyle = TextStyle(color = secondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    val valueStyle = TextStyle(color = onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMANY)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        if (bars.isEmpty()) return@Canvas

        val chartLeft = 48.dp.toPx()
        val chartRight = size.width - 8.dp.toPx()
        val chartTop = 20.dp.toPx()
        val chartBottom = size.height - 28.dp.toPx()
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop
        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        // ---------- Wertebereich ----------
        val dataMax = max(bars.maxOf { it.steps }, goal ?: 0).coerceAtLeast(1_000)
        val step = niceStep(dataMax / 3f)
        val maxValue = ceil(dataMax * 1.1f / step) * step

        fun yFor(value: Float): Float = chartBottom - (value / maxValue) * chartHeight

        // ---------- Gitterlinien + Y-Beschriftung ----------
        val dotted = PathEffect.dashPathEffect(floatArrayOf(2f, 8f), 0f)
        val lineCount = (maxValue / step).roundToInt()

        for (i in 0..lineCount) {
            val value = step * i
            val y = yFor(value)

            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
                pathEffect = dotted
            )

            val measured = textMeasurer.measure(StepFormat.steps(value.roundToInt()), axisStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = chartLeft - measured.size.width - 8.dp.toPx(),
                    y = y - measured.size.height / 2f
                )
            )
        }

        // ---------- Balken ----------
        val count = bars.size
        val slot = chartWidth / count
        val barWidth = (slot * 0.62f).coerceAtMost(28.dp.toPx())

        bars.forEachIndexed { index, bar ->
            if (bar.steps <= 0) return@forEachIndexed

            val x = chartLeft + slot * index + (slot - barWidth) / 2f
            val barHeight = (bar.steps / maxValue) * chartHeight
            val reached = goal != null && bar.steps >= goal

            drawRoundRect(
                color = if (goal == null || reached) barColor else barColor.copy(alpha = 0.55f),
                topLeft = Offset(x, chartBottom - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }

        // ---------- Ziel-Linie (gestrichelt) ----------
        if (goal != null) {
            val y = yFor(goal.toFloat())
            drawLine(
                color = onSurface.copy(alpha = 0.7f),
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(10.dp.toPx(), 6.dp.toPx()), 0f
                )
            )
        }

        // ---------- Wert-Labels (nur bei wenigen Balken) ----------
        if (count <= 7) {
            bars.forEachIndexed { index, bar ->
                if (bar.steps <= 0) return@forEachIndexed

                val measured = textMeasurer.measure(StepFormat.steps(bar.steps), valueStyle)
                val centerX = chartLeft + slot * index + slot / 2f
                val barTop = yFor(bar.steps.toFloat())

                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = (centerX - measured.size.width / 2f)
                            .coerceIn(chartLeft - 8.dp.toPx(), size.width - measured.size.width),
                        y = (barTop - measured.size.height - 3.dp.toPx()).coerceAtLeast(0f)
                    )
                )
            }
        }

        // ---------- X-Beschriftung ----------
        bars.forEachIndexed { index, bar ->
            val showLabel = count <= 7 || (count - 1 - index) % 5 == 0
            if (!showLabel) return@forEachIndexed

            val date = LocalDate.ofEpochDay(bar.dateEpochDay)
            val label = if (count <= 7) {
                date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.GERMANY)
            } else {
                date.format(dateFormatter)
            }

            val measured = textMeasurer.measure(label, axisStyle)
            val centerX = chartLeft + slot * index + slot / 2f

            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = (centerX - measured.size.width / 2f)
                        .coerceIn(0f, size.width - measured.size.width),
                    y = chartBottom + 6.dp.toPx()
                )
            )
        }
    }
}

/** Liefert einen "runden" Schritt (1, 2, 5, 10 … mal Zehnerpotenz) für die Y-Achse. */
private fun niceStep(rough: Float): Float {
    val r = rough.coerceAtLeast(1f).toDouble()
    val magnitude = 10.0.pow(floor(log10(r)))
    val normalized = r / magnitude
    val nice = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 5.0 -> 5.0
        else -> 10.0
    }
    return (nice * magnitude).toFloat()
}
