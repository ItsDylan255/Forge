package com.example.fitnesstracker.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnesstracker.MainActivity
import com.example.fitnesstracker.data.local.PhotoReminderPreferences
import com.example.fitnesstracker.domain.food.FoodCalculator
import com.example.fitnesstracker.domain.food.FoodFormat
import com.example.fitnesstracker.domain.food.toData
import com.example.fitnesstracker.domain.photos.PhotoDue
import com.example.fitnesstracker.domain.photos.PhotoReminderCalculator
import com.example.fitnesstracker.domain.steps.StepCalculator
import com.example.fitnesstracker.domain.steps.StepFormat
import com.example.fitnesstracker.domain.weight.GoalDirection
import com.example.fitnesstracker.domain.weight.WeightFormat
import com.example.fitnesstracker.domain.weight.WeightGoalCalculator
import com.example.fitnesstracker.i18n.tr
import com.example.fitnesstracker.ui.calories.CalorieViewModel
import com.example.fitnesstracker.ui.food.FoodViewModel
import com.example.fitnesstracker.ui.photos.OverdueBackground
import com.example.fitnesstracker.ui.photos.dueText
import com.example.fitnesstracker.ui.photos.photoDate
import com.example.fitnesstracker.ui.steps.StepsViewModel
import com.example.fitnesstracker.ui.theme.ForgeColors
import java.time.LocalDate

private val CardBackground = ForgeColors.Card
private val AccentColor = ForgeColors.Accent
private val SecondaryText = ForgeColors.SecondaryText

/**
 * Startseite mit echten Daten. Jede Karte öffnet den passenden Tab über [onNavigate].
 * Routen: "gym", "food", "steps", "weight".
 */
