package com.example.fitnesstracker.ui.weight

import com.example.fitnesstracker.ui.chart.chartPan
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnesstracker.domain.chart.ChartWindow
import com.example.fitnesstracker.domain.weight.ChartRange
import com.example.fitnesstracker.domain.weight.WeightCalculator
import com.example.fitnesstracker.domain.weight.WeightChartData
import com.example.fitnesstracker.domain.weight.WeightForecaster
import com.example.fitnesstracker.domain.weight.WeightFormat
import com.example.fitnesstracker.domain.weight.WeightGoalCalculator
import com.example.fitnesstracker.domain.weight.WeightInputParser
import com.example.fitnesstracker.domain.weight.WeightParseResult
import com.example.fitnesstracker.domain.weight.toMeasurement
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

// Feste Höhe von allem außer dem Diagramm (Titel, Tabs, Boxen, Zeitraum-Leiste, Abstände).
// Das Diagramm bekommt den Rest des Bildschirms, mindestens aber MIN_CHART_HEIGHT.
private val CHART_OVERHEAD = 384.dp
private val MIN_CHART_HEIGHT = 260.dp

/** Ein ungewöhnlicher Wert (< 20 kg oder > 400 kg), der erst nach Rückfrage gespeichert wird. */
private sealed interface PendingSave {
    val weightKg: Double

    data class Entry(override val weightKg: Double, val dateEpochDay: Long) : PendingSave
    data class Goal(override val weightKg: Double) : PendingSave
}

/** Prüft die Eingabe mit dem WeightInputParser (Komma/Punkt, "kg"-Suffix, nur positive Zahlen). */
private fun checkInput(
    input: String,
    onInvalid: (String) -> Unit,
    onValid: (weightKg: Double, isUnusual: Boolean) -> Unit
) {
    when (val result = WeightInputParser.parse(input)) {
        is WeightParseResult.Invalid -> onInvalid(result.error.message)
        is WeightParseResult.Valid -> onValid(result.weightKg, result.isUnusual)
    }
}

