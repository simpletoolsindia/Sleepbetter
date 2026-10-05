package com.sleepbetter.app.ui.sleep

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.ui.components.rememberClock
import com.sleepbetter.app.ui.components.rememberEngineFrame
import com.sleepbetter.app.ui.components.rememberReduceMotion
import com.sleepbetter.app.ui.theme.Display
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.app.ui.tonight.IslandScene
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

private const val CYCLE_MINUTES = 90

/**
 * Sleep mode: near-black, one big countdown, a dimmed island. After 30 s
 * everything fades further and ambient motion stops; sound keeps playing.
 */
@Composable
fun SleepModeScreen(vm: AppViewModel, onWake: (logged: Boolean) -> Unit, onBack: () -> Unit) {
    val mix by vm.mix.collectAsStateWithLifecycle()
    val frame by rememberEngineFrame(vm.engine)
    val time by rememberClock()
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(30_000)
        settled = true
    }
    val fade by animateFloatAsState(if (settled) 0.45f else 1f, tween(4000), label = "fade")
    val still = rememberReduceMotion() || settled
    val visitors = remember { vm.repository.visitorProgress().unlocked }

    val started = vm.repository.sleepStartedAt ?: Instant.now()
    val asleepMinutes = Duration.between(started, Instant.now()).toMinutes().toInt().coerceAtLeast(0)
    val cycle = asleepMinutes / CYCLE_MINUTES

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.SleepBlack)
            .clickable(role = Role.Button, onClickLabel = "Brighten") { settled = false }
            .alpha(fade)
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Back",
                style = Type.Label,
                color = Color(0xFF8E8BD6),
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onBack).heightIn(min = 44.dp).padding(12.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(mix.active.joinToString(", ") { it.label }, style = Type.Small, color = Color(0xFF6E6BB0))
        }

        IslandScene(
            active = mix.active,
            frame = frame,
            time = if (still) 0f else time,
            visitors = visitors,
            dim = 0.6f,
            still = still,
            modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(390f / 470f),
        )

        Text("Sound stops in", style = Type.Body, color = Color(0xFF6E6BB0))
        Text(
            clockText(frame.remainingSeconds),
            style = Type.Hero.copy(fontFamily = Display, fontWeight = FontWeight.Light, fontSize = 72.sp, lineHeight = 76.sp),
            color = Color(0xFFDAD8FA),
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp).semantics { contentDescription = "Estimated sleep cycle ${cycle + 1} of 5" },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            (0 until 5).forEach { i ->
                val c = when {
                    i < cycle -> Color(0xFF5B58A8)
                    i == cycle -> Color(0xFF8E8BD6)
                    else -> Color(0xFF1E2152)
                }
                Box(Modifier.weight(1f).height(8.dp).background(c, RoundedCornerShape(4.dp)))
            }
        }
        Text("Each segment is one estimated 90-minute sleep cycle", style = Type.Small, color = Color(0xFF6E6BB0), modifier = Modifier.padding(top = 8.dp))

        Row(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DimButton("−15 m", "15 minutes less") { vm.extendTimer(-15 * 60) }
            DimButton(if (mix.playing) "Pause" else "Play", if (mix.playing) "Pause" else "Play") { vm.togglePlay() }
            DimButton("+15 m", "15 minutes more") { vm.extendTimer(15 * 60) }
        }

        Spacer(Modifier.weight(1f))
        DimButton("I'm awake, log my sleep", "I'm awake, log my sleep", Modifier.padding(bottom = 28.dp)) {
            onWake(vm.wakeUp())
        }
    }
}

@Composable
private fun DimButton(text: String, description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, Color(0xFF1E2152), RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.Label, color = Color(0xFF8E8BD6))
    }
}

private fun clockText(seconds: Int?): String {
    if (seconds == null) return "All night"
    return "%d:%02d:%02d".format(seconds / 3600, seconds % 3600 / 60, seconds % 60)
}
