package com.example.fitnesstracker.ui.photos

import android.content.ActivityNotFoundException
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnesstracker.data.local.ProgressPhoto
import com.example.fitnesstracker.data.photos.PhotoStorage
import com.example.fitnesstracker.domain.food.FoodFormat
import com.example.fitnesstracker.domain.photos.PhotoDue
import com.example.fitnesstracker.domain.steps.StepFormat
import com.example.fitnesstracker.domain.weight.WeightFormat
import com.example.fitnesstracker.domain.weight.WeightInputParser
import com.example.fitnesstracker.domain.weight.WeightParseResult
import com.example.fitnesstracker.i18n.L10n
import com.example.fitnesstracker.i18n.tr
import com.example.fitnesstracker.ui.gym.ConfirmDialog
import com.example.fitnesstracker.ui.gym.EmptyHint
import com.example.fitnesstracker.ui.gym.GroupLabel
import com.example.fitnesstracker.ui.gym.GymHeader
import com.example.fitnesstracker.ui.gym.GymPage
import com.example.fitnesstracker.ui.theme.ForgeColors
import com.example.fitnesstracker.ui.weight.ForgeCard
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Kurzes Datum für Termine und Fotos: "So, 02.11." bzw. "Sun, 02/11". */
internal fun photoDate(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern(tr("EEE, dd.MM.", "EEE, dd/MM"), L10n.locale))

/** Text zum nächsten Foto-Termin, für den Fotos-Tab und die Startseite. */
internal fun dueText(due: PhotoDue): String = when {
    due.isOverdue -> if (due.daysOverdue == 1L) {
        tr("Fortschrittsfoto überfällig seit gestern", "Progress photo overdue since yesterday")
    } else {
        tr("Fortschrittsfoto seit ${due.daysOverdue} Tagen überfällig", "Progress photo overdue by ${due.daysOverdue} days")
    }
    due.isToday -> tr("Heute ist Foto-Tag", "Photo day is today")
    due.dueEpochDay == due.today + 1 -> tr("Nächstes Foto: morgen", "Next photo: tomorrow")
    else -> tr("Nächstes Foto: ${photoDate(due.dueEpochDay)}", "Next photo: ${photoDate(due.dueEpochDay)}")
}

/** Leicht rote Fläche für einen überfälligen Termin. */
internal val OverdueBackground = Color(0xFF3A2224)

/**
 * Inhalt des Fotos-Tabs im Gym: Foto aufnehmen oder aus der Galerie wählen, darunter alle Fotos.
 * Ohne Gewicht von heute wird vor dem Foto danach gefragt, damit jedes Foto ein Gewicht hat.
 */
@Composable
internal fun PhotosTab(viewModel: PhotoViewModel, onOpen: (Long) -> Unit) {
    val context = LocalContext.current
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val hasWeightToday by viewModel.hasWeightToday.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    // Wohin es nach der Gewichtsabfrage weitergeht: "camera" oder "gallery"
    var pendingSource by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraFile by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = cameraFile?.let(::File)
        if (success && file != null) {
            viewModel.addPhoto(Uri.fromFile(file)) { file.delete() }
        } else {
            file?.delete()
        }
        cameraFile = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.addPhoto(it) }
    }

    fun launch(source: String) {
        if (source == "camera") {
            val (file, uri) = PhotoStorage.newCameraTarget(context)
            cameraFile = file.absolutePath
            try {
                cameraLauncher.launch(uri)
            } catch (_: ActivityNotFoundException) {
                file.delete()
                cameraFile = null
            }
        } else {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    fun start(source: String) {
        if (hasWeightToday) launch(source) else pendingSource = source
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = { start("camera") }, enabled = !saving, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(tr("Kamera", "Camera"))
        }
        OutlinedButton(onClick = { start("gallery") }, enabled = !saving, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(tr("Galerie", "Gallery"))
        }
    }

    if (saving) {
        Spacer(Modifier.height(8.dp))
        EmptyHint(tr("Foto wird gespeichert …", "Saving photo …"))
    }

    viewModel.due(photos.maxOfOrNull { it.dateEpochDay })?.let { due ->
        Spacer(Modifier.height(12.dp))
        Text(
            text = dueText(due),
            style = MaterialTheme.typography.bodyMedium,
            color = if (due.isOverdue) ForgeColors.Danger else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (photos.isEmpty()) {
        Spacer(Modifier.height(16.dp))
        EmptyHint(
            tr(
                "Noch keine Fotos. Mit jedem Foto werden Datum, Gewicht und deine Werte von diesem Tag gespeichert. " +
                    "Wie oft du erinnert wirst, stellst du in den Einstellungen ein.",
                "No photos yet. Each photo saves the date, your weight and your stats from that day. " +
                    "Set how often you get reminded in Settings."
            )
        )
    } else {
        GroupLabel(tr("Deine Fotos", "Your photos"))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            photos.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { photo ->
                        PhotoTile(photo = photo, modifier = Modifier.weight(1f), onClick = { onOpen(photo.id) })
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }

    pendingSource?.let { source ->
        TodayWeightDialog(
            onSave = { kg ->
                viewModel.saveTodayWeight(kg) {
                    pendingSource = null
                    launch(source)
                }
            },
            onDismiss = { pendingSource = null }
        )
    }

    error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } }
        )
    }
}