@Composable
fun WeightScreen(
    viewModel: WeightViewModel
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val goal by viewModel.goal.collectAsStateWithLifecycle()

    val todayEpochDay = LocalDate.now().toEpochDay()

    // ---------- UI-Zustand ----------
    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0 = Gewicht, 1 = Ziel
    var chartRange by rememberSaveable { mutableStateOf(ChartRange.WEEK) }
    // Ende des sichtbaren Fensters; null = heute (bzw. letzter Eintrag)
    var chartEnd by rememberSaveable { mutableStateOf<Long?>(null) }

    // Zielgewicht bearbeiten
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    var goalInput by rememberSaveable { mutableStateOf("") }
    var goalError by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeleteGoal by rememberSaveable { mutableStateOf(false) }

    // Zieldatum bearbeiten
    var showGoalDatePicker by rememberSaveable { mutableStateOf(false) }
    var showNeedGoalHint by rememberSaveable { mutableStateOf(false) }
    var dateError by rememberSaveable { mutableStateOf<String?>(null) }

    // Messpunkt im Diagramm antippen / bearbeiten / löschen (über die Id, damit der Zustand überlebt)
    var selectedEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isEditingEntry by rememberSaveable { mutableStateOf(false) }
    var entryEditInput by rememberSaveable { mutableStateOf("") }
    var entryEditError by rememberSaveable { mutableStateOf<String?>(null) }
    var entryToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Gewicht eintragen (Plus-Button)
    var isAddingWeight by rememberSaveable { mutableStateOf(false) }
    var addDateEpochDay by rememberSaveable { mutableStateOf(todayEpochDay) }
    var addInput by rememberSaveable { mutableStateOf("") }
    var addError by rememberSaveable { mutableStateOf<String?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    var pendingUnusual by remember { mutableStateOf<PendingSave?>(null) }

    // ---------- Berechnungen (alles aus der getesteten Domain-Schicht) ----------
    val measurements = entries.map { it.toMeasurement() }
    val stats = WeightCalculator.statistics(measurements, todayEpochDay)
    val change90Days = WeightCalculator.changeOverDays(measurements, 90, todayEpochDay)

    // Das Fenster lässt sich zwischen erstem und letztem Eintrag verschieben
    val firstDay = measurements.minOfOrNull { it.dateEpochDay } ?: todayEpochDay
    val lastDay = maxOf(todayEpochDay, measurements.maxOfOrNull { it.dateEpochDay } ?: todayEpochDay)
    val windowSpan = chartRange.days ?: 0
    val windowEnd = ChartWindow.clampEnd(chartEnd ?: todayEpochDay, windowSpan, firstDay, lastDay)
    val chartSlice = WeightChartData.sliceFor(measurements, chartRange, windowEnd)

    val goalProgress = goal?.let { currentGoal ->
        stats.latest?.let { latest ->
            WeightGoalCalculator.progress(
                targetKg = currentGoal.targetWeightKg,
                currentKg = latest.weightKg,
                startKg = currentGoal.startWeightKg
            )
        }
    }

    // Prognose: Trend der letzten 28 Tage, unabhängig davon, wann das Ziel angelegt wurde
    val forecast = goal?.let { currentGoal ->
        WeightForecaster.predictTrend(
            targetKg = currentGoal.targetWeightKg,
            startKg = currentGoal.startWeightKg,
            measurements = measurements,
            today = LocalDate.now().toEpochDay()
        )
    }

    // ---------- Aktionen ----------
    fun resetEntryDialogs() {
        selectedEntryId = null
        isEditingEntry = false
        entryEditError = null
    }

    fun commitEntry(weightKg: Double, dateEpochDay: Long) {
        viewModel.saveWeight(weightKg, LocalDate.ofEpochDay(dateEpochDay))
        isAddingWeight = false
        addInput = ""
        addError = null
        resetEntryDialogs()
    }

    fun commitGoal(weightKg: Double) {
        viewModel.saveGoal(weightKg)
        showGoalDialog = false
        goalInput = ""
        goalError = null
    }

    fun openGoalDialog() {
        goalInput = goal?.targetWeightKg?.let { WeightFormat.forInput(it) } ?: ""
        goalError = null
        showGoalDialog = true
    }

    // ---------- Layout ----------
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val chartHeight = (maxHeight - CHART_OVERHEAD).coerceAtLeast(MIN_CHART_HEIGHT)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 12.dp)
        ) {
            Text(
                text = "Gewicht",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            WeightTabs(
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Oberer Bereich: nur dieser Teil wechselt mit dem Tab, das Diagramm bleibt stehen
            if (selectedTab == 0) {
                StatsGrid(
                    latest = stats.latest,
                    changeToPrevious = stats.changeToPrevious,
                    changeTotal = stats.changeSinceFirst,
                    change7 = stats.change7Days,
                    change30 = stats.change30Days,
                    change90 = change90Days
                )
            } else {
                GoalGrid(
                    goal = goal,
                    currentKg = stats.latest?.weightKg,
                    progress = goalProgress,
                    forecast = forecast,
                    onTargetClick = { openGoalDialog() },
                    onDateClick = {
                        if (goal == null) showNeedGoalHint = true
                        else showGoalDatePicker = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ChartCard(
                slice = chartSlice,
                targetKg = goal?.targetWeightKg,
                height = chartHeight,
                // Punkte mit Werten nur in der Wochenansicht, sonst wird es zu voll
                showPoints = chartRange == ChartRange.WEEK,
                onPointClick = { entryId ->
                    isEditingEntry = false
                    selectedEntryId = entryId
                },
                modifier = Modifier.chartPan(
                    enabled = chartRange.days != null && ChartWindow.canPan(windowSpan, firstDay, lastDay),
                    visibleDays = windowSpan
                ) { delta ->
                    chartEnd = ChartWindow.clampEnd(windowEnd + delta, windowSpan, firstDay, lastDay)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RangeSelector(
                    selected = chartRange,
                    onSelect = {
                        chartRange = it
                        chartEnd = null
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                FloatingActionButton(
                    onClick = {
                        addDateEpochDay = todayEpochDay
                        addInput = entries.firstOrNull { it.dateEpochDay == todayEpochDay }
                            ?.weightKg?.let { WeightFormat.forInput(it) } ?: ""
                        addError = null
                        isAddingWeight = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = "+",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }

    // ---------- Dialoge: Gewicht eintragen ----------
    if (isAddingWeight) {
        AddWeightDialog(
            dateEpochDay = addDateEpochDay,
            existingKgForDate = entries
                .firstOrNull { it.dateEpochDay == addDateEpochDay }?.weightKg,
            input = addInput,
            error = addError,
            onInputChange = {
                addInput = it
                addError = null
            },
            onChangeDate = { showDatePicker = true },
            onSave = {
                checkInput(
                    input = addInput,
                    onInvalid = { addError = it },
                    onValid = { kg, unusual ->
                        if (unusual) pendingUnusual = PendingSave.Entry(kg, addDateEpochDay)
                        else commitEntry(kg, addDateEpochDay)
                    }
                )
            },
            onDismiss = {
                isAddingWeight = false
                addError = null
            }
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = addDateEpochDay * MILLIS_PER_DAY
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            addDateEpochDay = Math.floorDiv(millis, MILLIS_PER_DAY)
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    // ---------- Dialoge: Messpunkt (Bearbeiten / Löschen / Abbrechen) ----------
    val selectedEntry = entries.firstOrNull { it.id == selectedEntryId }
    val entryToDelete = entries.firstOrNull { it.id == entryToDeleteId }

    if (selectedEntry != null && entryToDelete == null) {
        if (!isEditingEntry) {
            EntryActionDialog(
                dateEpochDay = selectedEntry.dateEpochDay,
                weightKg = selectedEntry.weightKg,
                onEdit = {
                    entryEditInput = WeightFormat.forInput(selectedEntry.weightKg)
                    entryEditError = null
                    isEditingEntry = true
                },
                onDelete = { entryToDeleteId = selectedEntry.id },
                onDismiss = { resetEntryDialogs() }
            )
        } else {
            EditEntryDialog(
                dateEpochDay = selectedEntry.dateEpochDay,
                input = entryEditInput,
                error = entryEditError,
                onInputChange = {
                    entryEditInput = it
                    entryEditError = null
                },
                onSave = {
                    checkInput(
                        input = entryEditInput,
                        onInvalid = { entryEditError = it },
                        onValid = { kg, unusual ->
                            if (unusual) {
                                pendingUnusual = PendingSave.Entry(kg, selectedEntry.dateEpochDay)
                            } else {
                                commitEntry(kg, selectedEntry.dateEpochDay)
                            }
                        }
                    )
                },
                onBack = {
                    isEditingEntry = false
                    entryEditError = null
                }
            )
        }
    }

    if (entryToDelete != null) {
        ConfirmDeleteDialog(
            title = "Eintrag löschen?",
            message = "Der Eintrag vom ${WeightFormat.date(entryToDelete.dateEpochDay)} " +
                    "(${WeightFormat.weight(entryToDelete.weightKg)}) wird endgültig gelöscht.",
            onConfirm = {
                viewModel.deleteEntry(entryToDelete)
                entryToDeleteId = null
                resetEntryDialogs()
            },
            onDismiss = { entryToDeleteId = null }
        )
    }

    // ---------- Dialoge: Zielgewicht ----------
    if (showGoalDialog) {
        GoalWeightDialog(
            hasGoal = goal != null,
            input = goalInput,
            error = goalError,
            onInputChange = {
                goalInput = it
                goalError = null
            },
            onSave = {
                checkInput(
                    input = goalInput,
                    onInvalid = { goalError = it },
                    onValid = { kg, unusual ->
                        if (unusual) pendingUnusual = PendingSave.Goal(kg)
                        else commitGoal(kg)
                    }
                )
            },
            onRemove = { showDeleteGoal = true },
            onDismiss = {
                showGoalDialog = false
                goalError = null
            }
        )
    }

    if (showDeleteGoal) {
        ConfirmDeleteDialog(
            title = "Ziel entfernen?",
            message = "Dein Zielgewicht, das Zieldatum und der bisherige Fortschritt werden gelöscht. " +
                    "Deine Messungen bleiben erhalten.",
            onConfirm = {
                viewModel.deleteGoal()
                showDeleteGoal = false
                showGoalDialog = false
            },
            onDismiss = { showDeleteGoal = false }
        )
    }

    // ---------- Dialoge: Zieldatum ----------
    if (showNeedGoalHint) {
        InfoMessageDialog(
            message = "Lege zuerst ein Zielgewicht fest. Danach kannst du ein Zieldatum wählen.",
            confirmLabel = "Zielgewicht festlegen",
            onConfirm = {
                showNeedGoalHint = false
                openGoalDialog()
            },
            onDismiss = { showNeedGoalHint = false }
        )
    }

    if (showGoalDatePicker) {
        val currentDate = goal?.targetDateEpochDay
        val goalPickerState = rememberDatePickerState(
            initialSelectedDateMillis = (currentDate ?: todayEpochDay) * MILLIS_PER_DAY
        )

        DatePickerDialog(
            onDismissRequest = { showGoalDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = goalPickerState.selectedDateMillis
                            ?.let { Math.floorDiv(it, MILLIS_PER_DAY) }

                        when {
                            picked == null -> showGoalDatePicker = false
                            picked <= todayEpochDay ->
                                dateError = "Das Zieldatum muss in der Zukunft liegen."
                            else -> {
                                viewModel.saveGoalDate(picked)
                                showGoalDatePicker = false
                            }
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                Row {
                    if (currentDate != null) {
                        TextButton(
                            onClick = {
                                viewModel.saveGoalDate(null)
                                showGoalDatePicker = false
                            }
                        ) { Text("Entfernen", color = MaterialTheme.colorScheme.error) }
                    }
                    TextButton(onClick = { showGoalDatePicker = false }) { Text("Abbrechen") }
                }
            }
        ) {
            DatePicker(state = goalPickerState)
        }
    }

    dateError?.let { message ->
        InfoMessageDialog(
            message = message,
            confirmLabel = "OK",
            onConfirm = { dateError = null }
        )
    }

    // ---------- Rückfrage bei ungewöhnlichen Werten ----------
    pendingUnusual?.let { pending ->
        UnusualValueDialog(
            weightKg = pending.weightKg,
            onConfirm = {
                when (pending) {
                    is PendingSave.Entry -> commitEntry(pending.weightKg, pending.dateEpochDay)
                    is PendingSave.Goal -> commitGoal(pending.weightKg)
                }
                pendingUnusual = null
            },
            onDismiss = { pendingUnusual = null }
        )
    }
}
