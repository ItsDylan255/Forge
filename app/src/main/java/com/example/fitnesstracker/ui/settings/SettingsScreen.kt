package com.example.fitnesstracker.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.PowerManager
import android.os.SystemClock
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.fitnesstracker.data.export.DataExporter
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.domain.export.DataExport
import com.example.fitnesstracker.data.local.ReminderPreferences
import com.example.fitnesstracker.data.local.StepPreferences
import com.example.fitnesstracker.data.local.WorkoutPreferences
import com.example.fitnesstracker.domain.calories.CalorieProfile
import com.example.fitnesstracker.data.repository.StepRepository
import com.example.fitnesstracker.domain.reminder.ReminderCalculator
import com.example.fitnesstracker.service.RestSignal
import com.example.fitnesstracker.service.StepCounterService
import com.example.fitnesstracker.service.StepTracking
import com.example.fitnesstracker.service.WeightReminderScheduler
import com.example.fitnesstracker.domain.calories.Sex
import com.example.fitnesstracker.ui.calories.CalorieGoalCard
import com.example.fitnesstracker.ui.calories.CalorieProfileDialog
import com.example.fitnesstracker.ui.calories.CalorieViewModel
import com.example.fitnesstracker.ui.calories.activityLabel
import com.example.fitnesstracker.ui.calories.sexLabel
import com.example.fitnesstracker.ui.gym.ConfirmDialog
import com.example.fitnesstracker.ui.gym.GroupLabel
import com.example.fitnesstracker.ui.gym.GymHeader
import com.example.fitnesstracker.ui.gym.GymPage
import com.example.fitnesstracker.ui.weight.ForgeCard

/** Einstellungen: persönliches Profil für das Kalorienziel. Ändert sich selten, daher hier statt im Essen-Tab. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    calorieViewModel: CalorieViewModel = viewModel()
) {
    val profile by calorieViewModel.profile.collectAsStateWithLifecycle()
    val target by calorieViewModel.target.collectAsStateWithLifecycle()

    var showProfile by rememberSaveable { mutableStateOf(false) }

    GymPage {
        GymHeader(title = "Einstellungen", onBack = onBack)

        GroupLabel("Erinnerungen")

        ReminderCard()

        GroupLabel("Training")

        TrainingCard()

        GroupLabel("Schritte")

        StepDiagnosticsCard()

        GroupLabel("Profil")

        ForgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                val p = profile
                if (p == null) {
                    Text(
                        text = "Noch kein Profil. Es wird für dein Kalorienziel gebraucht.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ProfileLine("Größe", "${p.heightCm} cm")
                    ProfileLine("Alter", "${p.ageYears} Jahre")
                    ProfileLine("Geschlecht", sexLabel(p.sex))
                    ProfileLine("Aktivität", activityLabel(p.activity))
                    ProfileLine("Makros", macroText(p))
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showProfile = true }) {
                        Text(if (p == null) "Profil anlegen" else "Profil bearbeiten")
                    }
                }
            }
        }

        GroupLabel("Berechnung")

        CalorieGoalCard(
            hasProfile = profile != null,
            target = target,
            onEditProfile = { showProfile = true },
            showEditButton = false
        )

        GroupLabel("Daten")

        ExportCard()
    }

    if (showProfile) {
        CalorieProfileDialog(
            initial = profile,
            onSave = {
                calorieViewModel.saveProfile(it)
                showProfile = false
            },
            onDismiss = { showProfile = false }
        )
    }
}

private fun macroText(p: CalorieProfile): String =
    "${p.split.proteinPercent} % Protein \u00B7 ${p.split.carbsPercent} % Carbs \u00B7 ${p.split.fatPercent} % Fett"

@Composable
private fun ProfileLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

/** Tägliche Erinnerung, das Gewicht einzutragen (nur wenn für heute noch nichts eingetragen ist). */
@Composable
private fun ReminderCard() {
    val context = LocalContext.current
    val prefs = remember { ReminderPreferences(context) }

    var enabled by remember { mutableStateOf(prefs.isEnabled()) }
    var hour by remember { mutableStateOf(prefs.getHour()) }
    var minute by remember { mutableStateOf(prefs.getMinute()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var notificationsOn by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsOn = granted || NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun save(newEnabled: Boolean, newHour: Int, newMinute: Int) {
        enabled = newEnabled
        hour = newHour
        minute = newMinute
        prefs.setEnabled(newEnabled)
        prefs.setTime(newHour, newMinute)
        WeightReminderScheduler.schedule(context)
    }

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(text = "Gewicht-Erinnerung", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Täglich, nur wenn heute noch kein Gewicht eingetragen ist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = { on ->
                        save(on, hour, minute)
                        if (on && Build.VERSION.SDK_INT >= 33 && !notificationsOn) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Uhrzeit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(onClick = { showTimePicker = true }) {
                        Text(
                            text = ReminderCalculator.format(hour, minute),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                if (!notificationsOn) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Benachrichtigungen sind für Forge ausgeschaltet. " +
                                "Erlaube sie in den Android-Einstellungen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
                        }
                    ) { Text("Einstellungen öffnen") }
                }
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            hour = hour,
            minute = minute,
            onConfirm = { h, m ->
                save(true, h, m)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

/** Export aller Daten als ZIP mit CSV-Dateien (für Excel, Sheets, Python, R). */
@Composable
private fun ExportCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var running by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        running = true
        status = null
        scope.launch {
            status = DataExporter.writeZip(context, uri) ?: "Export gespeichert."
            running = false
        }
    }

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "Daten exportieren", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Eine ZIP-Datei mit CSV-Tabellen: Gewicht, Schritte, Ernährung und Training, " +
                        "dazu eine Übersicht pro Tag. Lässt sich in Excel, Google Sheets oder Python öffnen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            status?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { launcher.launch(DataExport.fileName(LocalDate.now())) },
                    enabled = !running
                ) { Text(if (running) "Exportiert …" else "Exportieren") }
            }
        }
    }
}