@Composable
private fun PhotoTile(photo: ProgressPhoto, modifier: Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val bitmap = rememberPhotoBitmap(PhotoStorage.file(context, photo.fileName), maxSide = 480)
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .clip(shape)
            .background(ForgeColors.Card)
            .border(1.dp, ForgeColors.Outline, shape)
            .clickable(onClick = onClick)
    ) {
        bitmap?.let {
            Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(photoDate(photo.dateEpochDay), style = MaterialTheme.typography.labelSmall, color = Color.White)
            photo.weightKg?.let {
                Text(WeightFormat.weight(it), style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Ein Foto groß, mit den Werten vom Tag der Aufnahme und dem Vergleich zum vorherigen und ersten Foto. */
@Composable
internal fun PhotoDetailScreen(viewModel: PhotoViewModel, photoId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val photo = photos.firstOrNull { it.id == photoId }
    if (photo == null) {
        GymPage { GymHeader(title = tr("Foto", "Photo"), onBack = onBack) }
        return
    }

    // Liste ist neueste zuerst: das vorherige Foto steht dahinter, das erste ganz am Ende
    val index = photos.indexOf(photo)
    val previous = photos.getOrNull(index + 1)
    val first = photos.lastOrNull()?.takeIf { it.id != photo.id }

    GymPage {
        GymHeader(title = photoDate(photo.dateEpochDay), onBack = onBack)
        Spacer(Modifier.height(16.dp))

        val bitmap = rememberPhotoBitmap(PhotoStorage.file(context, photo.fileName), maxSide = 1600)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ForgeColors.Card),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = tr("Fortschrittsfoto", "Progress photo"),
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(Modifier.aspectRatio(3f / 4f))
            }
        }

        GroupLabel(tr("Werte an diesem Tag", "Stats on this day"))
        ForgeCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                StatLine(tr("Gewicht", "Weight"), photo.weightKg?.let { WeightFormat.weight(it) } ?: "–")
                change(photo, previous)?.let { StatLine(tr("Seit dem vorherigen Foto", "Since previous photo"), it) }
                change(photo, first)?.let { StatLine(tr("Seit dem ersten Foto", "Since first photo"), it) }
                photo.goalWeightKg?.let { StatLine(tr("Zielgewicht", "Target weight"), WeightFormat.weight(it)) }
                StatLine(tr("Trainings gesamt", "Workouts total"), photo.workoutsTotal.toString())
                StatLine(tr("Trainings (30 Tage)", "Workouts (30 days)"), photo.workouts30Days.toString())
                photo.avgSteps7Days?.let { StatLine(tr("Ø Schritte (7 Tage)", "Avg. steps (7 days)"), StepFormat.steps(it)) }
                photo.avgKcal7Days?.let { StatLine(tr("Ø Kalorien (7 Tage)", "Avg. calories (7 days)"), FoodFormat.kcal(it.toDouble())) }
                photo.kcalTarget?.let { StatLine(tr("Kalorienziel", "Calorie goal"), FoodFormat.kcal(it.toDouble())) }
            }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { confirmDelete = true }) {
            Text(tr("Foto löschen", "Delete photo"), color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = tr("Foto löschen?", "Delete photo?"),
            message = tr("Das Foto und seine Werte werden endgültig gelöscht.", "The photo and its stats will be deleted permanently."),
            confirmLabel = tr("Löschen", "Delete"),
            onConfirm = {
                confirmDelete = false
                viewModel.delete(photo)
                onBack()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

private fun change(photo: ProgressPhoto, other: ProgressPhoto?): String? {
    val now = photo.weightKg ?: return null
    val then = other?.weightKg ?: return null
    return WeightFormat.delta(now - then)
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 16.dp))
    }
}

/** Fragt das heutige Gewicht ab, bevor ein Foto gemacht wird. */
@Composable
private fun TodayWeightDialog(onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Gewicht von heute", "Today's weight")) },
        text = {
            Column {
                Text(
                    tr(
                        "Du hast heute noch kein Gewicht eingetragen. Es wird zusammen mit dem Foto gespeichert.",
                        "You haven't logged your weight today. It's saved together with the photo."
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    label = { Text(tr("Gewicht in kg", "Weight in kg")) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (val result = WeightInputParser.parse(input)) {
                    is WeightParseResult.Valid -> onSave(result.weightKg)
                    is WeightParseResult.Invalid -> error = result.error.message
                }
            }) { Text(tr("Weiter", "Continue")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Abbrechen", "Cancel")) } }
    )
}

// Kleine Vorschaubilder werden behalten, damit das Raster beim Scrollen nicht neu lädt
private val thumbnailCache = object : LruCache<String, ImageBitmap>(24 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4 / 1024
}

/** Lädt ein gespeichertes Foto im Hintergrund, verkleinert auf etwa [maxSide] Pixel. */
@Composable
internal fun rememberPhotoBitmap(file: File, maxSide: Int): ImageBitmap? {
    val key = "${file.name}@$maxSide"
    val cacheable = maxSide <= 600
    val state = produceState(initialValue = if (cacheable) thumbnailCache.get(key) else null, key) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { decode(file, maxSide) }
                ?.also { if (cacheable) thumbnailCache.put(key, it) }
        }
    }
    return state.value
}

private fun decode(file: File, maxSide: Int): ImageBitmap? {
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
        ?.asImageBitmap()
}
