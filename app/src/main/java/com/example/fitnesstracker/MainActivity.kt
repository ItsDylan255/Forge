
package com.example.fitnesstracker

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnesstracker.domain.food.FoodCalculator
import com.example.fitnesstracker.domain.food.FoodFormat
import com.example.fitnesstracker.domain.food.toData
import com.example.fitnesstracker.ui.calories.CalorieViewModel
import java.time.LocalDate
import com.example.fitnesstracker.service.StepSyncScheduler
import com.example.fitnesstracker.service.WeightReminderScheduler
import com.example.fitnesstracker.ui.food.FoodScreen
import com.example.fitnesstracker.ui.home.HomeScreen
import com.example.fitnesstracker.ui.settings.SettingsScreen
import com.example.fitnesstracker.ui.food.FoodViewModel
import com.example.fitnesstracker.ui.gym.GymScreen
import com.example.fitnesstracker.ui.gym.GymViewModel
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
    val label: String,
    val icon: ImageVector
)

private val destinations = listOf(
    AppDestination("home", "Home", Icons.Filled.Home),
    AppDestination("gym", "Gym", Icons.Filled.FitnessCenter),
    AppDestination("food", "Essen", Icons.Filled.Restaurant),
    AppDestination("steps", "Schritte", Icons.AutoMirrored.Filled.DirectionsWalk),
    AppDestination("weight", "Gewicht", Icons.Filled.MonitorWeight)
)

class MainActivity : ComponentActivity() {

    // Ziel, das eine Benachrichtigung öffnen soll (zum Beispiel "weight")
    private var openRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nach einer Drehung nicht noch einmal zum Ziel der Benachrichtigung springen
        if (savedInstanceState == null) {
            openRoute = intent?.getStringExtra(EXTRA_OPEN_ROUTE)
        }
        // App ist immer dunkel: Statusleiste/Navigationsleiste mit hellen Icons
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // Schritte auch im Hintergrund zählen (alle ca. 15 Minuten)
        StepSyncScheduler.schedule(applicationContext)

        // Gewicht-Erinnerung (falls eingeschaltet) für den nächsten Termin planen
        WeightReminderScheduler.schedule(applicationContext)

        setContent {
            FitnessTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppBackground
                ) {
                    FitnessTrackerApp(
                        openRoute = openRoute,
                        onRouteHandled = { openRoute = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openRoute = intent.getStringExtra(EXTRA_OPEN_ROUTE)
    }

    companion object {
        const val EXTRA_OPEN_ROUTE = "open_route"
    }
}

@Composable
fun FitnessTrackerApp(
    openRoute: String? = null,
    onRouteHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Gleiche Navigation wie die Leiste unten, auch für die Karten auf der Startseite
    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Eine Benachrichtigung kann direkt einen Tab öffnen
    LaunchedEffect(openRoute) {
        openRoute?.let { route ->
            navigateTo(route)
            onRouteHandled()
        }
    }

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
                        icon = { Icon(imageVector = destination.icon, contentDescription = null) },
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
                HomeScreen(
                    onNavigate = navigateTo,
                    onOpenSettings = { navController.navigate("settings") }
                )
            }

            composable("gym") {
                val gymViewModel: GymViewModel = viewModel()

                GymScreen(
                    viewModel = gymViewModel
                )
            }

            composable("food") {
                val foodViewModel: FoodViewModel = viewModel()

                FoodScreen(
                    viewModel = foodViewModel,
                    onOpenSettings = { navController.navigate("settings") }
                )
            }

            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
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