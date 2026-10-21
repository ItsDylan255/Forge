package com.example.fitnesstracker.ui.settings

import android.Manifest
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.fitnesstracker.data.local.ReminderPreferences
import com.example.fitnesstracker.domain.calories.CalorieProfile
import com.example.fitnesstracker.domain.reminder.ReminderCalculator
import com.example.fitnesstracker.service.WeightReminderScheduler
import com.example.fitnesstracker.domain.calories.Sex
import com.example.fitnesstracker.ui.calories.CalorieGoalCard
import com.example.fitnesstracker.ui.calories.CalorieProfileDialog
import com.example.fitnesstracker.ui.calories.CalorieViewModel
import com.example.fitnesstracker.ui.calories.activityLabel
import com.example.fitnesstracker.ui.calories.sexLabel
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Stunde in 1er-, Minute in 5er-Schritten
                        TextButton(onClick = { save(true, ReminderCalculator.stepHour(hour, -1), minute) }) {
                            Text("\u2212")
                        }
                        Text(
                            text = ReminderCalculator.format(hour, minute),
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = { save(true, ReminderCalculator.stepHour(hour, 1), minute) }) {
                            Text("+")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Minuten",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { save(true, hour, ReminderCalculator.stepMinute(minute, -5)) }) {
                        Text("\u2212")
                    }
                    TextButton(onClick = { save(true, hour, ReminderCalculator.stepMinute(minute, 5)) }) {
                        Text("+")
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
}
