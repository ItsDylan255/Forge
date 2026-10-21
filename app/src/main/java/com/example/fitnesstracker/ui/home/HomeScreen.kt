package com.example.fitnesstracker.ui.home

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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnesstracker.domain.food.FoodCalculator
import com.example.fitnesstracker.domain.food.FoodFormat
import com.example.fitnesstracker.domain.food.toData
import com.example.fitnesstracker.domain.steps.StepCalculator
import com.example.fitnesstracker.domain.steps.StepFormat
import com.example.fitnesstracker.domain.weight.GoalDirection
import com.example.fitnesstracker.domain.weight.WeightFormat
import com.example.fitnesstracker.domain.weight.WeightGoalCalculator
import com.example.fitnesstracker.ui.calories.CalorieViewModel
import com.example.fitnesstracker.ui.food.FoodViewModel
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
        latest == null -> "Noch kein Gewicht eingetragen"
        goalProgress != null -> {
            val base = when (goalProgress.direction) {
                GoalDirection.REACHED -> "Ziel erreicht"
                GoalDirection.LOSE -> "Noch ${WeightFormat.weight(goalProgress.remainingKg)} abnehmen"
                GoalDirection.GAIN -> "Noch ${WeightFormat.weight(goalProgress.remainingKg)} zunehmen"
            }
            val percent = goalProgress.progressFraction?.let { " \u00B7 ${(it * 100).toInt()} %" } ?: ""
            base + percent
        }
        previous != null -> "\u00C4nderung: ${WeightFormat.delta(latest.weightKg - previous.weightKg)}"
        else -> "Noch kein Ziel gesetzt"
    }
    val weightRight = weightGoal?.let { "Ziel: ${WeightFormat.weight(it.targetWeightKg)}" } ?: "Ziel: -- kg"

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
                Text(text = "\u00DCbersicht", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Einstellungen",
                    tint = SecondaryText
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "HEUTE", color = SecondaryText, fontSize = 12.sp, letterSpacing = 1.5.sp)

            TrackerCard(
                title = "Schritte",
                value = StepFormat.steps(stepProgress.steps),
                detail = "von ${StepFormat.steps(stepProgress.goal)} Schritten",
                progress = StepFormat.percent(stepProgress.percent),
                onClick = { onNavigate("steps") }
            )

            TrackerCard(
                title = "Kalorien",
                value = FoodFormat.kcal(kcalToday),
                detail = if (kcalGoal != null) "von ${FoodFormat.kcal(kcalGoal.toDouble())}"
                else "Noch kein Kalorienziel",
                progress = FoodCalculator.goalPercent(kcalToday, kcalGoal)
                    ?.let { FoodFormat.percent(it) } ?: "",
                onClick = { onNavigate("food") }
            )

            TrackerCard(
                title = "K\u00F6rpergewicht",
                value = weightValue,
                detail = weightDetail,
                progress = weightRight,
                onClick = { onNavigate("weight") }
            )
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