@Composable
fun HomeScreen(
    onNavigate: (route: String) -> Unit,
    onOpenSettings: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    calorieViewModel: CalorieViewModel = viewModel(),
    foodViewModel: FoodViewModel = viewModel(),
    stepsViewModel: StepsViewModel = viewModel()
) {
    val weightEntries by homeViewModel.weightEntries.collectAsStateWithLifecycle()
    val weightGoal by homeViewModel.weightGoal.collectAsStateWithLifecycle()
    val stepEntries by homeViewModel.stepEntries.collectAsStateWithLifecycle()
    val calorieTarget by calorieViewModel.target.collectAsStateWithLifecycle()
    val foodLog by foodViewModel.log.collectAsStateWithLifecycle()
    val stepGoal by stepsViewModel.stepGoal.collectAsStateWithLifecycle()
    val lastPhotoDay by homeViewModel.lastPhotoDay.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val photoPrefs = remember { PhotoReminderPreferences(context) }

    val today = LocalDate.now().toEpochDay()

    // ---------- Schritte ----------
    val stepsToday = stepEntries.firstOrNull { it.dateEpochDay == today }?.steps ?: 0
    val stepProgress = StepCalculator.goalProgress(stepsToday, stepGoal)

    // ---------- Gewicht ----------
    val latest = weightEntries.lastOrNull()
    val previous = weightEntries.getOrNull(weightEntries.size - 2)
    val goalProgress = if (latest != null && weightGoal != null) {
        WeightGoalCalculator.progress(
            weightGoal!!.targetWeightKg, latest.weightKg, weightGoal!!.startWeightKg
        )
    } else {
        null
    }

    val weightValue = latest?.let { WeightFormat.weight(it.weightKg) } ?: "-- kg"
    val weightDetail = when {
        latest == null -> tr("Noch kein Gewicht eingetragen", "No weight logged yet")
        goalProgress != null -> {
            val base = when (goalProgress.direction) {
                GoalDirection.REACHED -> tr("Ziel erreicht", "Goal reached")
                GoalDirection.LOSE -> tr("Noch ${WeightFormat.weight(goalProgress.remainingKg)} abnehmen", "${WeightFormat.weight(goalProgress.remainingKg)} left to lose")
                GoalDirection.GAIN -> tr("Noch ${WeightFormat.weight(goalProgress.remainingKg)} zunehmen", "${WeightFormat.weight(goalProgress.remainingKg)} left to gain")
            }
            val percent = goalProgress.progressFraction?.let { " \u00B7 ${(it * 100).toInt()} %" } ?: ""
            base + percent
        }
        previous != null -> tr("\u00C4nderung: ${WeightFormat.delta(latest.weightKg - previous.weightKg)}", "Change: ${WeightFormat.delta(latest.weightKg - previous.weightKg)}")
        else -> tr("Noch kein Ziel gesetzt", "No goal set yet")
    }
    val weightRight = weightGoal?.let { tr("Ziel: ${WeightFormat.weight(it.targetWeightKg)}", "Goal: ${WeightFormat.weight(it.targetWeightKg)}") } ?: tr("Ziel: -- kg", "Goal: -- kg")

    // ---------- Kalorien ----------
    val kcalGoal = calorieTarget?.targetKcal
    val kcalToday = FoodCalculator.dayTotals(foodLog.map { it.toData() }, today).kcal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(text = tr("\u00DCbersicht", "Overview"), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = tr("Einstellungen", "Settings"),
                    tint = SecondaryText
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = tr("HEUTE", "TODAY"), color = SecondaryText, fontSize = 12.sp, letterSpacing = 1.5.sp)

            TrackerCard(
                title = tr("Schritte", "Steps"),
                value = StepFormat.steps(stepProgress.steps),
                detail = tr("von ${StepFormat.steps(stepProgress.goal)} Schritten", "of ${StepFormat.steps(stepProgress.goal)} steps"),
                progress = StepFormat.percent(stepProgress.percent),
                onClick = { onNavigate("steps") }
            )

            TrackerCard(
                title = tr("Kalorien", "Calories"),
                value = FoodFormat.kcal(kcalToday),
                detail = if (kcalGoal != null) tr("von ${FoodFormat.kcal(kcalGoal.toDouble())}", "of ${FoodFormat.kcal(kcalGoal.toDouble())}")
                else tr("Noch kein Kalorienziel", "No calorie goal yet"),
                progress = FoodCalculator.goalPercent(kcalToday, kcalGoal)
                    ?.let { FoodFormat.percent(it) } ?: "",
                onClick = { onNavigate("food") }
            )

            TrackerCard(
                title = tr("K\u00F6rpergewicht", "Body weight"),
                value = weightValue,
                detail = weightDetail,
                progress = weightRight,
                onClick = { onNavigate("weight") }
            )
        }

        // Nur sichtbar, wenn in den Einstellungen ein Rhythmus für Fortschrittsfotos gewählt ist
        PhotoReminderCalculator.status(photoPrefs.getSchedule(), lastPhotoDay, photoPrefs.getAnchorDay(), today)
            ?.let { due ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = tr("FORTSCHRITTSFOTO", "PROGRESS PHOTO"), color = SecondaryText, fontSize = 12.sp, letterSpacing = 1.5.sp)
                    PhotoDueCard(due = due, onClick = { onNavigate(MainActivity.ROUTE_PHOTOS) })
                }
            }
    }
}

/** Nächstes Fortschrittsfoto; leicht rot, sobald der Termin verstrichen ist. */
@Composable
private fun PhotoDueCard(due: PhotoDue, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (due.isOverdue) Modifier.border(1.dp, ForgeColors.Danger.copy(alpha = 0.6f), shape) else Modifier)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (due.isOverdue) OverdueBackground else CardBackground
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = if (due.isOverdue) ForgeColors.Danger else SecondaryText
            )
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    text = dueText(due),
                    color = if (due.isOverdue) ForgeColors.Danger else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        due.isOverdue -> tr("Fällig seit ${photoDate(due.dueEpochDay)} · Tippe, um es jetzt zu machen.",
                            "Due since ${photoDate(due.dueEpochDay)} · Tap to take it now.")
                        due.isToday -> tr("Tippe, um es jetzt zu machen.", "Tap to take it now.")
                        else -> tr("Im Gym-Tab unter „Fotos“.", "In the Gym tab under “Photos”.")
                    },
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TrackerCard(
    title: String,
    value: String,
    detail: String,
    progress: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = CardBackground
    ) {
        Row(modifier = Modifier.padding(18.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = SecondaryText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(value, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(detail, color = SecondaryText, fontSize = 12.sp)
            }

            Text(
                text = progress,
                color = AccentColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp, top = 24.dp)
            )
        }
    }
}
