package com.sleepbetter.app.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import com.sleepbetter.app.audio.AudioEngine
import com.sleepbetter.app.audio.EngineFrame

/** True when the user turned animations off in system settings. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Seconds since this composable appeared, updated every frame. Drives ambient motion. */
@Composable
fun rememberClock(): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { now -> time.floatValue = (now - start) / 1_000_000_000f }
    }
    return time
}

/** The engine's levels, beats and thunder strikes, sampled once per frame. */
@Composable
fun rememberEngineFrame(engine: AudioEngine): State<EngineFrame> {
    val frame = remember { mutableStateOf(EngineFrame()) }
    LaunchedEffect(engine) {
        while (true) withFrameNanos { frame.value = engine.frame() }
    }
    return frame
}

/** Clickable that squishes a little while pressed and springs back: every tap feels physical. */
fun Modifier.pressable(role: Role = Role.Button, enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(0.5f, 600f), label = "press")
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = role, onClick = onClick)
}

/** Rises in from below with a fade, after [delayMs]. Used once per screen for a calm entrance. */
fun Modifier.enter(delayMs: Int = 0): Modifier = composed {
    val still = rememberReduceMotion()
    val p = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        p.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 36f
    }
}

/** A number that counts up to [target] when it first appears or changes. */
@Composable
fun rememberCountUp(target: Float, durationMs: Int = 900): Float {
    val still = rememberReduceMotion()
    val anim = remember { Animatable(if (still) target else 0f) }
    LaunchedEffect(target) { anim.animateTo(target, tween(durationMs, easing = FastOutSlowInEasing)) }
    return anim.value
}

/** Gentle haptics: rich primitives where supported, a plain tick elsewhere. */
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
