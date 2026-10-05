package com.sleepbetter.app.ui.winddown

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.ui.components.KineticHeadline
import com.sleepbetter.app.ui.components.PillButton
import com.sleepbetter.app.ui.components.glass
import com.sleepbetter.app.ui.components.rememberClock
import com.sleepbetter.app.ui.components.rememberReduceMotion
import com.sleepbetter.app.ui.theme.Display
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** One 4-7-8 breathing cycle: in for 4 s, hold 7 s, out for 8 s. */
private data class BreathPhase(val word: String, val count: Int, val size: Float)

private fun breathAt(seconds: Float): BreathPhase {
    val c = seconds % 19f
    fun ease(x: Float) = 0.5f - 0.5f * cos(PI.toFloat() * x.coerceIn(0f, 1f))
    return when {
        c < 4f -> BreathPhase("Breathe in", c.toInt() + 1, 0.62f + 0.38f * ease(c / 4f))
        c < 11f -> BreathPhase("Hold", (c - 4f).toInt() + 1, 1f)
        else -> BreathPhase("Breathe out", (c - 11f).toInt() + 1, 1f - 0.38f * ease((c - 11f) / 8f))
    }
}

@Composable
fun WindDownScreen(vm: AppViewModel, onStartSleep: () -> Unit, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val time by rememberClock()
    val still = rememberReduceMotion()
    var dim by remember { mutableFloatStateOf(0f) }
    val phase = breathAt(time)
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.setReminders(granted)
    }

    Box(modifier) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.padding(top = 12.dp))
            Text("Wind down, 4-7-8 breathing", style = Type.Body, color = Palette.InkSoft)
            KineticHeadline("Let the day go.", Type.Hero)

            Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize(0.82f)) {
                    // A soft blob whose outline wobbles while it grows and shrinks with the breath.
                    val r = size.minDimension / 2f * phase.size
                    val t = if (still) 0f else time
                    val path = Path()
                    val steps = 72
                    for (i in 0..steps) {
                        val a = i / steps.toFloat() * 2f * PI.toFloat()
                        val wobble = 1f + 0.05f * sin(3f * a + t * 0.9f) + 0.035f * sin(5f * a - t * 1.3f)
                        val p = Offset(center.x + cos(a) * r * wobble, center.y + sin(a) * r * wobble)
                        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                    }
                    path.close()
                    drawCircle(Brush.radialGradient(listOf(Color(0x59E86FA8), Color.Transparent), center, r * 1.4f), r * 1.4f)
                    drawPath(
                        path,
                        Brush.sweepGradient(
                            listOf(Color(0xFFF6C28B), Color(0xFFE86FA8), Color(0xFF7B5CF0), Color(0xFF4FA7E8), Color(0xFFF6C28B)),
                            center,
                        ),
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    Text(phase.word, style = Type.Title.copy(fontSize = 30.sp), color = Color(0xFF160C26))
                    Text(
                        phase.count.toString(),
                        style = Type.Hero.copy(fontFamily = Display, fontWeight = FontWeight.Light, fontSize = 60.sp),
                        color = Color(0xFF160C26),
                    )
                }
            }
            Text(
                "Breathe in through your nose for 4, hold for 7, and breathe out slowly for 8.",
                style = Type.Body,
                color = Palette.InkSoft,
            )

            Column(
                Modifier
                    .padding(top = 20.dp)
                    .glass(RoundedCornerShape(30.dp))
                    .padding(18.dp),
            ) {
                Text("Lights down", style = Type.Section, color = Palette.Ink)
                Text("Slide to dim the screen as you get sleepy.", style = Type.Small, color = Palette.InkSoft)
                Slider(
                    value = dim,
                    onValueChange = { dim = it },
                    valueRange = 0f..0.8f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFFF6C28B)),
                    modifier = Modifier.semantics { stateDescription = "Screen dimmed ${(dim * 100).toInt()} percent" },
                )

                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Bedtime ${settings.bedtimeLabel}", style = Type.Section, color = Palette.Ink)
                        Text("Steady bedtimes bring new visitors.", style = Type.Small, color = Palette.InkSoft)
                    }
                    RoundStep("−", "15 minutes earlier") { vm.shiftBedtime(-15) }
                    Spacer(Modifier.size(8.dp))
                    RoundStep("+", "15 minutes later") { vm.shiftBedtime(15) }
                }

                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Remind me", style = Type.Label, color = Palette.Ink)
                        Text("${settings.windDownMinutes} minutes before bedtime", style = Type.Small, color = Palette.InkSoft)
                    }
                    Switch(
                        checked = settings.remindersOn,
                        onCheckedChange = { on ->
                            if (on && Build.VERSION.SDK_INT >= 33) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                vm.setReminders(on)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Palette.Lantern),
                    )
                }

                PillButton(
                    "Start sleep mode",
                    onClick = onStartSleep,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
            }
            Spacer(Modifier.padding(bottom = 24.dp))
        }
        // The dimmer covers the screen but has no touch handling, so controls stay usable.
        if (dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
    }
}

@Composable
private fun RoundStep(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0x1FFFFFFF))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = Type.Title, color = Palette.Ink)
    }
}
