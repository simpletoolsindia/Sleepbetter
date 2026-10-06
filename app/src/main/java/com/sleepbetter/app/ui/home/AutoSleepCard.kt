package com.sleepbetter.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepbetter.app.data.AutoSummary
import com.sleepbetter.app.ui.components.BentoCard
import com.sleepbetter.app.ui.components.PrimaryButton
import com.sleepbetter.app.ui.components.floaty
import com.sleepbetter.app.ui.components.hoursMinutes
import com.sleepbetter.app.ui.components.pressable
import com.sleepbetter.app.ui.components.rememberReduceMotion
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Auto sleep tracking on the home screen: an invitation to turn it on, a
 * nudge to allow usage access, or last night as worked out from the phone,
 * drawn as a timeline of screen-on moments around the sleep.
 */
@Composable
fun AutoSleepCard(
    enabled: Boolean,
    hasAccess: Boolean,
    summary: AutoSummary?,
    onTurnOn: () -> Unit,
    onOpenAccess: () -> Unit,
    onTurnOff: () -> Unit,
    onRate: () -> Unit,
    onNotRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BentoCard(modifier.fillMaxWidth(), color = if (summary != null && enabled) Palette.Sky else Palette.Card) {
        when {
            !enabled -> {
                Header("📱💤", "Auto sleep tracking", "New")
                Text(
                    "Just put your phone down at night. We work out when you fell asleep and woke up from when the screen was off. Nothing leaves your phone.",
                    style = Type.Small, color = Palette.InkSoft, modifier = Modifier.padding(top = 8.dp),
                )
                PrimaryButton("Turn on ✨", onTurnOn, Modifier.padding(top = 14.dp).fillMaxWidth())
            }
            !hasAccess -> {
                Header("🔐", "One more step", null)
                Text(
                    "Allow \"Usage access\" for SleepBetter so we can see when the screen was on. It's one switch in Settings.",
                    style = Type.Small, color = Palette.InkSoft, modifier = Modifier.padding(top = 8.dp),
                )
                PrimaryButton("Show me how 👆", onOpenAccess, Modifier.padding(top = 14.dp).fillMaxWidth())
                TurnOff(onTurnOff)
            }
            summary == null -> {
                Header("🌙", "Tracking is on", null)
                Text(
                    "Put your phone down at bedtime. In the morning your night shows up here, worked out for you.",
                    style = Type.Small, color = Palette.InkSoft, modifier = Modifier.padding(top = 8.dp),
                )
                TurnOff(onTurnOff)
            }
            else -> TrackedNight(summary, onRate, onNotRight, onTurnOff)
        }
    }
}

@Composable
private fun Header(emoji: String, title: String, badge: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 26.sp, modifier = Modifier.floaty(amplitude = 2.5f))
        Spacer(Modifier.width(10.dp))
        Text(title, style = Type.Heading, color = Palette.Ink, modifier = Modifier.weight(1f))
        if (badge != null) {
            Text(
                badge, style = Type.Label, color = Palette.OnInk,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.AccentDeep)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun TurnOff(onTurnOff: () -> Unit) {
    Text(
        "Turn off auto tracking",
        style = Type.Small, color = Palette.InkMuted,
        modifier = Modifier.padding(top = 10.dp).pressable(onClick = onTurnOff).padding(vertical = 6.dp),
    )
}

@Composable
private fun TrackedNight(summary: AutoSummary, onRate: () -> Unit, onNotRight: () -> Unit, onTurnOff: () -> Unit) {
    val night = summary.night
    val zone = ZoneId.systemDefault()
    fun at(i: Instant) = i.atZone(zone).toLocalTime().let { "%02d:%02d".format(it.hour, it.minute) }
    Header("📱", "Last night, tracked for you", null)
    Text(hoursMinutes(night.minutes), style = Type.Title.copy(fontSize = 32.sp), color = Palette.Ink, modifier = Modifier.padding(top = 6.dp))
    Text("😴 ${at(night.asleep)}  →  ⏰ ${at(night.awake)}", style = Type.Body, color = Palette.InkSoft)
    Text(
        when (night.wakeUps) {
            0 -> "Slept right through, no phone checks 🌟"
            1 -> "Checked the phone once in the night"
            else -> "Checked the phone ${night.wakeUps} times in the night"
        },
        style = Type.Small, color = Palette.InkSoft, modifier = Modifier.padding(top = 2.dp),
    )
    NightTimeline(summary, Modifier.padding(top = 14.dp).fillMaxWidth().height(58.dp))
    Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PrimaryButton("How did you sleep?", onRate, Modifier.weight(1f))
        Text(
            "Not right",
            style = Type.Label, color = Palette.InkSoft,
            modifier = Modifier.pressable(onClick = onNotRight).padding(horizontal = 10.dp, vertical = 14.dp),
        )
    }
    TurnOff(onTurnOff)
}

/**
 * The evening to the morning: a track with the sleep as a glowing bar and
 * every screen-on moment as a tick, the ones inside the sleep in warm colour.
 */
@Composable
private fun NightTimeline(summary: AutoSummary, modifier: Modifier) {
    val night = summary.night
    val zone = ZoneId.systemDefault()
    val from = night.phoneDown.minus(Duration.ofHours(2))
    val to = night.pickedUp.plus(Duration.ofHours(1))
    val span = Duration.between(from, to).toMillis().toFloat()
    fun x(i: Instant, w: Float) = (Duration.between(from, i).toMillis() / span).coerceIn(0f, 1f) * w
    val still = rememberReduceMotion()
    val grow = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
    val label = "Phone down ${night.phoneDown.atZone(zone).toLocalTime()}, picked up ${night.pickedUp.atZone(zone).toLocalTime()}"
    val sleepTop = Palette.AccentDeep
    val sleepLow = Palette.Accent
    val track = Palette.veil(0.7f)
    val tick = Palette.InkMuted
    val wake = Palette.PeachDeep
    val ink = Palette.InkSoft
    Column(modifier.semantics { contentDescription = label }) {
        Canvas(Modifier.fillMaxWidth().height(36.dp)) {
            val h = size.height
            val barH = h * 0.5f
            val top = (h - barH) / 2f
            drawRoundRect(track, Offset(0f, top), Size(size.width, barH), CornerRadius(barH / 2f))
            val s = x(night.asleep, size.width)
            val e = s + (x(night.awake, size.width) - s) * grow.value
            drawRoundRect(
                Brush.horizontalGradient(listOf(sleepLow, sleepTop), startX = s, endX = e.coerceAtLeast(s + 1f)),
                Offset(s, top), Size((e - s).coerceAtLeast(barH), barH), CornerRadius(barH / 2f),
            )
            for (u in summary.uses) {
                if (u.end.isBefore(from) || u.start.isAfter(to)) continue
                val ux = x(u.start, size.width)
                val uw = (x(u.end, size.width) - ux).coerceAtLeast(3f)
                val inside = u.start.isAfter(night.phoneDown) && u.end.isBefore(night.pickedUp)
                drawRoundRect(if (inside) wake else tick, Offset(ux, 0f), Size(uw, h), CornerRadius(2f))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            fun at(i: Instant) = i.atZone(zone).toLocalTime().let { "%02d:%02d".format(it.hour, it.minute) }
            Text(at(from), style = Type.Small, color = ink, modifier = Modifier.weight(1f))
            Text("📱 screen on", style = Type.Small, color = ink)
            Text(at(to), style = Type.Small, color = ink, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
    }
}
