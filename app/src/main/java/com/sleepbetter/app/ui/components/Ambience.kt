package com.sleepbetter.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sleepbetter.app.audio.AudioEngine
import com.sleepbetter.app.audio.EngineFrame
import com.sleepbetter.app.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.sin

/** Seconds since this composable appeared, updated every frame. Drives all ambient motion. */
@Composable
fun rememberClock(): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> time.floatValue = (now - start) / 1_000_000_000f }
        }
    }
    return time
}

/** The engine's levels, beats and thunder strikes, sampled once per frame. */
@Composable
fun rememberEngineFrame(engine: AudioEngine): State<EngineFrame> {
    val frame = remember { mutableStateOf(EngineFrame()) }
    LaunchedEffect(engine) {
        while (true) {
            withFrameNanos { frame.value = engine.frame() }
        }
    }
    return frame
}

/**
 * Soft moving colour fields ("aurora") tinted by whatever is playing.
 * Pure gradients, no blur, so it is cheap on every API level.
 */
@Composable
fun AuroraBackground(colors: List<Color>, modifier: Modifier = Modifier, base: Color = Palette.Night, still: Boolean = false) {
    val c1 by animateColorAsState(colors.getOrElse(0) { Color(0xFF3B3F8C) }, tween(1400), label = "a1")
    val c2 by animateColorAsState(colors.getOrElse(1) { Color(0xFF2A2E6E) }, tween(1400), label = "a2")
    val c3 by animateColorAsState(colors.getOrElse(2) { colors.getOrElse(0) { Color(0xFF40306E) } }, tween(1400), label = "a3")
    val clock by rememberClock()
    Canvas(modifier.fillMaxSize().background(base)) {
        val t = if (still) 0f else clock
        val w = size.width
        val h = size.height
        fun blob(color: Color, cx: Float, cy: Float, r: Float) {
            drawCircle(
                Brush.radialGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0f)), Offset(cx, cy), r),
                r,
                Offset(cx, cy),
            )
        }
        blob(c1, w * (0.15f + 0.12f * sin(t / 9f)), h * (0.12f + 0.08f * cos(t / 11f)), w * 0.9f)
        blob(c2, w * (0.95f + 0.1f * cos(t / 13f)), h * (0.42f + 0.06f * sin(t / 10f)), w * 0.85f)
        blob(c3, w * (0.3f + 0.15f * sin(t / 15f)), h * (0.85f + 0.05f * cos(t / 12f)), w * 0.95f)
    }
}

/** Frosted-glass look: translucent fill, bright hairline edge. */
fun Modifier.glass(shape: Shape): Modifier = this
    .background(Palette.GlassFill, shape)
    .border(1.dp, Palette.GlassEdge, shape)

/** Gentle haptics. Uses rich primitives on phones that support them, a plain tick elsewhere. */
object Haptics {
    fun tick(context: Context) = play(context, VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f, 12)

    fun sessionDone(context: Context) = play(context, VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.8f, 60)

    private fun play(context: Context, primitive: Int, scale: Float, fallbackMs: Long) {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 30 && vibrator.areAllPrimitivesSupported(primitive)) {
            vibrator.vibrate(VibrationEffect.startComposition().addPrimitive(primitive, scale).compose())
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(fallbackMs, (255 * scale).toInt().coerceIn(1, 255)))
        }
    }
}
