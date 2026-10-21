package com.example.fitnesstracker.ui.gym

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
import com.example.fitnesstracker.domain.gym.GymFormat
import com.example.fitnesstracker.ui.theme.ForgeColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** Balkendiagramm für Volumen (z. B. pro Woche oder pro Training). */
@Composable
fun GymBarChart(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val barColor = ForgeColors.ChartLine
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline

    val axisStyle = TextStyle(color = secondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    val valueStyle = TextStyle(color = onSurface, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        if (values.isEmpty()) return@Canvas

        val chartLeft = 44.dp.toPx()
        val chartRight = size.width - 8.dp.toPx()
        val chartTop = 20.dp.toPx()
        val chartBottom = size.height - 28.dp.toPx()
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop
        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        val dataMax = (values.maxOrNull() ?: 0.0).toFloat().coerceAtLeast(1f)
        val step = niceStep(dataMax / 3f)
        val maxValue = ceil(dataMax * 1.1f / step) * step

        fun yFor(value: Float): Float = chartBottom - (value / maxValue) * chartHeight

        val dotted = PathEffect.dashPathEffect(floatArrayOf(2f, 8f), 0f)
        for (i in 0..(maxValue / step).roundToInt()) {
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

            val measured = textMeasurer.measure(GymFormat.compact(value.toDouble()), axisStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(chartLeft - measured.size.width - 8.dp.toPx(), y - measured.size.height / 2f)
            )
        }

        val count = values.size
        val slot = chartWidth / count
        val barWidth = (slot * 0.62f).coerceAtMost(28.dp.toPx())

        values.forEachIndexed { index, value ->
            if (value <= 0.0) return@forEachIndexed

            val x = chartLeft + slot * index + (slot - barWidth) / 2f
            val barHeight = (value.toFloat() / maxValue) * chartHeight

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, chartBottom - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            if (count <= 8) {
                val measured = textMeasurer.measure(GymFormat.compact(value), valueStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = (chartLeft + slot * index + slot / 2f - measured.size.width / 2f)
                            .coerceIn(0f, size.width - measured.size.width),
                        y = (chartBottom - barHeight - measured.size.height - 3.dp.toPx()).coerceAtLeast(0f)
                    )
                )
            }
        }

        labels.forEachIndexed { index, label ->
            if (count > 8 && (count - 1 - index) % 2 != 0) return@forEachIndexed

            val measured = textMeasurer.measure(label, axisStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = (chartLeft + slot * index + slot / 2f - measured.size.width / 2f)
                        .coerceIn(0f, size.width - measured.size.width),
                    y = chartBottom + 6.dp.toPx()
                )
            )
        }
    }
}

/** Liniendiagramm mit hohlen Punkten (z. B. Höchstgewicht einer Übung über die Zeit). */
@Composable
fun GymLineChart(
    epochDays: List<Long>,
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val lineColor = ForgeColors.ChartLine
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outline
    val fillColor = MaterialTheme.colorScheme.surface

    val axisStyle = TextStyle(color = secondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    val valueStyle = TextStyle(color = onSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMANY)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        if (values.isEmpty() || values.size != epochDays.size) return@Canvas

        val chartLeft = 44.dp.toPx()
        val chartRight = size.width - 16.dp.toPx()
        val chartTop = 20.dp.toPx()
        val chartBottom = size.height - 28.dp.toPx()
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop
        if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

        val floats = values.map { it.toFloat() }
        val dataMin = floats.minOrNull() ?: 0f
        val dataMax = floats.maxOrNull() ?: 0f
        val span = (dataMax - dataMin).coerceAtLeast(1f)
        val step = niceStep(span / 3f)
        val minValue = floor((dataMin - span * 0.1f) / step) * step
        val maxValue = ceil((dataMax + span * 0.15f) / step) * step
        val valueRange = (maxValue - minValue).coerceAtLeast(0.1f)

        fun yFor(value: Float): Float = chartBottom - ((value - minValue) / valueRange) * chartHeight

        val firstDay = epochDays.first()
        val daySpan = (epochDays.last() - firstDay).toFloat()

        fun xFor(index: Int): Float =
            if (daySpan <= 0f) (chartLeft + chartRight) / 2f
            else chartLeft + chartWidth * ((epochDays[index] - firstDay) / daySpan)

        val decimals = if (step < 1f) 1 else 0
        val dotted = PathEffect.dashPathEffect(floatArrayOf(2f, 8f), 0f)

        for (i in 0..((maxValue - minValue) / step).roundToInt()) {
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

            val measured = textMeasurer.measure(
                String.format(Locale.GERMANY, "%.${decimals}f", value), axisStyle
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(chartLeft - measured.size.width - 8.dp.toPx(), y - measured.size.height / 2f)
            )
        }

        if (values.size >= 2) {
            val path = Path().apply {
                moveTo(xFor(0), yFor(floats[0]))
                for (i in 1 until floats.size) lineTo(xFor(i), yFor(floats[i]))
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        val radius = 5.dp.toPx()
        floats.forEachIndexed { index, value ->
            val center = Offset(xFor(index), yFor(value))
            drawCircle(color = fillColor, radius = radius, center = center)
            drawCircle(color = lineColor, radius = radius, center = center, style = Stroke(width = 2.5.dp.toPx()))
        }

        val labelStep = if (values.size <= 8) 1 else 2
        floats.forEachIndexed { index, value ->
            if (index % labelStep != 0 && index != floats.size - 1) return@forEachIndexed

            val measured = textMeasurer.measure(GymFormat.number(values[index]), valueStyle)
            val cy = yFor(value)
            var labelY = cy - measured.size.height - radius - 4.dp.toPx()
            if (labelY < 0f) labelY = cy + radius + 4.dp.toPx()

            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = (xFor(index) - measured.size.width / 2f)
                        .coerceIn(chartLeft - 8.dp.toPx(), size.width - measured.size.width),
                    y = labelY
                )
            )
        }

        val ticks = if (daySpan <= 0f) 1 else minOf(4, daySpan.toInt() + 1)
        for (k in 0 until ticks) {
            val fraction = if (ticks == 1) 0f else k / (ticks - 1f)
            val day = firstDay + (daySpan * fraction).roundToInt()
            val x = if (daySpan <= 0f) (chartLeft + chartRight) / 2f
            else chartLeft + chartWidth * ((day - firstDay) / daySpan)

            val measured = textMeasurer.measure(LocalDate.ofEpochDay(day).format(dateFormatter), axisStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = (x - measured.size.width / 2f).coerceIn(chartLeft - 8.dp.toPx(), size.width - measured.size.width),
                    y = chartBottom + 6.dp.toPx()
                )
            )
        }
    }
}

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
