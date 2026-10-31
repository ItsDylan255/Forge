package com.example.fitnesstracker.watch

import android.content.Context
import android.media.AudioAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import android.content.ComponentName
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Empfängt den Stand vom Handy, auch wenn die App auf der Uhr nicht offen ist:
 * Training (Anzeige und Hinweis auf dem Zifferblatt), Tageswerte (Kachel und Komplikation)
 * und das Pausenende (Vibration).
 */
class WatchListenerService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val item = event.dataItem
            val map = DataMapItem.fromDataItem(item).dataMap
            when (item.uri.path) {
                WearProtocol.PATH_WORKOUT -> applyWorkout(this, WorkoutState.from(map))
                WearProtocol.PATH_TODAY -> applyToday(this, TodayState.from(map))
            }
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == WearProtocol.PATH_REST_OVER) {
            vibrateRestOver(this)
            // Hinweis auf dem Zifferblatt von der Pause zurück auf die Trainingsdauer stellen
            WatchStore.workout.value?.let { WorkoutOngoing.update(this, it) }
        }
    }

    companion object {

        fun applyWorkout(context: Context, state: WorkoutState?) {
            WatchStore.setWorkout(state)
            WorkoutOngoing.update(context, state)
        }

        fun applyToday(context: Context, today: TodayState) {
            TodayStore(context).save(today)
            TileService.getUpdater(context).requestUpdate(ForgeTileService::class.java)
            ComplicationDataSourceUpdateRequester
                .create(context, ComponentName(context, StepsComplicationService::class.java))
                .requestUpdateAll()
        }

        /** Zweimal kräftig, als Alarm gekennzeichnet, damit es auch bei „Nicht stören“ ankommt. */
        fun vibrateRestOver(context: Context) {
            val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator ?: return
            if (!vibrator.hasVibrator()) return
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0L, 400L, 200L, 400L), -1),
                attributes
            )
        }
    }
}
