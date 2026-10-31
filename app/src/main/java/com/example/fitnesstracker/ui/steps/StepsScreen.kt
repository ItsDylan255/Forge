package com.example.fitnesstracker.ui.steps

import com.example.fitnesstracker.i18n.tr
import com.example.fitnesstracker.ui.chart.chartPan
import com.example.fitnesstracker.domain.chart.ChartWindow
import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnesstracker.domain.steps.StepCalculator
import com.example.fitnesstracker.domain.steps.StepChartData
import com.example.fitnesstracker.domain.steps.StepChartRange
import com.example.fitnesstracker.domain.steps.StepDay
import com.example.fitnesstracker.domain.steps.StepGoalParseResult
import com.example.fitnesstracker.domain.steps.StepGoalParser
import com.example.fitnesstracker.domain.steps.toStepDay
import com.example.fitnesstracker.service.StepTracking
import java.time.LocalDate

// Feste Höhe von allem außer dem Diagramm (Titel, Heute-Karte, Statistik, Zeitraum-Leiste, Abstände).
// Das Diagramm bekommt den Rest des Bildschirms, mindestens aber MIN_CHART_HEIGHT.
// Beides ist knapp gehalten, damit die ganze Seite ohne Scrollen auf ein Handy passt.
private val CHART_OVERHEAD = 380.dp
private val MIN_CHART_HEIGHT = 180.dp

private fun hasActivityRecognition(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

private fun Context.findComponentActivity(): ComponentActivity {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    error("StepsScreen muss in einer ComponentActivity laufen.")
}

@Composable
fun StepsScreen(
    viewModel: StepsViewModel
) {
    val context = LocalContext.current
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val goal by viewModel.stepGoal.collectAsStateWithLifecycle()

    val todayEpochDay = LocalDate.now().toEpochDay()

    // ---------- Berechtigung ----------
    var hasPermission by remember { mutableStateOf(hasActivityRecognition(context)) }
    var permissionDeniedOnce by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) permissionDeniedOnce = true
    }

    // ---------- Sensor nur registrieren, solange der Screen sichtbar ist ----------
    val lifecycleOwner = remember(context) { context.findComponentActivity() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // Der Nutzer kann die Berechtigung inzwischen in den Einstellungen erteilt haben.
                    hasPermission = hasActivityRecognition(context)
                    // Der Zähl-Dienst läuft im Hintergrund, hier wird er (nach dem Erteilen der Berechtigung) gestartet
                    if (hasPermission) StepTracking.start(context)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Berechtigung wurde gerade erteilt, während der Screen schon sichtbar ist
    LaunchedEffect(hasPermission) {
        if (hasPermission && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            StepTracking.start(context)
        }
    }

    // ---------- UI-Zustand ----------
    var chartRange by rememberSaveable { mutableStateOf(StepChartRange.WEEK) }
    // Letzter sichtbarer Tag im Diagramm; null = heute
    var chartEnd by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedDayEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    var goalInput by rememberSaveable { mutableStateOf("") }
    var goalError by rememberSaveable { mutableStateOf<String?>(null) }

    // ---------- Berechnungen ----------
    val days = entries.map { it.toStepDay() }
    val todaySteps = StepCalculator.stepsOn(days, todayEpochDay)
    val progress = StepCalculator.goalProgress(todaySteps, goal)
    val week = StepCalculator.summary(days, 7, todayEpochDay)
    val month = StepCalculator.summary(days, 30, todayEpochDay)
    // Das Fenster lässt sich bis zum ersten Tag mit Schritten zurückwischen
    val firstStepDay = days.filter { it.steps > 0 }.minOfOrNull { it.dateEpochDay } ?: todayEpochDay
    val windowSpan = chartRange.days - 1
    val windowEnd = ChartWindow.clampEnd(chartEnd ?: todayEpochDay, windowSpan, firstStepDay, todayEpochDay)
    val bars = StepChartData.barsFor(days, chartRange, windowEnd)

    // ---------- Layout ----------
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val chartHeight = (maxHeight - CHART_OVERHEAD).coerceAtLeast(MIN_CHART_HEIGHT)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
        ) {
            Text(
                text = tr("Schritte", "Steps"),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!viewModel.isSensorAvailable) {
                StepsNoticeCard(message = tr("Dein Gerät unterstützt keinen Schrittzähler.", "Your device doesn't have a step counter."))
            } else {
                if (!hasPermission) {
                    StepsNoticeCard(
                        message = tr("Um deine Schritte zu zählen, braucht Forge die Berechtigung ", "To count your steps, Forge needs the ") +
                                tr("„Körperliche Aktivität“.", "“Physical activity” permission."),
                        primaryLabel = tr("Berechtigung erteilen", "Grant permission"),
                        onPrimary = {
                            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        },
                        // Nach einer Ablehnung zeigt Android den Dialog oft nicht mehr: dann Einstellungen
                        secondaryLabel = if (permissionDeniedOnce) tr("Einstellungen öffnen", "Open settings") else null,
                        onSecondary = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                TodayCard(
                    progress = progress,
                    onEditGoal = {
                        goalInput = goal.toString()
                        goalError = null
                        showGoalDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                SummaryCard(week = week, month = month)

                Spacer(modifier = Modifier.height(12.dp))

                StepChartSection(
                    bars = bars,
                    goal = goal,
                    range = chartRange,
                    chartHeight = chartHeight,
                    onRangeChange = {
                        chartRange = it
                        chartEnd = null
                    },
                    onBarClick = { selectedDayEpoch = it },
                    chartModifier = Modifier.chartPan(
                        enabled = ChartWindow.canPan(windowSpan, firstStepDay, todayEpochDay),
                        visibleDays = chartRange.days
                    ) { delta ->
                        chartEnd = ChartWindow.clampEnd(windowEnd + delta, windowSpan, firstStepDay, todayEpochDay)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // ---------- Dialoge ----------
    // Tippen auf einen Balken zeigt den Tag. Tage ohne Eintrag zeigen 0 Schritte.
    selectedDayEpoch?.let { epochDay ->
        StepDayDialog(
            day = StepDay(epochDay, StepCalculator.stepsOn(days, epochDay)),
            goal = goal,
            onDismiss = { selectedDayEpoch = null }
        )
    }

    if (showGoalDialog) {
        StepGoalDialog(
            input = goalInput,
            error = goalError,
            onInputChange = {
                goalInput = it
                goalError = null
            },
            onSave = {
                when (val result = StepGoalParser.parse(goalInput)) {
                    is StepGoalParseResult.Invalid -> goalError = result.error.message
                    is StepGoalParseResult.Valid -> {
                        viewModel.setStepGoal(result.goal)
                        showGoalDialog = false
                        goalError = null
                    }
                }
            },
            onDismiss = {
                showGoalDialog = false
                goalError = null
            }
        )
    }
}
