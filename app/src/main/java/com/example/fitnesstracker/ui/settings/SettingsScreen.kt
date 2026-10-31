package com.example.fitnesstracker.ui.settings

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.PowerManager
import android.os.SystemClock
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnesstracker.i18n.AppLanguage
import com.example.fitnesstracker.i18n.L10n
import com.example.fitnesstracker.i18n.tr
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.fitnesstracker.data.export.DataExporter
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.domain.export.DataExport
import com.example.fitnesstracker.data.local.PhotoReminderPreferences
import com.example.fitnesstracker.data.local.ReminderPreferences
import com.example.fitnesstracker.data.local.StepPreferences
import com.example.fitnesstracker.data.local.WorkoutPreferences
import com.example.fitnesstracker.domain.calories.CalorieProfile
import com.example.fitnesstracker.data.repository.StepRepository
import com.example.fitnesstracker.domain.photos.PhotoReminderCalculator
import com.example.fitnesstracker.domain.photos.PhotoSchedule
import com.example.fitnesstracker.domain.reminder.ReminderCalculator
import com.example.fitnesstracker.service.PhotoReminderScheduler
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
import com.example.fitnesstracker.ui.photos.dueText
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
        GymHeader(title = tr("Einstellungen", "Settings"), onBack = onBack)

        GroupLabel(tr("Sprache", "Language"))

        LanguageCard()

        GroupLabel(tr("Erinnerungen", "Reminders"))

        ReminderCard()

        GroupLabel(tr("Fortschrittsfotos", "Progress photos"))

        PhotoReminderCard()

        GroupLabel("Training")

        TrainingCard()

        GroupLabel(tr("Schritte", "Steps"))

        StepDiagnosticsCard()

        GroupLabel(tr("Profil", "Profile"))

        ForgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                val p = profile
                if (p == null) {
                    Text(
                        text = tr("Noch kein Profil. Es wird für dein Kalorienziel gebraucht.", "No profile yet. It's needed for your calorie goal."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ProfileLine(tr("Größe", "Height"), "${p.heightCm} cm")
                    ProfileLine(tr("Alter", "Age"), tr("${p.ageYears} Jahre", "${p.ageYears} years"))
                    ProfileLine(tr("Geschlecht", "Sex"), sexLabel(p.sex))
                    ProfileLine(tr("Aktivität", "Activity"), activityLabel(p.activity))
                    ProfileLine(tr("Makros", "Macros"), macroText(p))
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showProfile = true }) {
                        Text(if (p == null) tr("Profil anlegen", "Create profile") else tr("Profil bearbeiten", "Edit profile"))
                    }
                }
            }
        }

        GroupLabel(tr("Berechnung", "Calculation"))

        CalorieGoalCard(
            hasProfile = profile != null,
            target = target,
            onEditProfile = { showProfile = true },
            showEditButton = false
        )

        GroupLabel(tr("Daten", "Data"))

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
    tr("${p.split.proteinPercent} % Protein \u00B7 ${p.split.carbsPercent} % Carbs \u00B7 ${p.split.fatPercent} % Fett", "${p.split.proteinPercent} % protein · ${p.split.carbsPercent} % carbs · ${p.split.fatPercent} % fat")

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

/** Sprache der App. Nach dem Wechsel wird der Bildschirm neu aufgebaut, damit alle Texte passen. */
@Composable
private fun LanguageCard() {
    val context = LocalContext.current
    val selected = remember { L10n.load(context) }

    val options = listOf(
        AppLanguage.SYSTEM to tr("Wie das System", "Same as system"),
        AppLanguage.GERMAN to "Deutsch",
        AppLanguage.ENGLISH to "English"
    )

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            options.forEach { (language, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = language == selected,
                            role = Role.RadioButton,
                            onClick = {
                                if (language != selected) {
                                    L10n.save(context, language)
                                    context.findActivity()?.recreate()
                                }
                            }
                        )
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = language == selected, onClick = null)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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
                    Text(text = tr("Gewicht-Erinnerung", "Weight reminder"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = tr("Täglich, nur wenn heute noch kein Gewicht eingetragen ist.", "Daily, only if you haven't logged your weight today."),
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
                        text = tr("Uhrzeit", "Time"),
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
                        text = tr("Benachrichtigungen sind für Forge ausgeschaltet. ", "Notifications are turned off for Forge. ") +
                                tr("Erlaube sie in den Android-Einstellungen.", "Allow them in the Android settings."),
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
                    ) { Text(tr("Einstellungen öffnen", "Open settings")) }
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
            status = DataExporter.writeZip(context, uri) ?: tr("Export gespeichert.", "Export saved.")
            running = false
        }
    }

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = tr("Daten exportieren", "Export data"), style = MaterialTheme.typography.titleMedium)
            Text(
                text = tr("Eine ZIP-Datei mit CSV-Tabellen: Gewicht, Schritte, Ernährung und Training, ", "A ZIP file with CSV tables: weight, steps, nutrition and training, ") +
                        tr("dazu eine Übersicht pro Tag. Lässt sich in Excel, Google Sheets oder Python öffnen.", "plus a daily summary. Opens in Excel, Google Sheets or Python."),
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
                ) { Text(if (running) tr("Exportiert …", "Exporting …") else tr("Exportieren", "Export")) }
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
        title = { Text(tr("Uhrzeit wählen", "Choose time")) },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Abbrechen", "Cancel")) } }
    )
}

