package com.example.fitnesstracker.ui.weight

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.example.fitnesstracker.domain.weight.ChartRange
import com.example.fitnesstracker.domain.weight.WeightCalculator
import com.example.fitnesstracker.domain.weight.WeightChartData
import com.example.fitnesstracker.domain.weight.WeightFormat
import com.example.fitnesstracker.domain.weight.WeightGoalCalculator
import com.example.fitnesstracker.domain.weight.WeightInputParser
import com.example.fitnesstracker.domain.weight.WeightParseResult
import com.example.fitnesstracker.domain.weight.toMeasurement
import java.time.LocalDate

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

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
    var chartRange by rememberSaveable { mutableStateOf(ChartRange.MONTH) }

    // Ziel bearbeiten
    var isEditingGoal by rememberSaveable { mutableStateOf(false) }
    var goalInput by rememberSaveable { mutableStateOf("") }
    var goalError by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeleteGoal by rememberSaveable { mutableStateOf(false) }

    // Verlaufseintrag antippen / bearbeiten / löschen (über die Id, damit der Zustand überlebt)
    var selectedEntryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isEditingEntry by rememberSaveable { mutableStateOf(false) }
    var entryEditInput by rememberSaveable { mutableStateOf("") }
    var entryEditError by rememberSaveable { mutableStateOf<String?>(null) }
    var entryToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Gewicht eintragen (FAB)
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
    val historyItems = WeightCalculator.historyItems(measurements)

    val chartPoints = WeightChartData.pointsFor(measurements, chartRange, todayEpochDay)

    val goalProgress = goal?.let { currentGoal ->
        stats.latest?.let { latest ->
            WeightGoalCalculator.progress(
                targetKg = currentGoal.targetWeightKg,
                currentKg = latest.weightKg,
                startKg = currentGoal.startWeightKg
            )
        }
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
        isEditingGoal = false
        goalInput = ""
        goalError = null
    }

    // ---------- Layout ----------
    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp)
        ) {
            Text(
                text = "Gewicht",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            WeightTabs(
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                StatsCard(
                    latest = stats.latest,
                    changeToPrevious = stats.changeToPrevious,
                    changeTotal = stats.changeSinceFirst,
                    change7 = stats.change7Days,
                    change30 = stats.change30Days,
                    change90 = change90Days
                )

                Spacer(modifier = Modifier.height(16.dp))

                ChartSection(
                    points = chartPoints,
                    hasEnoughPoints = WeightChartData.hasEnoughPoints(chartPoints),
                    targetKg = goal?.targetWeightKg,
                    range = chartRange,
                    onRangeChange = { chartRange = it }
                )

                Spacer(modifier = Modifier.height(28.dp))

                SectionLabel("Gewichtsverlauf")

                Spacer(modifier = Modifier.height(10.dp))

                HistoryList(
                    items = historyItems,
                    onItemClick = { entryId ->
                        isEditingEntry = false
                        selectedEntryId = entryId
                    }
                )
            } else {
                GoalCard(
                    targetKg = goal?.targetWeightKg,
                    startKg = goal?.startWeightKg,
                    progress = goalProgress,
                    isEditing = isEditingGoal,
                    input = goalInput,
                    error = goalError,
                    onInputChange = {
                        goalInput = it
                        goalError = null
                    },
                    onStartEdit = {
                        goalInput = goal?.targetWeightKg?.let { WeightFormat.forInput(it) } ?: ""
                        goalError = null
                        isEditingGoal = true
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
                    onCancel = {
                        isEditingGoal = false
                        goalInput = ""
                        goalError = null
                    },
                    onDelete = { showDeleteGoal = true }
                )
            }

            // Platz, damit der Plus-Button den letzten Eintrag nicht verdeckt
            Spacer(modifier = Modifier.height(96.dp))
        }

        // Plus-Button nur im Gewicht-Tab
        if (selectedTab == 0) {
            FloatingActionButton(
                onClick = {
                    addDateEpochDay = todayEpochDay
                    addInput = entries.firstOrNull { it.dateEpochDay == todayEpochDay }
                        ?.weightKg?.let { WeightFormat.forInput(it) } ?: ""
                    addError = null
                    isAddingWeight = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Text(
                    text = "+",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }

    // ---------- Dialoge ----------

    // Gewicht eintragen
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

    // Datum wählen
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

    // Verlaufseintrag: Bearbeiten / Löschen / Abbrechen
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

    // Sicherheitsfrage: Eintrag löschen
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

    // Sicherheitsfrage: Ziel entfernen
    if (showDeleteGoal) {
        ConfirmDeleteDialog(
            title = "Ziel entfernen?",
            message = "Dein Zielgewicht und der bisherige Fortschritt werden gelöscht.",
            onConfirm = {
                viewModel.deleteGoal()
                showDeleteGoal = false
            },
            onDismiss = { showDeleteGoal = false }
        )
    }

    // Rückfrage bei ungewöhnlichen Werten
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
