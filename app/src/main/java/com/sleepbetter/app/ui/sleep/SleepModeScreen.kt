package com.sleepbetter.app.ui.sleep

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.ui.components.CircleButton
import com.sleepbetter.app.ui.components.DuskScene
import com.sleepbetter.app.ui.components.Glyph
import com.sleepbetter.app.ui.components.GlyphIcon
import com.sleepbetter.app.ui.components.Grabber
import com.sleepbetter.app.ui.components.clock
import com.sleepbetter.app.ui.components.pressable
import com.sleepbetter.app.ui.components.rememberClock
import com.sleepbetter.app.ui.components.rememberEngineFrame
import com.sleepbetter.app.ui.components.rememberReduceMotion
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import kotlin.math.sin

private const val CYCLE_MINUTES = 90

/**
 * Sleep mode: the dusk scene fills the screen, Pico and Lulu sleep on the moon, one
 * big countdown. After 30 s it dims further and ambient motion stops; sound
 * keeps playing. Swipe up (or tap) "I'm awake" in the morning.
 */
@Composable
fun SleepModeScreen(vm: AppViewModel, onWake: (logged: Boolean) -> Unit, onBack: () -> Unit) {
    val mix by vm.mix.collectAsStateWithLifecycle()
    val frame by rememberEngineFrame(vm.engine)
    val time by rememberClock()
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(settled) {
        if (!settled) {
            delay(30_000)
            settled = true
        }
    }
    val dim by animateFloatAsState(if (settled) 0.55f else 0.1f, tween(4000), label = "dim")
    val still = rememberReduceMotion() || settled
    val started = vm.repository.sleepStartedAt ?: Instant.now()
    val asleep = Duration.between(started, Instant.now()).toMinutes().toInt().coerceAtLeast(0)
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    fun wake() = onWake(vm.wakeUp())

    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Night)
            .pointerInput(Unit) { detectTapGestures(onTap = { settled = false }) }, // tap anywhere to brighten
    ) {
        DuskScene(mix.active, frame, if (still) 0f else time, Modifier.fillMaxSize(), still = still, dim = dim)
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleButton(Glyph.BACK, "Back to wind down", onBack, bg = Color.White.copy(alpha = 0.14f), tint = Color.White)
                Spacer(Modifier.weight(1f))
                Text(if (mix.playing) "Sound on" else "Sound paused", style = Type.Label, color = Color.White.copy(alpha = 0.8f))
            }
            Spacer(Modifier.weight(0.55f))
            Text("Sound fades out in", style = Type.Body, color = Color.White.copy(alpha = 0.8f))
            Text(clock(frame.remainingSeconds), style = Type.Numeral.copy(fontSize = 72.sp, lineHeight = 76.sp), color = Color.White)

            Row(
                Modifier
                    .padding(top = 22.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat("${asleep / CYCLE_MINUTES + 1} of 5", "Sleep cycle")
                Stat("${mix.active.size}", "Sounds")
                Stat(if (mix.playing) "On" else "Off", "Playback")
            }
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("−15 min") { vm.extendTimer(-15 * 60) }
                GhostButton(if (mix.playing) "Pause" else "Play") { vm.togglePlay() }
                GhostButton("+15 min") { vm.extendTimer(15 * 60) }
            }
            Spacer(Modifier.weight(1f))

            // Swipe up to wake; also a plain button for accessibility.
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationY = drag.value }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (drag.value < -140f) wake() else scope.launch { drag.animateTo(0f, spring(0.5f)) }
                            },
                        ) { change, dy ->
                            change.consume()
                            scope.launch { drag.snapTo((drag.value + dy).coerceIn(-260f, 0f)) }
                        }
                    }
                    .semantics {
                        role = Role.Button
                        contentDescription = "I'm awake. Log my sleep"
                        onClick { wake(); true }
                    }
                    .padding(bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val bob = if (still) 0f else 6f * sin(time * 2.4f)
                GlyphIcon(Glyph.BACK, Color.White, Modifier.graphicsLayer { rotationZ = 90f; translationY = bob })
                Text("Swipe up when you wake", style = Type.Label, color = Color.White, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(10.dp))
                Grabber()
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = Type.Heading, color = Color.White)
        Text(label, style = Type.Small, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun GhostButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .pressable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) { Text(text, style = Type.Label, color = Color.White) }
}
