package com.sleepbetter.app.ui.checkin

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.ui.components.MochiView
import com.sleepbetter.app.ui.components.Mood
import com.sleepbetter.app.ui.components.PrimaryButton
import com.sleepbetter.app.ui.components.enter
import com.sleepbetter.app.ui.components.hoursMinutes
import com.sleepbetter.app.ui.components.pressable
import com.sleepbetter.app.ui.components.BouncyEmoji
import com.sleepbetter.app.ui.components.LocalBurst
import com.sleepbetter.app.ui.components.floaty
import androidx.compose.ui.geometry.Offset
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.core.sleep.NightTag
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Morning check-in. Slide and Pico's face follows you, from a rough night
 * to a great one; the background takes the mood's colour.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInScreen(vm: AppViewModel, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val night = sessions.lastOrNull()
    var mood by remember(night) { mutableFloatStateOf(((night?.rating ?: 4) - 1).toFloat()) }
    var tags by remember(night) { mutableStateOf(night?.tags ?: emptySet()) }
    val index = mood.roundToInt().coerceIn(0, 4)
    val current = Mood.entries[index]
    val lower = Mood.entries[mood.toInt().coerceIn(0, 4)]
    val upper = Mood.entries[(mood.toInt() + 1).coerceIn(0, 4)]
    val burst = LocalBurst.current
    val bg by animateColorAsState(lerp(lower.tint, upper.tint, mood - mood.toInt()), label = "bg")

    Column(
        modifier
            .background(lerp(Palette.Paper, bg, 0.55f))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Good morning ☀️", style = Type.Label, color = Palette.InkSoft)
        Text("How did you sleep?", style = Type.Display, color = Palette.Ink)
        if (night != null) {
            val zone = ZoneId.systemDefault()
            val from = night.start.atZone(zone).toLocalTime()
            val to = night.end.atZone(zone).toLocalTime()
            Text(
                "${hoursMinutes(night.minutes)}, from %02d:%02d to %02d:%02d".format(from.hour, from.minute, to.hour, to.minute),
                style = Type.Body,
                color = Palette.InkSoft,
            )
        }

        Box(Modifier.fillMaxWidth().padding(vertical = 18.dp).enter(0), contentAlignment = Alignment.Center) {
            Box(Modifier.size(240.dp).background(Color.White.copy(alpha = 0.55f), CircleShape))
            MochiView(Modifier.size(210.dp), mood = mood)
            BouncyEmoji(current.emoji, Modifier.align(Alignment.TopEnd).padding(end = 18.dp).floaty(amplitude = 4f))
        }
        Text(current.label, style = Type.Title, color = Palette.Ink, modifier = Modifier.align(Alignment.CenterHorizontally))
        Slider(
            value = mood,
            onValueChange = { mood = it },
            valueRange = 0f..4f,
            colors = SliderDefaults.colors(thumbColor = Palette.Ink, activeTrackColor = Palette.Ink, inactiveTrackColor = Color.White),
            modifier = Modifier.padding(top = 8.dp).semantics { stateDescription = current.label },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("😫 Rough", style = Type.Small, color = Palette.InkSoft)
            Text("Great 🤩", style = Type.Small, color = Palette.InkSoft)
        }

        Text("Anything different last night?", style = Type.Heading, color = Palette.Ink, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NightTag.entries.forEach { tag ->
                val on = tag in tags
                val chipBg by animateColorAsState(if (on) Palette.Ink else Color.White, label = "chip")
                Text(
                    "${tag.emoji} ${tag.label}",
                    style = Type.Label,
                    color = if (on) Color.White else Palette.Ink,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(chipBg)
                        .border(1.dp, if (on) Palette.Ink else Palette.Line, RoundedCornerShape(20.dp))
                        .pressable(role = Role.Checkbox) { tags = if (on) tags - tag else tags + tag }
                        .semantics { stateDescription = if (on) "Selected" else "Not selected" }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }
        Text("After two weeks we show which of these change your sleep.", style = Type.Small, color = Palette.InkSoft, modifier = Modifier.padding(top = 10.dp))

        PrimaryButton(
            "Save my morning",
            onClick = {
                vm.rateLastNight(index + 1, tags)
                burst.fire(current.confetti, Offset(0.5f, 0.85f), count = 20)
                onDone()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
        )
    }
}
