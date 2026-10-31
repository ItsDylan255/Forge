package com.example.fitnesstracker.watch

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.CircularProgressIndicator
import androidx.wear.protolayout.material.ProgressIndicatorColors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.EdgeContentLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import java.time.LocalDate

/**
 * Kachel mit dem heutigen Tag: Schritte als Ring, dazu Gewicht und Kalorien.
 * Die Werte kommen vom Handy und liegen im [TodayStore].
 */
class ForgeTileService : TileService() {

    override fun onCreate() {
        super.onCreate()
        WatchLang.init(this)
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(30 * 60 * 1000L)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(requestParams)))
            .build()
        return CallbackToFutureAdapter.getFuture { it.set(tile); "tile" }
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
        return CallbackToFutureAdapter.getFuture { it.set(resources); "resources" }
    }

    private fun layout(request: RequestBuilders.TileRequest): LayoutElementBuilders.LayoutElement {
        val device = request.deviceConfiguration
        val today = TodayStore(this).load()?.takeIf { it.dateEpochDay == LocalDate.now().toEpochDay() }

        val progress = today?.let { (it.steps.toFloat() / it.stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f) } ?: 0f
        val details = today?.let {
            listOfNotNull(
                it.weightKg?.let { kg -> WatchFormat.kg(kg) },
                it.kcal?.let { kcal -> "${WatchFormat.steps(kcal)} kcal" }
            ).joinToString(" · ")
        }.orEmpty()

        val open = ModifiersBuilders.Clickable.Builder()
            .setId("open")
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(packageName)
                            .setClassName(MainActivity::class.java.name)
                            .build()
                    )
                    .build()
            )
            .build()

        val content = LayoutElementBuilders.Column.Builder()
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(open).build())
            .addContent(
                Text.Builder(this, today?.let { WatchFormat.steps(it.steps) } ?: "–")
                    .setTypography(Typography.TYPOGRAPHY_DISPLAY2)
                    .setColor(argb(WHITE))
                    .build()
            )
            .addContent(
                Text.Builder(this, today?.let { tr("von ${WatchFormat.steps(it.stepGoal)}", "of ${WatchFormat.steps(it.stepGoal)}") }
                    ?: tr("Öffne Forge am Handy", "Open Forge on your phone"))
                    .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                    .setColor(argb(GREY))
                    .build()
            )
            .build()

        return EdgeContentLayout.Builder(device)
            .setEdgeContent(
                CircularProgressIndicator.Builder()
                    .setProgress(progress)
                    .setCircularProgressIndicatorColors(ProgressIndicatorColors(argb(ORANGE), argb(TRACK)))
                    .build()
            )
            .setPrimaryLabelTextContent(
                Text.Builder(this, tr("Schritte", "Steps"))
                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                    .setColor(argb(ORANGE))
                    .build()
            )
            .setContent(content)
            .apply {
                if (details.isNotEmpty()) {
                    setSecondaryLabelTextContent(
                        Text.Builder(this@ForgeTileService, details)
                            .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                            .setColor(argb(GREY))
                            .build()
                    )
                }
            }
            .build()
    }

    private companion object {
        const val RESOURCES_VERSION = "1"
        const val ORANGE = 0xFFFF7A1A.toInt()
        const val TRACK = 0xFF3A3A3A.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
        const val GREY = 0xFFB0B0B0.toInt()
    }
}
