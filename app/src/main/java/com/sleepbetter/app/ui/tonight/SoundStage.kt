package com.sleepbetter.app.ui.tonight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepbetter.app.audio.EngineFrame
import com.sleepbetter.app.ui.components.Critter
import com.sleepbetter.app.ui.components.CritterView
import com.sleepbetter.app.ui.components.Haptics
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.app.ui.theme.uiColor
import com.sleepbetter.core.audio.SoundId
import com.sleepbetter.core.audio.Spatial
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The Sound Stage: each playing sound is a glowing bubble around Pip. Drag it
 * closer to make it louder, or left and right to place it in stereo.
 * Bubbles pulse with the sound's real level from the engine.
 */
@Composable
fun SoundStage(
    active: Set<SoundId>,
    positions: Map<SoundId, Pair<Float, Float>>,
    frame: EngineFrame,
    time: Float,
    onMove: (SoundId, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentPositions by rememberUpdatedState(positions)
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1f)) {
        val density = LocalDensity.current
        val sidePx = with(density) { maxWidth.toPx() }
        val radiusPx = sidePx / 2f * 0.86f
        val center = Offset(sidePx / 2f, sidePx / 2f)

        Canvas(Modifier.fillMaxSize()) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 12f))
            drawCircle(Color(0x29FFFFFF), radiusPx, center, style = Stroke(1.5f, pathEffect = dash))
            drawCircle(Color(0x29FFFFFF), radiusPx * 0.62f, center, style = Stroke(1.5f, pathEffect = dash))
            drawCircle(Color(0x1AFFFFFF), radiusPx * 0.3f, center, style = Stroke(1.5f))
        }
        Text("far", style = Type.Small, color = Palette.InkMuted, modifier = Modifier.align(Alignment.TopCenter))
        Text("left", style = Type.Small, color = Palette.InkMuted, modifier = Modifier.align(Alignment.CenterStart))
        Text("right", style = Type.Small, color = Palette.InkMuted, modifier = Modifier.align(Alignment.CenterEnd))

        val musicOn = SoundId.FOCUS_MUSIC in active
        CritterView(
            Critter.PANDA,
            Modifier.align(Alignment.Center).size(72.dp),
            headphones = true,
            beat = if (musicOn) frame.beatPhase else null,
        )

        for (id in active) key(id) {
            val (x, y) = positions[id] ?: (id.stageX to id.stageY)
            val closeness = Spatial.closeness(x, y)
            val level = frame.levels[id.ordinal]
            val sizeDp = (50f + closeness * 34f).dp
            val sizePx = with(density) { sizeDp.toPx() }
            val pulse = 1f + level * 0.22f + 0.03f * sin(time * 3f + id.ordinal)
            val color = id.uiColor()
            val pct = (closeness * 100).roundToInt()
            val side = when {
                x < -0.25f -> "left"
                x > 0.25f -> "right"
                else -> "centre"
            }

            fun nudge(dx: Float, dy: Float, scale: Float = 1f) {
                val p = currentPositions[id] ?: (id.stageX to id.stageY)
                onMove(id, p.first * scale + dx, p.second * scale + dy)
            }

            Box(
                Modifier
                    .offset { IntOffset((center.x + x * radiusPx - sizePx / 2f).roundToInt(), (center.y + y * radiusPx - sizePx / 2f).roundToInt()) }
                    .size(sizeDp)
                    .scale(pulse)
                    .drawBehind {
                        val r = size.minDimension / 2f
                        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent), this.center, r * 1.8f), r * 1.8f)
                    }
                    .background(color, CircleShape)
                    .pointerInput(id) {
                        detectDragGestures(
                            onDragStart = { Haptics.tick(context) },
                            onDragEnd = { Haptics.tick(context) },
                        ) { change, drag ->
                            change.consume()
                            val p = currentPositions[id] ?: (id.stageX to id.stageY)
                            onMove(id, p.first + drag.x / radiusPx, p.second + drag.y / radiusPx)
                        }
                    }
                    .semantics {
                        contentDescription = "${id.label}, volume $pct percent, $side"
                        customActions = listOf(
                            CustomAccessibilityAction("Move closer") { nudge(0f, 0f, 0.7f); true },
                            CustomAccessibilityAction("Move farther") { nudge(0f, 0f, 1.3f); true },
                            CustomAccessibilityAction("Move left") { nudge(-0.2f, 0f); true },
                            CustomAccessibilityAction("Move right") { nudge(0.2f, 0f); true },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(id.label, style = Type.Small.copy(fontSize = 11.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold), color = Color(0xFF0A0B1C))
                    Text("$pct%", style = Type.Small.copy(fontSize = 11.sp, lineHeight = 12.sp), color = Color(0xFF0A0B1C))
                }
            }
        }
    }
}

/** "Rain is close. Thunder is far away." */
fun stageCaption(active: Set<SoundId>, positions: Map<SoundId, Pair<Float, Float>>): String {
    if (active.isEmpty()) return "Add a sound to place it on the stage."
    val near = mutableListOf<String>()
    val far = mutableListOf<String>()
    for (id in active) {
        val (x, y) = positions[id] ?: (id.stageX to id.stageY)
        val c = Spatial.closeness(x, y)
        if (c > 0.6f) near += id.label else if (c < 0.3f) far += id.label
    }
    val parts = mutableListOf<String>()
    if (near.isNotEmpty()) parts += near.joinToString(" and ") + if (near.size > 1) " are close" else " is close"
    if (far.isNotEmpty()) parts += far.joinToString(" and ") + if (far.size > 1) " are far away" else " is far away"
    return if (parts.isEmpty()) "Everything sits in the middle distance." else parts.joinToString(". ") + "."
}
