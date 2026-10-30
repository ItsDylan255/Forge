package com.example.fitnesstracker.data.export

import android.content.Context
import android.net.Uri
import com.example.fitnesstracker.data.local.DatabaseProvider
import com.example.fitnesstracker.domain.export.DataExport
import com.example.fitnesstracker.domain.export.ExportData
import com.example.fitnesstracker.domain.export.FoodExportRow
import com.example.fitnesstracker.domain.export.SessionExportRow
import com.example.fitnesstracker.domain.gym.toData
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Schreibt alle Daten als ZIP mit CSV-Dateien in die vom Nutzer gewählte Datei. */
object DataExporter {

    suspend fun load(context: Context): ExportData {
        val db = DatabaseProvider.getDatabase(context)
        val gym = db.gymDao()

        // Laufende Einheiten sind noch nicht fertig und gehören nicht in den Export
        val sessions = gym.observeSessions().first().mapNotNull { s ->
            s.endedAt?.let { SessionExportRow(s.id, s.routineName, s.dateEpochDay, s.startedAt, it, s.planName) }
        }

        return ExportData(
            weights = db.weightEntryDao().getAllEntries().first().map { it.dateEpochDay to it.weightKg },
            steps = db.stepEntryDao().getAll().map { it.dateEpochDay to it.steps },
            food = db.foodDao().observeLog().first().map {
                FoodExportRow(
                    dateEpochDay = it.dateEpochDay,
                    name = it.foodName,
                    grams = it.grams,
                    servingNote = it.servingNote,
                    calories = it.calories,
                    protein = it.protein,
                    carbs = it.carbs,
                    fat = it.fat
                )
            },
            sessions = sessions,
            sets = gym.observeSets().first().map { it.toData() }
        )
    }

    /** Gibt null zurück, wenn alles geklappt hat, sonst eine Fehlermeldung. */
    suspend fun writeZip(context: Context, target: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val files = DataExport.files(load(context), ZoneId.systemDefault(), LocalDate.now())
            val stream = context.contentResolver.openOutputStream(target)
                ?: return@withContext "Die Datei konnte nicht geöffnet werden."

            ZipOutputStream(stream).use { zip ->
                files.forEach { (name, content) ->
                    zip.putNextEntry(ZipEntry(name))
                    // BOM, damit Excel die Umlaute richtig erkennt
                    if (name.endsWith(".csv")) zip.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    zip.write(content.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
            null
        } catch (e: Exception) {
            "Export fehlgeschlagen: ${e.message ?: e.javaClass.simpleName}"
        }
    }
}
