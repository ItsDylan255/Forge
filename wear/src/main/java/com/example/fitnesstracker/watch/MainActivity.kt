package com.example.fitnesstracker.watch

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Die Forge-Farben, passend zur Handy-App. */
private val ForgeOrange = Color(0xFFFF7A1A)

private val forgeColors = Colors(
    primary = ForgeOrange,
    primaryVariant = Color(0xFFC85A00),
    secondary = ForgeOrange,
    onPrimary = Color.Black,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0)
)

/**
 * Begleiter fürs Training: zeigt, was am Handy läuft. Eingaben gibt es hier nur für die Pause
 * (verlängern oder überspringen), alles andere passiert am Handy.
 */
class MainActivity : ComponentActivity() {

    private val dataListener = DataClient.OnDataChangedListener { events ->
        events.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            when (event.dataItem.uri.path) {
                WearProtocol.PATH_WORKOUT -> WatchListenerService.applyWorkout(this, WorkoutState.from(map))
                WearProtocol.PATH_TODAY -> WatchListenerService.applyToday(this, TodayState.from(map))
            }
        }
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            WatchStore.workout.value?.let { WorkoutOngoing.update(this, it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MaterialTheme(colors = forgeColors) {
                ForgeWatchApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val client = Wearable.getDataClient(this)
        client.addListener(dataListener)

        // Den letzten Stand holen, falls die Uhr in der Zwischenzeit nichts empfangen hat
        client.getDataItems(Uri.parse("wear://*${WearProtocol.PATH_WORKOUT}")).addOnSuccessListener { items ->
            val item = items.firstOrNull()
            WatchListenerService.applyWorkout(this, item?.let { WorkoutState.from(DataMapItem.fromDataItem(it).dataMap) })
            items.release()
        }
        client.getDataItems(Uri.parse("wear://*${WearProtocol.PATH_TODAY}")).addOnSuccessListener { items ->
            items.firstOrNull()?.let { WatchListenerService.applyToday(this, TodayState.from(DataMapItem.fromDataItem(it).dataMap)) }
            items.release()
        }
    }

    override fun onStop() {
        Wearable.getDataClient(this).removeListener(dataListener)
        super.onStop()
    }
}

@Composable
private fun ForgeWatchApp() {
    val workout by WatchStore.workout.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }

    val current = workout
    Scaffold(timeText = { TimeText() }) {
        when {
            current == null -> IdleScreen()
            current.restRunning(now) -> RestScreen(current, now)
            else -> WorkoutScreen(current, now)
        }
    }
}

@Composable
private fun IdleScreen() {
    val context = LocalContext.current
    val today = remember { TodayStore(context).load() }
    val isToday = today?.dateEpochDay == LocalDate.now().toEpochDay()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Forge", style = MaterialTheme.typography.title2, color = MaterialTheme.colors.primary)
        Spacer(Modifier.height(6.dp))
        if (today != null && isToday) {
            Text("${WatchFormat.steps(today.steps)} / ${WatchFormat.steps(today.stepGoal)} Schritte",
                style = MaterialTheme.typography.body2, textAlign = TextAlign.Center)
            today.kcal?.let { kcal ->
                val target = today.kcalTarget?.let { " / $it" }.orEmpty()
                Text("$kcal$target kcal", style = MaterialTheme.typography.body2)
            }
            today.weightKg?.let { Text(WatchFormat.kg(it), style = MaterialTheme.typography.body2) }
            Spacer(Modifier.height(8.dp))
        }
        Text(
            "Starte ein Training am Handy.",
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WorkoutScreen(state: WorkoutState, now: Long) {
    val listState = rememberScalingLazyListState()

    Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    "${state.routineName} · ${WatchFormat.timer(now - state.startedAt)}",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (state.exerciseName == null) {
                item {
                    Text(
                        "Füge am Handy eine Übung hinzu.",
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center
                    )
                }
                return@ScalingLazyColumn
            }
            if (state.superset) {
                item { Text("SUPERSET", style = MaterialTheme.typography.caption3, color = MaterialTheme.colors.primary) }
            }
            item {
                Text(
                    state.exerciseName,
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            state.setText?.let { item { Text(it, style = MaterialTheme.typography.title3, color = MaterialTheme.colors.primary) } }
            state.targetText?.let { item { Text(it, style = MaterialTheme.typography.body2) } }
            state.lastText?.let {
                item {
                    Text(
                        it,
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            state.recordText?.let {
                item {
                    Text(
                        it,
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            state.progressText?.let {
                item { Text(it, style = MaterialTheme.typography.caption3, color = MaterialTheme.colors.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun RestScreen(state: WorkoutState, now: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var sending by remember { mutableStateOf(false) }

    val remaining = (state.restEndsAt - now).coerceAtLeast(0L)
    val progress = if (state.restTotal > 0L) remaining.toFloat() / state.restTotal else 0f

    fun send(path: String) {
        if (sending) return
        sending = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            sendToPhone(context, path)
            sending = false
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize().padding(4.dp),
            startAngle = 0f,
            endAngle = 360f,
            strokeWidth = 6.dp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAUSE", style = MaterialTheme.typography.caption3, color = MaterialTheme.colors.onSurfaceVariant)
            Text(WatchFormat.countdown(remaining), fontSize = 40.sp, fontWeight = FontWeight.Bold)
            state.exerciseName?.let { next ->
                Text(
                    "Weiter: $next",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 28.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { send(WearProtocol.PATH_REST_EXTEND) },
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                ) { Text("+30 s", style = MaterialTheme.typography.caption2) }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { send(WearProtocol.PATH_REST_SKIP) },
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip),
                        contentDescription = "Überspringen"
                    )
                }
            }
        }
    }
}

/** Schickt einen Knopfdruck an das Handy. Ohne Verbindung passiert nichts. */
private suspend fun sendToPhone(context: android.content.Context, path: String) {
    try {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        val messages = Wearable.getMessageClient(context)
        nodes.forEach { messages.sendMessage(it.id, path, ByteArray(0)).await() }
    } catch (_: Exception) {
        // Handy nicht erreichbar
    }
}
