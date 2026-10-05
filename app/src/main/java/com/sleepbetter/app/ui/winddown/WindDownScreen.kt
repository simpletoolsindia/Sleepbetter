package com.sleepbetter.app.ui.winddown

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.ui.components.CircleButton
import com.sleepbetter.app.ui.components.Glyph
import androidx.compose.ui.geometry.Offset
import com.sleepbetter.app.ui.components.DriftingEmoji
import com.sleepbetter.app.ui.components.LocalBurst
import com.sleepbetter.app.ui.components.BestFriends
import com.sleepbetter.app.ui.components.PrimaryButton
import com.sleepbetter.app.ui.components.hm
import com.sleepbetter.app.ui.components.rememberClock
import com.sleepbetter.app.ui.components.rememberReduceMotion
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import kotlin.math.PI
import kotlin.math.cos

private data class Breath(val word: String, val count: Int, val size: Float)

/** One 4-7-8 cycle: in for 4 s, hold 7 s, out for 8 s. */
private fun breathAt(seconds: Float): Breath {
    val c = seconds % 19f
    fun ease(x: Float) = 0.5f - 0.5f * cos(PI.toFloat() * x.coerceIn(0f, 1f))
    return when {
        c < 4f -> Breath("Breathe in", c.toInt() + 1, 0.7f + 0.3f * ease(c / 4f))
        c < 11f -> Breath("Hold", (c - 4f).toInt() + 1, 1f)
        else -> Breath("Breathe out", (c - 11f).toInt() + 1, 1f - 0.3f * ease((c - 11f) / 8f))
    }
}

/** Wind down: breathe with Pico and Lulu (they swell and settles with you), set the evening, then sleep. */
@Composable
fun WindDownScreen(vm: AppViewModel, onBack: () -> Unit, onStartSleep: () -> Unit) {
    val burst = LocalBurst.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val time by rememberClock()
    val still = rememberReduceMotion()
    var dim by remember { mutableFloatStateOf(0f) }
    val breath = breathAt(time)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> vm.setReminders(granted) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.DuskTop, Palette.DuskMid, Palette.DuskLow))),
    ) {
        // Stars and Zs drifting up behind everything.
        DriftingEmoji(listOf("✨", "⭐", "💤", "🌙"), count = 9)
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleButton(Glyph.BACK, "Back", onBack, bg = Color.White.copy(alpha = 0.16f), tint = Color.White)
                Spacer(Modifier.weight(1f))
                Text("Bedtime ${settings.bedtimeLabel}", style = Type.Label, color = Color.White.copy(alpha = 0.85f))
            }
            Text("Breathe with Pico & Lulu", style = Type.Display, color = Color.White, modifier = Modifier.padding(top = 18.dp))
            Text("In through your nose for 4, hold for 7, out slowly for 8.", style = Type.Body, color = Color.White.copy(alpha = 0.8f))

            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                val s = if (still) 0.85f else breath.size
                Box(
                    Modifier
                        .size(260.dp)
                        .graphicsLayer { scaleX = s; scaleY = s }
                        .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)), RoundedCornerShape(50)),
                )
                BestFriends(
                    Modifier.width(260.dp).graphicsLayer { scaleX = s; scaleY = s },
                    sleeping = breath.word == "Hold",
                    mood = 3.2f,
                )
            }
            Column(
                Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(breath.word, style = Type.Title.copy(fontSize = 28.sp), color = Color.White)
                Text(breath.count.toString(), style = Type.Numeral, color = Color.White)
            }

            Column(
                Modifier
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Lights down", style = Type.Heading, color = Color.White)
                Slider(
                    value = dim,
                    onValueChange = { dim = it },
                    valueRange = 0f..0.8f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Palette.Moon, inactiveTrackColor = Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.semantics { stateDescription = "Screen dimmed ${(dim * 100).toInt()} percent" },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Bedtime", style = Type.Heading, color = Color.White)
                        Text("A steady bedtime brings new friends", style = Type.Small, color = Color.White.copy(alpha = 0.75f))
                    }
                    CircleButton(Glyph.MINUS, "15 minutes earlier", { vm.shiftBedtime(-15) }, bg = Color.White.copy(alpha = 0.18f), tint = Color.White, size = 40.dp)
                    Text(settings.bedtimeLabel, style = Type.Heading, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp))
                    CircleButton(Glyph.PLUS, "15 minutes later", { vm.shiftBedtime(15) }, bg = Color.White.copy(alpha = 0.18f), tint = Color.White, size = 40.dp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Remind me", style = Type.Heading, color = Color.White)
                        Text("At ${hm(settings.bedtimeMinute - settings.windDownMinutes)}, ${settings.windDownMinutes} min before bed", style = Type.Small, color = Color.White.copy(alpha = 0.75f))
                    }
                    Switch(
                        checked = settings.remindersOn,
                        onCheckedChange = { on ->
                            if (on && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.setReminders(on)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Palette.Moon, checkedThumbColor = Palette.DarkInk),
                    )
                }
                PrimaryButton("Start sleep mode 😴", {
                    burst.fire(listOf("🌙", "💤", "⭐", "✨"), Offset(0.5f, 0.9f), count = 20)
                    onStartSleep()
                }, Modifier.fillMaxWidth().padding(top = 6.dp), color = Palette.Moon, textColor = Palette.DarkInk, glyph = Glyph.MOON)
            }
            Spacer(Modifier.height(24.dp))
        }
        // The dimmer has no touch handling, so the controls underneath stay usable.
        if (dim > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
    }
}
