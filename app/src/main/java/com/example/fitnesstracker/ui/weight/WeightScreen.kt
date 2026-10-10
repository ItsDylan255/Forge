
package com.example.fitnesstracker.ui.weight

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeightScreen(
    viewModel: WeightViewModel
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val goal by viewModel.goal.collectAsStateWithLifecycle()

    var isEditing by rememberSaveable { mutableStateOf(false) }
    var weightInput by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isEditingGoal by rememberSaveable { mutableStateOf(false) }
    var goalInput by rememberSaveable { mutableStateOf("") }
    var goalError by rememberSaveable { mutableStateOf<String?>(null) }

    val today = LocalDate.now()

    val todayEntry = entries.firstOrNull {
        it.dateEpochDay == today.toEpochDay()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Gewicht",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Karte für das heutige Gewicht
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Heutiges Gewicht",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = todayEntry?.let {
                        String.format(
                            Locale.GERMANY,
                            "%.1f kg",
                            it.weightKg
                        )
                    } ?: "Noch nicht eingetragen",
                    style = MaterialTheme.typography.headlineSmall
                )

                if (isEditing) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = {
                            weightInput = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Gewicht in kg")
                        },
                        placeholder = {
                            Text("z. B. 95,4")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val parsedWeight = weightInput
                                .trim()
                                .replace(',', '.')
                                .toDoubleOrNull()

                            if (
                                parsedWeight == null ||
                                !parsedWeight.isFinite() ||
                                parsedWeight <= 0.0
                            ) {
                                errorMessage =
                                    "Bitte gib ein gültiges Gewicht ein."
                            } else {
                                viewModel.saveWeight(parsedWeight)

                                // Eingabeformular schließen
                                isEditing = false
                                weightInput = ""
                                errorMessage = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Speichern")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            isEditing = false
                            weightInput = ""
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Abbrechen")
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            weightInput =
                                todayEntry?.weightKg?.toString() ?: ""
                            errorMessage = null
                            isEditing = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (todayEntry == null) {
                                "Gewicht eintragen"
                            } else {
                                "Gewicht korrigieren"
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Zielgewicht
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Mein Zielgewicht",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = goal?.let {
                        String.format(
                            Locale.GERMANY,
                            "%.1f kg",
                            it.targetWeightKg
                        )
                    } ?: "Noch kein Ziel gesetzt",
                    style = MaterialTheme.typography.headlineSmall
                )

                if (isEditingGoal) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = {
                            goalInput = it
                            goalError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Zielgewicht in kg") },
                        placeholder = { Text("z. B. 85,0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )

                    if (goalError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = goalError!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val parsed = goalInput
                                .trim()
                                .replace(',', '.')
                                .toDoubleOrNull()

                            if (
                                parsed == null ||
                                !parsed.isFinite() ||
                                parsed <= 0.0
                            ) {
                                goalError = "Bitte gib ein gültiges Zielgewicht ein."
                            } else {
                                viewModel.saveGoal(parsed)
                                isEditingGoal = false
                                goalInput = ""
                                goalError = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ziel speichern")
                    }

                    Button(
                        onClick = {
                            isEditingGoal = false
                            goalInput = ""
                            goalError = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Abbrechen")
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            goalInput = goal?.targetWeightKg?.toString() ?: ""
                            goalError = null
                            isEditingGoal = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (goal == null) {
                                "Zielgewicht festlegen"
                            } else {
                                "Zielgewicht ändern"
                            }
                        )
                    }

                    if (goal != null) {
                        Button(
                            onClick = {
                                viewModel.deleteGoal()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ziel entfernen")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        // Gewichtsverlauf
        Text(
            text = "Gewichtsverlauf",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (entries.isEmpty()) {
            Text(
                text = "Hier erscheinen deine gespeicherten Messungen.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                entries
                    .sortedByDescending { it.dateEpochDay }
                    .forEach { entry ->
                        val date = LocalDate.ofEpochDay(
                            entry.dateEpochDay
                        )

                        val formattedDate = date.format(
                            DateTimeFormatter.ofPattern(
                                "dd.MM.yyyy",
                                Locale.GERMANY
                            )
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formattedDate
                                )

                                Text(
                                    text = String.format(
                                        Locale.GERMANY,
                                        "%.1f kg",
                                        entry.weightKg
                                    ),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}