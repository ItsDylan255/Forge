package com.example.fitnesstracker.ui.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.domain.weight.WeightMeasurement
import com.example.fitnesstracker.ui.theme.ForgeColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Liniendiagramm für Gewichtsmessungen (Stil: Simple Weight Tracker).
 * Alle Werte sind echte Messungen – nichts wird interpoliert oder erfunden.
 *
 * - Y-Achse mit "runden" Schritten und gepunkteten Hilfslinien
 * - X-Achse zeitlich korrekt (Abstand der Punkte = Abstand der Tage)
 * - Hohle Punkte mit Wert-Label
 * - Zielgewicht als gestrichelte Linie
 *
 * @param measurements chronologisch aufsteigende echte Messungen
 * @param targetKg     optional: Zielgewicht für die gestrichelte Linie
 */
@Composable
fun WeightChart(
    measurements: List<WeightMeasurement>,
    targetKg: Double?,
    modifier: Modifier = Modifier
) {
    if (measurements.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Noch keine Daten für ein Diagramm.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()

    val lineColor = ForgeColors.ChartLine
    val onBackground = MaterialTheme.colorScheme.onBackground
    // Das Diagramm liegt in einer Karte (surface): so wirken die Punkte wirklich hohl
    val backgroundColor = MaterialTheme.colorScheme.surface
    val gridColor = onBackground.copy(alpha = 0.25f)

    val axisLabelStyle = TextStyle(
        color = onBackground.copy(alpha = 0.8f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
    val valueLabelStyle = TextStyle(
        color = onBackground,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMANY)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        val leftPadding = 40.dp.toPx()
        val bottomPadding = 28.dp.toPx()
        val topPadding = 16.dp.toPx()
        val rightPadding = 16.dp.toPx()

        val chartLeft = leftPadding
        val chartRight = size.width - rightPadding
        val chartTop = topPadding
        val chartBottom = size.height - bottomPadding
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        // ---------- Wertebereich mit "runden" Schritten ----------
        val weights = measurements.map { it.weightKg.toFloat() }
        val weightMin = weights.minOrNull() ?: 0f
        val weightMax = weights.maxOrNull() ?: 0f
        val targetFloat = targetKg?.toFloat()

        val dataMin = minOf(weightMin, targetFloat ?: weightMin)
        val dataMax = maxOf(weightMax, targetFloat ?: weightMax)

        val span = (dataMax - dataMin).coerceAtLeast(1f)
        val step = niceStep(span / 3f)

        val minValue = floor((dataMin - span * 0.1f) / step) * step
        val maxValue = ceil((dataMax + span * 0.15f) / step) * step
        val valueRange = (maxValue - minValue).coerceAtLeast(0.1f)

        fun yFor(value: Float): Float =
            chartBottom - ((value - minValue) / valueRange) * chartHeight

        // ---------- X-Achse nach echtem Datum ----------
        val firstDay = measurements.first().dateEpochDay
        val lastDay = measurements.last().dateEpochDay
        val daySpan = (lastDay - firstDay).toFloat()

        fun xForDay(day: Long): Float =
            if (daySpan <= 0f) {
                (chartLeft + chartRight) / 2f
            } else {
                chartLeft + chartWidth * ((day - firstDay) / daySpan)
            }

        fun xFor(index: Int): Float = xForDay(measurements[index].dateEpochDay)

        // ---------- Y-Gitterlinien (gepunktet) + Beschriftung ----------
        val decimals = if (step < 1f) 1 else 0
        val lineCount = ((maxValue - minValue) / step).roundToInt()
        val dotted = PathEffect.dashPathEffect(floatArrayOf(2f, 8f), 0f)

        for (i in 0..lineCount) {
            val value = minValue + step * i
            val y = yFor(value)

            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
                pathEffect = dotted
            )

            val label = String.format(Locale.GERMANY, "%.${decimals}f", value)
            val measured = textMeasurer.measure(label, axisLabelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = chartLeft - measured.size.width - 8.dp.toPx(),
                    y = y - measured.size.height / 2f
                )
            )
        }

        // ---------- Zielgewicht (gestrichelt) ----------
        if (targetFloat != null) {
            val y = yFor(targetFloat)
            drawLine(
                color = lineColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 2.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(14.dp.toPx(), 8.dp.toPx()), 0f
                )
            )
        }

        // ---------- Linie durch die Messpunkte ----------
        if (measurements.size >= 2) {
            val path = Path().apply {
                moveTo(xFor(0), yFor(weights[0]))
                for (i in 1 until measurements.size) {
                    lineTo(xFor(i), yFor(weights[i]))
                }
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // ---------- Hohle Punkte ----------
        val pointRadius = 5.dp.toPx()
        measurements.forEachIndexed { index, _ ->
            val center = Offset(xFor(index), yFor(weights[index]))
            drawCircle(color = backgroundColor, radius = pointRadius, center = center)
            drawCircle(
                color = lineColor,
                radius = pointRadius,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        // ---------- Wert-Labels über den Punkten ----------
        val count = measurements.size
        val labelStep = when {
            count <= 10 -> 1
            count <= 20 -> 2
            count <= 40 -> 3
            else -> 5
        }

        measurements.forEachIndexed { index, measurement ->
            if (index % labelStep != 0 && index != count - 1) return@forEachIndexed

            val valueText = String.format(Locale.GERMANY, "%.1f", measurement.weightKg)
            val measured = textMeasurer.measure(valueText, valueLabelStyle)

            val cx = xFor(index)
            val cy = yFor(weights[index])

            var labelX = cx - measured.size.width / 2f
            var labelY = cy - measured.size.height - pointRadius - 4.dp.toPx()

            labelX = labelX.coerceIn(chartLeft - 8.dp.toPx(), size.width - measured.size.width)
            if (labelY < 0f) {
                labelY = cy + pointRadius + 4.dp.toPx()
            }

            drawText(
                textLayoutResult = measured,
                topLeft = Offset(labelX, labelY)
            )
        }

        // ---------- X-Achse: gleichmäßig verteilte Datums-Beschriftung ----------
        val labelTickCount = if (daySpan <= 0f) 1 else min(5, daySpan.toInt() + 1)

        for (k in 0 until labelTickCount) {
            val fraction = if (labelTickCount == 1) 0f else k / (labelTickCount - 1f)
            val day = firstDay + (daySpan * fraction).roundToInt()
            val label = LocalDate.ofEpochDay(day).format(dateFormatter)
            val measured = textMeasurer.measure(label, axisLabelStyle)

            val centerX = if (labelTickCount == 1) xForDay(day) else xForDay(day)
            val x = (centerX - measured.size.width / 2f)
                .coerceIn(chartLeft - 8.dp.toPx(), size.width - measured.size.width)

            drawText(
                textLayoutResult = measured,
                topLeft = Offset(x = x, y = chartBottom + 6.dp.toPx())
            )
        }
    }
}

/** Liefert einen "runden" Schritt (1, 2, 5, 10, 20, 50 …) für die Y-Achse. */
private fun niceStep(rough: Float): Float {
    val r = rough.coerceAtLeast(0.1f).toDouble()
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