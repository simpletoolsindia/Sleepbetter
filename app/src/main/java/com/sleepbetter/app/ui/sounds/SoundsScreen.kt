package com.sleepbetter.app.ui.sounds

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.audio.TimerChoice
import com.sleepbetter.app.ui.components.BentoCard
import com.sleepbetter.app.ui.components.ChoiceRow
import com.sleepbetter.app.ui.components.DuskScene
import com.sleepbetter.app.ui.components.GlyphIcon
import com.sleepbetter.app.ui.components.Haptics
import com.sleepbetter.app.ui.components.MochiView
import com.sleepbetter.app.ui.components.MorphPlayButton
import com.sleepbetter.app.ui.components.SegmentedTabs
import com.sleepbetter.app.ui.components.SoundTile
import com.sleepbetter.app.ui.components.enter
import com.sleepbetter.app.ui.components.glyph
import com.sleepbetter.app.ui.components.rememberClock
import com.sleepbetter.app.ui.components.rememberEngineFrame
import com.sleepbetter.app.ui.components.rememberReduceMotion
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.app.ui.theme.deep
import com.sleepbetter.app.ui.theme.tint
import com.sleepbetter.core.audio.SoundId
import com.sleepbetter.core.audio.Spatial
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class SoundView(val label: String) { MIXES("Mixes"), MIX("Pick sounds"), SPACE("Place them") }

/**
 * Sounds: pick what plays (tiles light up with live level bars), then place
 * each sound nearer or farther, left or right, in the Space view.
 */
