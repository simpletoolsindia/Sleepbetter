package com.sleepbetter.app.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.sleepbetter.core.audio.SoundId

/**
 * Renders the dusk scene offscreen, for places that cannot run Compose:
 * notification artwork and the flip-book frames of the bedtime reminder.
 * Frames are RGB_565 to keep them small enough to send to the system;
 * layouts round their corners.
 */
object SceneFrames {
    /** One still of the scene with [active] sounds, at scene time [t]. */
    fun still(active: Set<SoundId>, width: Int, height: Int, t: Float = 3f): Bitmap =
        frames(active, width, height, listOf(t)).first()

    /**
     * Frames at [times] (seconds). With [lightningAt] set and Thunder in
     * [active], a strike plays across the frames from that moment.
     */
    fun frames(
        active: Set<SoundId>,
        width: Int,
        height: Int,
        times: List<Float>,
        lightningAt: Float = -10f,
    ): List<Bitmap> = times.map { t ->
        val image = ImageBitmap(width, height)
        val size = Size(width.toFloat(), height.toFloat())
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), size) {
            drawDuskScene(active, { if (it in active) 1f else 0f }, t, t, lightningAt, showMochi = true, dim = 0f)
        }
        image.asAndroidBitmap().copy(Bitmap.Config.RGB_565, false)
    }
}
