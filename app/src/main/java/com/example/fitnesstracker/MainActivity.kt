
package com.example.fitnesstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitnesstracker.ui.theme.FitnessTrackerTheme
import com.example.fitnesstracker.ui.theme.ForgeColors
import androidx.activity.SystemBarStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnesstracker.ui.steps.StepsScreen
import com.example.fitnesstracker.ui.steps.StepsViewModel
import com.example.fitnesstracker.ui.weight.WeightScreen
import com.example.fitnesstracker.ui.weight.WeightViewModel

// Farben kommen zentral aus ui/theme/Theme.kt (ForgeColors)
private val AppBackground = ForgeColors.Background
private val CardBackground = ForgeColors.Card
private val AccentColor = ForgeColors.Accent
private val SecondaryText = ForgeColors.SecondaryText

private data class AppDestination(
    val route: String,
    val label: String
)

private val destinations = listOf(
    AppDestination("home", "Home"),
    AppDestination("gym", "Gym"),
    AppDestination("steps", "Schritte"),
    AppDestination("weight", "Gewicht")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App ist immer dunkel: Statusleiste/Navigationsleiste mit hellen Icons
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            FitnessTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppBackground
                ) {
                    FitnessTrackerApp()
                }
            }
        }
    }
}

@Composable
fun FitnessTrackerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            NavigationBar(
                containerColor = CardBackground,
                contentColor = Color.White
            ) {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(
                                    navController.graph.findStartDestination().id
                                ) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {},
                        label = {
                            Text(destination.label)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentColor,
                            selectedTextColor = Color.White,
                            indicatorColor = ForgeColors.CardElevated,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen()
            }

            composable("gym") {
                PlaceholderScreen(
                    title = "Gym",
                    description = "Erstelle Trainingsroutinen und dokumentiere deine Workouts."
                )
            }

            composable("steps") {
                val stepsViewModel: StepsViewModel = viewModel()

                StepsScreen(
                    viewModel = stepsViewModel
                )
            }

            composable("weight") {
                val weightViewModel: WeightViewModel = viewModel()

                WeightScreen(
                    viewModel = weightViewModel
                )
            }
        }
    }
}

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Column {
            Text(
                text = "DEIN TRAINING",
                color = SecondaryText,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Übersicht",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "HEUTE",
                color = SecondaryText,
                fontSize = 12.sp,
                letterSpacing = 1.5.sp
            )

            TrackerCard(
                title = "Schritte",
                value = "0",
                detail = "von 10.000 Schritten",
                progress = "0 %"
            )

            TrackerCard(
                title = "Körpergewicht",
                value = "-- kg",
                detail = "Noch kein Gewicht eingetragen",
                progress = "Ziel: -- kg"
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "TRAINING",
                color = SecondaryText,
                fontSize = 12.sp,
                letterSpacing = 1.5.sp
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = CardBackground
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Dein nächstes Workout",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Erstelle deine erste Trainingsroutine.",
                        color = SecondaryText,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TrackerCard(
    title: String,
    value: String,
    detail: String,
    progress: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardBackground
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(18.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = SecondaryText, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    value,
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )

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

@Composable
fun PlaceholderScreen(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = description,
            color = SecondaryText,
            style = MaterialTheme.typography.bodyLarge
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = CardBackground
        ) {
            Text(
                text = "Dieser Bereich wird als Nächstes eingerichtet.",
                color = AccentColor,
                modifier = Modifier.padding(18.dp)
            )
        }
    }
}