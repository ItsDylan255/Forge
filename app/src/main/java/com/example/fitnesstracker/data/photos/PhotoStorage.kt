package com.example.fitnesstracker.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Bilder der Fortschrittsfotos im privaten App-Speicher. Jedes Bild wird beim Speichern verkleinert
 * (längste Seite höchstens [MAX_SIDE] px), richtig gedreht und als JPEG ohne Metadaten abgelegt,
 * damit z. B. kein Aufnahmeort mitgespeichert wird.
 */
object PhotoStorage {

    private const val DIR = "progress_photos"
    private const val MAX_SIDE = 2048
    private const val QUALITY = 90

    fun file(context: Context, fileName: String): File = File(dir(context), fileName)

    /** Leere Datei im Cache, in die die Kamera schreibt, und die passende Adresse für die Kamera-App. */
    fun newCameraTarget(context: Context): Pair<File, Uri> {
        val folder = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(folder, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }

    /** Liest ein Bild (Kamera oder Galerie), verkleinert es und speichert es. Gibt den Dateinamen zurück. */
    @Throws(IOException::class)
    fun import(context: Context, source: Uri): String {
        val resolver = context.contentResolver

        // Nur die Größe lesen (liefert absichtlich kein Bitmap zurück)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val stream = resolver.openInputStream(source) ?: throw IOException("Bild nicht lesbar")
        stream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Kein Bild")

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2

        val decoded = resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IOException("Bild nicht lesbar")

        val rotation = resolver.openInputStream(source)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val scaled = scaleDown(decoded)
        val upright = if (rotation == 0f) scaled else {
            Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, Matrix().apply { postRotate(rotation) }, true)
        }

        val name = "photo_${System.currentTimeMillis()}.jpg"
        FileOutputStream(file(context, name)).use { upright.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        return name
    }

    fun delete(context: Context, fileName: String) {
        file(context, fileName).delete()
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_SIDE) return bitmap
        val factor = MAX_SIDE.toFloat() / longest
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * factor).toInt(), (bitmap.height * factor).toInt(), true)
    }

    private fun dir(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }
}