/** Uhr zum Einstellen einer Uhrzeit (24 Stunden). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    hour: Int,
    minute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uhrzeit wählen") },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

/** Signalton und Vibration am Ende der Satzpause, einzeln ein- und ausschaltbar. */
@Composable
private fun TrainingCard() {
    val context = LocalContext.current
    val prefs = remember { WorkoutPreferences(context) }

    var sound by remember { mutableStateOf(prefs.isSoundEnabled()) }
    var vibration by remember { mutableStateOf(prefs.isVibrationEnabled()) }

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            SwitchLine(
                title = "Signalton am Pausenende",
                description = "Zwei Töne in der Medien-Lautstärke deines Handys.",
                checked = sound,
                onChange = {
                    sound = it
                    prefs.setSoundEnabled(it)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SwitchLine(
                title = "Vibration am Pausenende",
                description = "Zweimal kurz, auch bei gesperrtem Bildschirm.",
                checked = vibration,
                onChange = {
                    vibration = it
                    prefs.setVibrationEnabled(it)
                }
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { RestSignal.play(context, sound, vibration) },
                    enabled = sound || vibration
                ) { Text("Signal testen") }
            }
        }
    }
}

@Composable
private fun SwitchLine(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/**
 * Schrittzähler: Schalter fürs Zählen im Hintergrund, Zustand aller Voraussetzungen und die letzten
 * Messwerte. So lässt sich erkennen, warum Schritte fehlen.
 */
@Composable
private fun StepDiagnosticsCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { StepPreferences(context) }
    val database = remember { DatabaseProvider.getDatabase(context) }
    val repository = remember {
        StepRepository(database.stepEntryDao(), database.stepSampleDao(), prefs)
    }
    val recent by remember { database.stepSampleDao().observeRecent(8) }
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var background by remember { mutableStateOf(prefs.isBackgroundCountingEnabled()) }
    var showReset by remember { mutableStateOf(false) }

    // Zeigt, ob der Dienst gerade läuft, und aktualisiert sich nach dem Umschalten oder Neustarten
    var serviceRunning by remember { mutableStateOf(StepCounterService.running) }
    LaunchedEffect(Unit) {
        while (true) {
            serviceRunning = StepCounterService.running
            delay(1_000L)
        }
    }

    val hasSensor = context.getSystemService(SensorManager::class.java)
        ?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    val hasPermission = StepTracking.hasPermission(context)
    val ignoresBattery = context.getSystemService(PowerManager::class.java)
        ?.isIgnoringBatteryOptimizations(context.packageName) == true

    val timeFormat = remember { DateTimeFormatter.ofPattern("dd.MM. HH:mm") }

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            SwitchLine(
                title = "Im Hintergrund zählen",
                description = "Zeigt eine kleine, stumme Benachrichtigung mit deinen Schritten. " +
                        "Nur so liefert Android den Schrittzähler auch bei gesperrtem Handy zuverlässig.",
                checked = background,
                onChange = {
                    background = it
                    prefs.setBackgroundCountingEnabled(it)
                    StepTracking.start(context)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileLine("Zähl-Dienst", if (serviceRunning) "läuft" else "aus")
            ProfileLine("Schrittzähler-Sensor", if (hasSensor) "vorhanden" else "fehlt")
            ProfileLine("Berechtigung Aktivität", if (hasPermission) "erteilt" else "fehlt")
            ProfileLine("Akku-Optimierung", if (ignoresBattery) "ausgenommen" else "aktiv")

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!ignoresBattery) {
                    TextButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:" + context.packageName)
                                )
                            )
                        }
                    ) { Text("Akku-Ausnahme") }
                }

                TextButton(onClick = { StepTracking.restart(context) }) { Text("Neu starten") }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showReset = true }) {
                    Text("Schrittdaten zurücksetzen", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Letzte Messwerte",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (recent.isEmpty()) {
                Text(
                    text = "Noch kein Messwert.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                recent.forEach { sample ->
                    val time = Instant.ofEpochMilli(sample.timeMillis).atZone(ZoneId.systemDefault())
                        .format(timeFormat)
                    Text(
                        text = "$time \u00B7 Rohwert ${sample.total}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    if (showReset) {
        ConfirmDialog(
            title = "Schrittdaten zurücksetzen?",
            message = "Alle Tageswerte und Messwerte der Schritte werden gelöscht. Danach zählt die App von vorn.",
            confirmLabel = "Zurücksetzen",
            onConfirm = {
                scope.launch { repository.resetAll() }
                showReset = false
            },
            onDismiss = { showReset = false }
        )
    }
}