@Composable
fun SoundsScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val mix by vm.mix.collectAsStateWithLifecycle()
    val frame by rememberEngineFrame(vm.engine)
    val time by rememberClock()
    val still = rememberReduceMotion()
    var view by rememberSaveable { mutableStateOf(SoundView.MIXES) }

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Bottom) {
            Text("Sounds 🎧", style = Type.Display, color = Palette.Ink, modifier = Modifier.weight(1f))
            Text(if (mix.active.isEmpty()) "Silent" else "${mix.active.size} playing", style = Type.Label, color = Palette.InkMuted)
        }

        Box(
            Modifier
                .enter(0)
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(30.dp)),
        ) {
            DuskScene(mix.active, frame, time, Modifier.fillMaxSize(), still = still)
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp).fillMaxWidth(0.68f)) {
                Text(if (mix.playing) "Now playing" else "Ready", style = Type.Label, color = Color.White.copy(alpha = 0.8f))
                Text(
                    if (mix.active.isEmpty()) "Pick a sound below" else mix.active.joinToString(", ") { it.label },
                    style = Type.Heading,
                    color = Color.White,
                    maxLines = 2,
                )
            }
            MorphPlayButton(
                mix.playing, vm::togglePlay,
                Modifier.align(Alignment.BottomEnd).padding(16.dp),
                diameter = 58.dp, color = Color.White, iconColor = Palette.Ink,
            )
        }

        SegmentedTabs(SoundView.entries.toList(), view, { it.label }, { view = it }, Modifier.fillMaxWidth())

        AnimatedContent(
            targetState = view,
            transitionSpec = { (fadeIn() + scaleIn(spring(0.7f, 400f), initialScale = 0.97f)) togetherWith fadeOut() },
            label = "view",
        ) { v ->
            when (v) {
                SoundView.MIXES -> MixesPanel(vm)
                SoundView.MIX -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoundId.entries.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { id ->
                                SoundTile(
                                    id, id in mix.active, frame.levels[id.ordinal], time,
                                    onClick = { vm.toggleSound(id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                SoundView.SPACE -> SoundSpace(mix.active, mix.positions, frame.levels, time, vm::moveSound)
            }
        }

        BentoCard(Modifier.fillMaxWidth()) {
            Text("Play for", style = Type.Heading, color = Palette.Ink)
            ChoiceRow(TimerChoice.entries.toList(), mix.timer, { it.label }, vm::setTimer, Modifier.fillMaxWidth().padding(top = 12.dp))
            val minutes = mix.timer.minutes
            Text(
                if (minutes == null) "Plays until you stop it" else LocalTime.now().plusMinutes(minutes.toLong()).let { "Fades out gently at %02d:%02d".format(it.hour, it.minute) },
                style = Type.Small,
                color = Palette.InkMuted,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Spacer(Modifier.height(120.dp))
    }
}

/**
 * The space: Mochi listens in the middle. Drag a sound closer to make it
 * louder, or to the side to move it left or right.
 */
@Composable
private fun SoundSpace(
    active: Set<SoundId>,
    positions: Map<SoundId, Pair<Float, Float>>,
    levels: FloatArray,
    time: Float,
    onMove: (SoundId, Float, Float) -> Unit,
) {
    val context = LocalContext.current
    val current by rememberUpdatedState(positions)
    BentoCard(Modifier.fillMaxWidth(), padding = 12.dp) {
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val density = LocalDensity.current
            val side = with(density) { maxWidth.toPx() }
            val radius = side / 2f * 0.84f
            val cx = side / 2f
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Brush.radialGradient(listOf(Palette.Accent.copy(alpha = 0.45f), Color.Transparent), center, radius), radius)
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 10f))
                listOf(1f, 0.64f, 0.3f).forEach { f -> drawCircle(Palette.Accent, radius * f, center, style = Stroke(2f, pathEffect = if (f < 0.5f) null else dash)) }
            }
            Text("far", style = Type.Small, color = Palette.InkMuted, modifier = Modifier.align(Alignment.TopCenter))
            Text("left", style = Type.Small, color = Palette.InkMuted, modifier = Modifier.align(Alignment.CenterStart))
            Text("right", style = Type.Small, color = Palette.InkMuted, modifier = Modifier.align(Alignment.CenterEnd))
            MochiView(Modifier.align(Alignment.Center).size(78.dp), headphones = true, mood = 3.5f)

            if (active.isEmpty()) {
                Text(
                    "Turn on a sound in Pick sounds, then place it here.",
                    style = Type.Body,
                    color = Palette.InkSoft,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                )
            }
            for (id in active) key(id) {
                val (x, y) = positions[id] ?: (id.stageX to id.stageY)
                val closeness = Spatial.closeness(x, y)
                val pct = (closeness * 100).roundToInt()
                val sizeDp = (54f + closeness * 26f).dp
                val sizePx = with(density) { sizeDp.toPx() }
                val pulse = 1f + levels[id.ordinal] * 0.18f + 0.025f * sin(time * 3f + id.ordinal)
                val side2 = when {
                    x < -0.25f -> "left"
                    x > 0.25f -> "right"
                    else -> "centre"
                }
                fun nudge(dx: Float, dy: Float, scale: Float = 1f) {
                    val p = current[id] ?: (id.stageX to id.stageY)
                    onMove(id, p.first * scale + dx, p.second * scale + dy)
                }
                Column(
                    Modifier
                        .offset { IntOffset((cx + x * radius - sizePx / 2f).roundToInt(), (cx + y * radius - sizePx / 2f).roundToInt()) }
                        .size(sizeDp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                        .drawBehind { drawCircle(id.tint().copy(alpha = 0.5f), size.minDimension / 2f * 1.25f) }
                        .clip(CircleShape)
                        .background(id.tint())
                        .pointerInput(id) {
                            detectDragGestures(onDragStart = { Haptics.tick(context) }, onDragEnd = { Haptics.tick(context) }) { change, drag ->
                                change.consume()
                                val p = current[id] ?: (id.stageX to id.stageY)
                                onMove(id, p.first + drag.x / radius, p.second + drag.y / radius)
                            }
                        }
                        .semantics {
                            contentDescription = "${id.label}, volume $pct percent, $side2"
                            customActions = listOf(
                                CustomAccessibilityAction("Move closer") { nudge(0f, 0f, 0.7f); true },
                                CustomAccessibilityAction("Move farther") { nudge(0f, 0f, 1.3f); true },
                                CustomAccessibilityAction("Move left") { nudge(-0.2f, 0f); true },
                                CustomAccessibilityAction("Move right") { nudge(0.2f, 0f); true },
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    GlyphIcon(id.glyph(), id.deep(), size = 22.dp)
                    Text("$pct%", style = Type.Small.copy(fontSize = 11.sp), color = id.deep())
                }
            }
        }
    }
}
