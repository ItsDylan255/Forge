package com.example.fitnesstracker.ui.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnesstracker.domain.weight.WeightChartSlice
import com.example.fitnesstracker.domain.weight.WeightMeasurement
import com.example.fitnesstracker.ui.theme.ForgeColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Liniendiagramm für Gewichtsmessungen. Alle Werte sind echte Messungen, nichts wird interpoliert.
 *
 * - Y-Achse mit "runden" Schritten und gepunkteten Hilfslinien
 * - X-Achse zeitlich korrekt über das ganze Fenster des Ausschnitts (Abstand der Punkte = Abstand der Tage)
 * - Die Linie läuft bis zum Rand weiter, wenn es davor oder danach Messungen gibt
 * - Hohle Punkte mit Wert-Label (nur mit [showPoints])
 * - Zielgewicht als gestrichelte Linie
 * - Tippen auf einen Punkt meldet die zugehörige Messung über [onPointClick]
 *
 * Die Größe bestimmt der Aufrufer über [modifier] (z. B. fillMaxSize in einer Karte mit fester Höhe).
 *
 * @param slice    sichtbarer Ausschnitt (echte Messungen, chronologisch aufsteigend)
 * @param targetKg optional: Zielgewicht für die gestrichelte Linie
 */
@Composable
fun WeightChart(
    slice: WeightChartSlice,
    targetKg: Double?,
    modifier: Modifier = Modifier,
    showPoints: Boolean = true,
    onPointClick: ((WeightMeasurement) -> Unit)? = null
) {
    val measurements = slice.visible
    val line = slice.line

    if (measurements.isEmpty()) {
        Box(
            modifier = modifier.padding(16.dp),
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
    val pointFillColor = MaterialTheme.colorScheme.surface
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
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .pointerInput(slice, targetKg, onPointClick) {
                if (onPointClick == null) return@pointerInput
                val density: Density = this

                detectTapGestures(onTap = { tap ->
                    val g = ChartGeometry(
                        measurements, targetKg, slice.startDay, slice.endDay,
                        size.width.toFloat(), size.height.toFloat(), density
                    )
                    if (!g.isDrawable) return@detectTapGestures

                    // Nächster Messpunkt zum Fingertipp, aber nur innerhalb eines großzügigen Radius
                    var bestIndex = -1
                    var bestDistance = Float.MAX_VALUE
                    measurements.forEachIndexed { index, m ->
                        val dx = g.xForDay(m.dateEpochDay) - tap.x
                        val dy = g.yFor(m.weightKg.toFloat()) - tap.y
                        val distance = hypot(dx, dy)
                        if (distance < bestDistance) {
                            bestDistance = distance
                            bestIndex = index
                        }
                    }

                    if (bestIndex >= 0 && bestDistance <= with(density) { 40.dp.toPx() }) {
                        onPointClick(measurements[bestIndex])
                    }
                })
            }
    ) {
        val g = ChartGeometry(measurements, targetKg, slice.startDay, slice.endDay, size.width, size.height, this)
        if (!g.isDrawable) return@Canvas

        val weights = measurements.map { it.weightKg.toFloat() }
        val targetFloat = targetKg?.toFloat()

        fun xFor(index: Int): Float = g.xForDay(measurements[index].dateEpochDay)

        // ---------- Y-Gitterlinien (gepunktet) + Beschriftung ----------
        val decimals = if (g.step < 1f) 1 else 0
        val lineCount = ((g.maxValue - g.minValue) / g.step).roundToInt()
        val dotted = PathEffect.dashPathEffect(floatArrayOf(2f, 8f), 0f)

        for (i in 0..lineCount) {
            val value = g.minValue + g.step * i
            val y = g.yFor(value)

            drawLine(
                color = gridColor,
                start = Offset(g.chartLeft, y),
                end = Offset(g.chartRight, y),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
                pathEffect = dotted
            )

            val label = String.format(Locale.GERMANY, "%.${decimals}f", value)
            val measured = textMeasurer.measure(label, axisLabelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = g.chartLeft - measured.size.width - 8.dp.toPx(),
                    y = y - measured.size.height / 2f
                )
            )
        }

        // ---------- Zielgewicht (gestrichelt) ----------
        if (targetFloat != null) {
            val y = g.yFor(targetFloat)
            drawLine(
                color = lineColor,
                start = Offset(g.chartLeft, y),
                end = Offset(g.chartRight, y),
                strokeWidth = 2.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(14.dp.toPx(), 8.dp.toPx()), 0f
                )
            )
        }

        // ---------- Linie durch die Messpunkte (inkl. Nachbarn außerhalb, am Rand abgeschnitten) ----------
        if (line.size >= 2) {
            val path = Path().apply {
                moveTo(g.xForDay(line[0].dateEpochDay), g.yFor(line[0].weightKg.toFloat()))
                for (i in 1 until line.size) {
                    lineTo(g.xForDay(line[i].dateEpochDay), g.yFor(line[i].weightKg.toFloat()))
                }
            }
            clipRect(left = g.chartLeft, top = 0f, right = g.chartRight, bottom = g.chartBottom) {
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
        }

        // ---------- Hohle Punkte ----------
        val pointRadius = 5.dp.toPx()
        if (showPoints) measurements.forEachIndexed { index, _ ->
            val center = Offset(xFor(index), g.yFor(weights[index]))
            drawCircle(color = pointFillColor, radius = pointRadius, center = center)
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

        if (showPoints) measurements.forEachIndexed { index, measurement ->
            if (index % labelStep != 0 && index != count - 1) return@forEachIndexed

            val valueText = String.format(Locale.GERMANY, "%.1f", measurement.weightKg)
            val measured = textMeasurer.measure(valueText, valueLabelStyle)

            val cx = xFor(index)
            val cy = g.yFor(weights[index])

            var labelX = cx - measured.size.width / 2f
            var labelY = cy - measured.size.height - pointRadius - 4.dp.toPx()

            labelX = labelX.coerceIn(g.chartLeft - 8.dp.toPx(), size.width - measured.size.width)
            if (labelY < 0f) {
                labelY = cy + pointRadius + 4.dp.toPx()
            }

            drawText(
                textLayoutResult = measured,
                topLeft = Offset(labelX, labelY)
            )
        }

        // ---------- X-Achse: gleichmäßig verteilte Datums-Beschriftung ----------
        val labelTickCount = if (g.daySpan <= 0f) 1 else min(5, g.daySpan.toInt() + 1)

        for (k in 0 until labelTickCount) {
            val fraction = if (labelTickCount == 1) 0f else k / (labelTickCount - 1f)
            val day = g.firstDay + (g.daySpan * fraction).roundToInt()
            val label = LocalDate.ofEpochDay(day).format(dateFormatter)
            val measured = textMeasurer.measure(label, axisLabelStyle)

            val x = (g.xForDay(day) - measured.size.width / 2f)
                .coerceIn(g.chartLeft - 8.dp.toPx(), size.width - measured.size.width)

            drawText(
                textLayoutResult = measured,
                topLeft = Offset(x = x, y = g.chartBottom + 6.dp.toPx())
            )
        }
    }
}

/**
 * Gemeinsame Geometrie von Zeichnen und Antippen, damit beide immer dieselben Punktpositionen sehen.
 * [width] und [height] sind die Maße der Zeichenfläche in Pixeln.
 */
private class ChartGeometry(
    measurements: List<WeightMeasurement>,
    targetKg: Double?,
    startDay: Long,
    endDay: Long,
    width: Float,
    height: Float,
    density: Density
) {
    val chartLeft: Float = with(density) { 40.dp.toPx() }
    val chartRight: Float = width - with(density) { 16.dp.toPx() }
    val chartTop: Float = with(density) { 16.dp.toPx() }
    val chartBottom: Float = height - with(density) { 28.dp.toPx() }

    val chartWidth: Float = chartRight - chartLeft
    val chartHeight: Float = chartBottom - chartTop
    val isDrawable: Boolean = chartWidth > 0f && chartHeight > 0f

    val minValue: Float
    val maxValue: Float
    val step: Float
    private val valueRange: Float

    val firstDay: Long = startDay
    val daySpan: Float = (endDay - startDay).toFloat()

    init {
        val weights = measurements.map { it.weightKg.toFloat() }
        val weightMin = weights.minOrNull() ?: 0f
        val weightMax = weights.maxOrNull() ?: 0f
        val targetFloat = targetKg?.toFloat()

        val dataMin = minOf(weightMin, targetFloat ?: weightMin)
        val dataMax = maxOf(weightMax, targetFloat ?: weightMax)

        val span = (dataMax - dataMin).coerceAtLeast(1f)
        step = niceStep(span / 3f)

        minValue = floor((dataMin - span * 0.1f) / step) * step
        maxValue = ceil((dataMax + span * 0.15f) / step) * step
        valueRange = (maxValue - minValue).coerceAtLeast(0.1f)
    }

    fun yFor(value: Float): Float =
        chartBottom - ((value - minValue) / valueRange) * chartHeight

    fun xForDay(day: Long): Float =
        if (daySpan <= 0f) {
            (chartLeft + chartRight) / 2f
        } else {
            chartLeft + chartWidth * ((day - firstDay) / daySpan)
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