/**
 * Rhythmus der Fortschrittsfotos: alle X Tage, jede Woche an einem Wochentag oder am ersten
 * Wochentag im Monat. Erinnert per Benachrichtigung und auf der Startseite.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotoReminderCard() {
    val context = LocalContext.current
    val prefs = remember { PhotoReminderPreferences(context) }

    var schedule by remember { mutableStateOf(prefs.getSchedule()) }
    var hour by remember { mutableStateOf(prefs.getHour()) }
    var minute by remember { mutableStateOf(prefs.getMinute()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var notificationsOn by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val lastPhotoDay by produceState<Long?>(initialValue = null) {
        value = DatabaseProvider.getDatabase(context).progressPhotoDao().lastPhotoDay()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsOn = granted || NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // Zuletzt gewählte Werte, damit beim Wechsel der Art nichts verloren geht
    val days = (schedule as? PhotoSchedule.EveryDays)?.days ?: PhotoReminderPreferences.DEFAULT_DAYS
    val weekday = when (val current = schedule) {
        is PhotoSchedule.Weekly -> current.day
        is PhotoSchedule.MonthlyFirst -> current.day
        else -> DayOfWeek.SUNDAY
    }

    fun save(newSchedule: PhotoSchedule) {
        val wasOff = schedule == PhotoSchedule.Off
        schedule = newSchedule
        prefs.setSchedule(newSchedule)
        PhotoReminderScheduler.schedule(context)
        if (wasOff && newSchedule != PhotoSchedule.Off && Build.VERSION.SDK_INT >= 33 && !notificationsOn) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = tr("Erinnerung an Fotos", "Photo reminder"), style = MaterialTheme.typography.titleMedium)
            Text(
                text = tr(
                    "Wie oft du ein Fortschrittsfoto machen willst. Der nächste Termin steht auch auf der Startseite.",
                    "How often you want to take a progress photo. The next date also shows on the home page."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = schedule == PhotoSchedule.Off,
                    onClick = { save(PhotoSchedule.Off) },
                    label = { Text(tr("Aus", "Off")) }
                )
                FilterChip(
                    selected = schedule is PhotoSchedule.EveryDays,
                    onClick = { save(PhotoSchedule.EveryDays(days)) },
                    label = { Text(tr("Alle X Tage", "Every X days")) }
                )
                FilterChip(
                    selected = schedule is PhotoSchedule.Weekly,
                    onClick = { save(PhotoSchedule.Weekly(weekday)) },
                    label = { Text(tr("Wöchentlich", "Weekly")) }
                )
                FilterChip(
                    selected = schedule is PhotoSchedule.MonthlyFirst,
                    onClick = { save(PhotoSchedule.MonthlyFirst(weekday)) },
                    label = { Text(tr("Monatlich", "Monthly")) }
                )
            }

            when (val current = schedule) {
                is PhotoSchedule.EveryDays -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Alle", "Every"), style = MaterialTheme.typography.bodyMedium)
                        TextButton(
                            onClick = { save(PhotoSchedule.EveryDays(current.days - 1)) },
                            enabled = current.days > PhotoSchedule.MIN_DAYS
                        ) { Text("\u2212", style = MaterialTheme.typography.titleLarge) }
                        Text(current.days.toString(), style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = { save(PhotoSchedule.EveryDays(current.days + 1)) },
                            enabled = current.days < PhotoSchedule.MAX_DAYS
                        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                        Text(
                            if (current.days == 1) tr("Tag", "day") else tr("Tage", "days"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                is PhotoSchedule.Weekly, is PhotoSchedule.MonthlyFirst -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (current is PhotoSchedule.Weekly) tr("Jeden", "Every")
                        else tr("Am ersten dieser Wochentage im Monat", "On the first of this weekday each month"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            FilterChip(
                                selected = day == weekday,
                                onClick = {
                                    save(if (current is PhotoSchedule.Weekly) PhotoSchedule.Weekly(day) else PhotoSchedule.MonthlyFirst(day))
                                },
                                label = { Text(day.getDisplayName(JavaTextStyle.SHORT, L10n.locale)) }
                            )
                        }
                    }
                }

                PhotoSchedule.Off -> Unit
            }

            if (schedule != PhotoSchedule.Off) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Uhrzeit der Erinnerung", "Reminder time"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(onClick = { showTimePicker = true }) {
                        Text(text = ReminderCalculator.format(hour, minute), style = MaterialTheme.typography.titleMedium)
                    }
                }

                PhotoReminderCalculator.status(schedule, lastPhotoDay, prefs.getAnchorDay(), LocalDate.now().toEpochDay())
                    ?.let { due ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dueText(due),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (due.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                if (!notificationsOn) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tr("Benachrichtigungen sind für Forge ausgeschaltet. ", "Notifications are turned off for Forge. ") +
                                tr("Erlaube sie in den Android-Einstellungen.", "Allow them in the Android settings."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            hour = hour,
            minute = minute,
            onConfirm = { h, m ->
                hour = h
                minute = m
                prefs.setTime(h, m)
                PhotoReminderScheduler.schedule(context)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
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
                title = tr("Signalton am Pausenende", "Sound when rest ends"),
                description = tr("Zwei Töne in der Medien-Lautstärke deines Handys.", "Two beeps at your phone's media volume."),
                checked = sound,
                onChange = {
                    sound = it
                    prefs.setSoundEnabled(it)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SwitchLine(
                title = tr("Vibration am Pausenende", "Vibrate when rest ends"),
                description = tr("Zweimal kurz, auch bei gesperrtem Bildschirm.", "Two short buzzes, even with the screen locked."),
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
                ) { Text(tr("Signal testen", "Test signal")) }
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

    val timeFormat = remember { DateTimeFormatter.ofPattern(tr("dd.MM. HH:mm", "dd/MM HH:mm")) }

    ForgeCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            SwitchLine(
                title = tr("Im Hintergrund zählen", "Count in the background"),
                description = tr("Zeigt eine kleine, stumme Benachrichtigung mit deinen Schritten. ", "Shows a small, silent notification with your steps. ") +
                        tr("Nur so liefert Android den Schrittzähler auch bei gesperrtem Handy zuverlässig.", "That's the only way Android reliably delivers step counts while the phone is locked."),
                checked = background,
                onChange = {
                    background = it
                    prefs.setBackgroundCountingEnabled(it)
                    StepTracking.start(context)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileLine(tr("Zähl-Dienst", "Counting service"), if (serviceRunning) tr("läuft", "running") else tr("aus", "off"))
            ProfileLine(tr("Schrittzähler-Sensor", "Step counter sensor"), if (hasSensor) tr("vorhanden", "available") else tr("fehlt", "missing"))
            ProfileLine(tr("Berechtigung Aktivität", "Activity permission"), if (hasPermission) tr("erteilt", "granted") else tr("fehlt", "missing"))
            ProfileLine(tr("Akku-Optimierung", "Battery optimization"), if (ignoresBattery) tr("ausgenommen", "exempt") else tr("aktiv", "active"))

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
                    ) { Text(tr("Akku-Ausnahme", "Battery exemption")) }
                }

                TextButton(onClick = { StepTracking.restart(context) }) { Text(tr("Neu starten", "Restart")) }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showReset = true }) {
                    Text(tr("Schrittdaten zurücksetzen", "Reset step data"), color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tr("Letzte Messwerte", "Latest readings"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (recent.isEmpty()) {
                Text(
                    text = tr("Noch kein Messwert.", "No readings yet."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                recent.forEach { sample ->
                    val time = Instant.ofEpochMilli(sample.timeMillis).atZone(ZoneId.systemDefault())
                        .format(timeFormat)
                    Text(
                        text = tr("$time \u00B7 Rohwert ${sample.total}", "$time · raw value ${sample.total}"),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    if (showReset) {
        ConfirmDialog(
            title = tr("Schrittdaten zurücksetzen?", "Reset step data?"),
            message = tr("Alle Tageswerte und Messwerte der Schritte werden gelöscht. Danach zählt die App von vorn.", "All daily step totals and readings will be deleted. The app then starts counting from scratch."),
            confirmLabel = tr("Zurücksetzen", "Reset"),
            onConfirm = {
                scope.launch { repository.resetAll() }
                showReset = false
            },
            onDismiss = { showReset = false }
        )
    }
}
