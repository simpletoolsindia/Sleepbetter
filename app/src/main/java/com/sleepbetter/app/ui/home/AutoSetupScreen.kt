package com.sleepbetter.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepbetter.app.ui.components.CircleButton
import com.sleepbetter.app.ui.components.Glyph
import com.sleepbetter.app.ui.components.LocalBurst
import com.sleepbetter.app.ui.components.MochiView
import com.sleepbetter.app.ui.components.PrimaryButton
import com.sleepbetter.app.ui.components.enter
import com.sleepbetter.app.ui.components.floaty
import com.sleepbetter.app.ui.components.pressable
import com.sleepbetter.app.ui.components.rememberClock
import com.sleepbetter.app.ui.components.rememberReduceMotion
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import kotlin.math.sin

/**
 * Full-screen setup for auto sleep tracking. Before access: what you get,
 * then an animated preview of the exact switch to flip in Settings, with one
 * pinned button. After: a success screen with what was found.
 */
@Composable
fun AutoSetupScreen(
    hasAccess: Boolean,
    nightsFound: Int,
    onAllow: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    restricted: Boolean = false,
    onOpenAppInfo: () -> Unit = {},
) {
    // Arriving back from Settings with access: confetti, once.
    val burst = LocalBurst.current
    var wasAllowed by remember { mutableStateOf(hasAccess) }
    LaunchedEffect(hasAccess) {
        if (hasAccess && !wasAllowed) burst.fire(listOf("🎉", "😴", "✨", "🌙", "⭐"), Offset(0.5f, 0.35f), count = 22)
        wasAllowed = hasAccess
    }

    Column(modifier.background(Palette.Paper)) {
        // Top bar: close, and where you are in the setup.
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleButton(Glyph.BACK, "Close", onClose, size = 44.dp)
            Spacer(Modifier.weight(1f))
            StepPill(if (hasAccess) 2 else 1)
        }

        AnimatedContent(
            targetState = hasAccess,
            transitionSpec = { (fadeIn(tween(380)) + scaleIn(spring(0.75f, 300f), initialScale = 0.96f)) togetherWith fadeOut(tween(200)) },
            label = "setup",
            modifier = Modifier.weight(1f),
        ) { allowed ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Hero(allowed, Modifier.enter(0))
                Spacer(Modifier.height(22.dp))
                if (allowed) Success(nightsFound) else Intro(restricted, onOpenAppInfo)
                Spacer(Modifier.height(24.dp))
            }
        }

        // One clear action, always in reach of the thumb.
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 16.dp)) {
            if (hasAccess) {
                PrimaryButton("Done", onClose, Modifier.fillMaxWidth(), glyph = Glyph.CHECK)
            } else {
                PrimaryButton("Allow access", onAllow, Modifier.fillMaxWidth(), color = Palette.AccentDeep, textColor = if (Palette.darkness > 0.5f) Palette.DarkInk else Color.White, glyph = Glyph.SPARK)
                Text(
                    "Not now",
                    style = Type.Label, color = Palette.InkMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp).pressable(onClick = onClose).padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun StepPill(step: Int) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).background(Palette.Card).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(2) { i ->
            val on = i < step
            val c by animateColorAsState(if (on) Palette.AccentDeep else Palette.Line, label = "dot")
            Box(Modifier.size(width = if (i == step - 1) 18.dp else 7.dp, height = 7.dp).clip(CircleShape).background(c))
        }
        Text(if (step == 2) "All set" else "Step 1 of 2", style = Type.Small, color = Palette.InkSoft, modifier = Modifier.padding(start = 2.dp))
    }
}

/**
 * The night sky with Pico asleep and a phone beside them. Before access the
 * phone's screen glows on and off (the signal we read); after, it rests dark
 * and a big check appears.
 */
@Composable
private fun Hero(allowed: Boolean, modifier: Modifier) {
    val time by rememberClock()
    val still = rememberReduceMotion()
    val t = if (still) 2f else time
    Box(
        modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.verticalGradient(listOf(Palette.Night, Palette.DuskMid))),
    ) {
        // Twinkling stars and a soft moon glow.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(listOf(Color(0x55FFE6A3), Color.Transparent), center = Offset(size.width * 0.3f, size.height * 0.55f), radius = size.width * 0.45f),
                radius = size.width * 0.45f, center = Offset(size.width * 0.3f, size.height * 0.55f),
            )
            val stars = listOf(0.06f to 0.32f, 0.22f to 0.36f, 0.6f to 0.08f, 0.62f to 0.18f, 0.86f to 0.12f, 0.93f to 0.42f, 0.55f to 0.36f, 0.15f to 0.7f)
            stars.forEachIndexed { i, (x, y) ->
                val a = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * (1.2f + i * 0.31f) + i))
                drawCircle(Color.White.copy(alpha = a), radius = (1.4f + (i % 3)).dp.toPx(), center = Offset(x * size.width, y * size.height))
            }
        }
        // Pico, asleep.
        MochiView(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 6.dp).size(170.dp), sleeping = true)
        Text("z", fontSize = 26.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.align(Alignment.TopStart).padding(start = 128.dp, top = 80.dp).floaty(5f, 2400))
        Text("z", fontSize = 18.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.TopStart).padding(start = 152.dp, top = 62.dp).floaty(5f, 2900, 0.5f))

        // The phone: glowing on and off, or resting with a check.
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 30.dp)) {
            PhoneMock(allowed, t)
        }

        // A glassy chip with the kind of result you'll get.
        Row(
            Modifier.align(Alignment.TopStart).padding(14.dp)
                .clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.14f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (allowed) "✅ Tracking on" else "😴 23:34  →  ⏰ 07:04", style = Type.Label, color = Color.White)
        }
    }
}

@Composable
private fun PhoneMock(allowed: Boolean, t: Float) {
    // Screen on for a moment, then off for longer: a night in miniature.
    val glow = if (allowed) 0f else ((sin(t * 1.3f) + 0.2f).coerceIn(0f, 1f))
    val check = remember { Animatable(0f) }
    LaunchedEffect(allowed) { check.animateTo(if (allowed) 1f else 0f, tween(700, easing = FastOutSlowInEasing)) }
    val accent = Palette.Accent
    Canvas(Modifier.size(width = 86.dp, height = 150.dp).semantics { contentDescription = "A phone at night" }) {
        val r = CornerRadius(16.dp.toPx())
        drawRoundRect(Color(0xFF15122B), size = size, cornerRadius = r)
        val inset = 5.dp.toPx()
        val screen = Size(size.width - inset * 2, size.height - inset * 2)
        drawRoundRect(Color(0xFF221D45), Offset(inset, inset), screen, CornerRadius(12.dp.toPx()))
        if (glow > 0f) {
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFFB8A6FF).copy(alpha = glow), Color(0xFF7C68E6).copy(alpha = glow * 0.8f))),
                Offset(inset, inset), screen, CornerRadius(12.dp.toPx()),
            )
            // A couple of "app" lines on the lit screen.
            repeat(3) { i ->
                drawRoundRect(Color.White.copy(alpha = 0.55f * glow), Offset(inset * 3, inset * 4 + i * 14.dp.toPx()), Size(screen.width * (0.7f - i * 0.15f), 6.dp.toPx()), CornerRadius(3.dp.toPx()))
            }
        }
        drawRoundRect(Color.White.copy(alpha = 0.25f), size = size, cornerRadius = r, style = Stroke(1.5.dp.toPx()))
        if (check.value > 0f) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(accent.copy(alpha = check.value), radius = 26.dp.toPx() * check.value, center = c)
            val p = check.value
            val a = Offset(c.x - 10.dp.toPx(), c.y)
            val b = Offset(c.x - 3.dp.toPx(), c.y + 7.dp.toPx())
            val e = Offset(c.x + 11.dp.toPx(), c.y - 8.dp.toPx())
            val w = 4.dp.toPx()
            val first = (p * 2f).coerceIn(0f, 1f)
            val second = (p * 2f - 1f).coerceIn(0f, 1f)
            drawLine(Color(0xFF2A2140), a, a + (b - a) * first, w, StrokeCap.Round)
            if (second > 0f) drawLine(Color(0xFF2A2140), b, b + (e - b) * second, w, StrokeCap.Round)
        }
    }
}

@Composable
private fun Intro(restricted: Boolean, onOpenAppInfo: () -> Unit) {
    Text("Sleep tracking,\non autopilot", style = Type.Display, color = Palette.Ink, modifier = Modifier.enter(60))
    Text(
        "Put your phone down and sleep. SleepBetter works out your bedtime and wake-up from when your screen was off.",
        style = Type.Body, color = Palette.InkSoft, modifier = Modifier.padding(top = 8.dp).enter(100),
    )

    // What you get, as a bento row.
    Row(Modifier.padding(top = 18.dp).fillMaxWidth().enter(140), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Benefit("🌙", "Bedtime & wake-up", Palette.Accent, Modifier.weight(1f))
        Benefit("📱", "Night-time phone checks", Palette.Sky, Modifier.weight(1f))
        Benefit("🔒", "Never leaves your phone", Palette.Sage, Modifier.weight(1f))
    }

    if (restricted) Unlock(onOpenAppInfo, Modifier.padding(top = 22.dp).enter(170))
    Text(if (restricted) "Then flip the switch" else "Just one switch to flip", style = Type.Title, color = Palette.Ink, modifier = Modifier.padding(top = 26.dp).enter(180))
    Text("Tap Allow access, then turn this on for SleepBetter:", style = Type.Body, color = Palette.InkSoft, modifier = Modifier.padding(top = 4.dp).enter(200))
    SettingsPreview(Modifier.padding(top = 12.dp).enter(220))
    FindIt(Modifier.padding(top = 14.dp).enter(260))
}

@Composable
private fun Benefit(emoji: String, text: String, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).background(color).padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(Palette.veil(0.7f)), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 19.sp)
        }
        Text(text, style = Type.Label, color = Palette.Ink, modifier = Modifier.padding(top = 10.dp))
    }
}

/**
 * A look-alike of Android's Usage access row, playing on a loop: a finger
 * comes in, taps, the switch turns on, holds, and it starts again. Shows
 * exactly what to do on the next screen.
 */
@Composable
private fun SettingsPreview(modifier: Modifier) {
    val still = rememberReduceMotion()
    val loop by rememberInfiniteTransition(label = "preview").animateFloat(
        0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart), label = "loop",
    )
    val p = if (still) 0.6f else loop
    val fingerIn = ((p - 0.05f) / 0.3f).coerceIn(0f, 1f).let { 1f - (1f - it) * (1f - it) }
    val tapped = p in 0.4f..0.92f
    val press = if (p in 0.36f..0.44f) 0.85f else 1f
    val fade = if (p > 0.92f) 1f - (p - 0.92f) / 0.08f else 1f
    val on = if (tapped) ((p - 0.4f) / 0.08f).coerceIn(0f, 1f) else 0f

    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Palette.Card)
            .border(1.dp, Palette.Line, RoundedCornerShape(24.dp))
            .semantics { contentDescription = "In Settings, turn on Permit usage access for SleepBetter" },
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Settings  ›  Usage access", style = Type.Small, color = Palette.InkMuted)
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Color(0xFF2B2170), Color(0xFF7A5BEA)))), contentAlignment = Alignment.Center) {
                    MochiView(Modifier.size(38.dp), sleeping = true)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("SleepBetter", style = Type.Heading, color = Palette.Ink)
                    Text("Permit usage access", style = Type.Small, color = Palette.InkSoft)
                }
                FakeSwitch(on)
            }
        }
        // The finger, sliding in from below right and tapping the switch.
        Text(
            "👆",
            fontSize = 30.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-10).dp + (40 * (1f - fingerIn)).dp, y = 8.dp + (40 * (1f - fingerIn)).dp)
                .graphicsLayer { scaleX = press; scaleY = press; alpha = fingerIn * fade },
        )
    }
}

@Composable
private fun FakeSwitch(on: Float) {
    val track = androidx.compose.ui.graphics.lerp(Palette.Line, Palette.AccentDeep, on)
    Box(Modifier.size(width = 52.dp, height = 30.dp).clip(CircleShape).background(track).padding(4.dp)) {
        Box(Modifier.offset(x = (22 * on).dp).size(22.dp).clip(CircleShape).background(Color.White))
    }
}

/**
 * Android 13+ greys out Usage access ("Controlled by restricted setting") for
 * apps installed from a file. One extra unlock on the app's info page fixes it.
 */
@Composable
private fun Unlock(onOpenAppInfo: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Palette.Peach).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔓", fontSize = 24.sp)
            Column(Modifier.padding(start = 10.dp)) {
                Text("First, unlock the switch", style = Type.Heading, color = Palette.Ink)
                Text("Android blocks it for apps installed from a file and says \"Controlled by restricted setting\".", style = Type.Small, color = Palette.InkSoft)
            }
        }
        MiniStep(1, "Tap Open App info below")
        MiniStep(2, "Tap ⋮ at the top right")
        MiniStep(3, "Choose Allow restricted settings and confirm")
        Text(
            "Don't see ⋮ option? Try the switch in Usage access once first, then come back.",
            style = Type.Small, color = Palette.InkSoft,
        )
        Row(
            Modifier.clip(RoundedCornerShape(18.dp)).background(Palette.Ink).pressable(onClick = onOpenAppInfo).padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Open App info  ›", style = Type.Label, color = Palette.OnInk)
        }
    }
}

@Composable
private fun MiniStep(n: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(Palette.veil(0.8f)), contentAlignment = Alignment.Center) {
            Text("$n", style = Type.Small, color = Palette.Ink)
        }
        Text(text, style = Type.Body, color = Palette.Ink, modifier = Modifier.padding(start = 10.dp))
    }
}

/** Where the switch lives, picked by phone brand. */
@Composable
private fun FindIt(modifier: Modifier) {
    val brands = listOf(
        "Pixel" to "Settings › Apps › Special app access › Usage access",
        "Samsung" to "Settings › Apps › ⋮ › Special access › Usage data access",
        "Xiaomi" to "Settings › Passwords & security › Privacy › Special app access › Apps with usage access",
        "OnePlus" to "Settings › Apps › Special app access › Usage access",
        "Vivo" to "Settings › Apps › Special access › Usage access",
    )
    var open by remember { mutableStateOf(false) }
    var pick by remember { mutableStateOf(0) }
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Palette.Card)) {
        Row(Modifier.fillMaxWidth().pressable { open = !open }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🧭", fontSize = 18.sp)
            Text("Can't find the switch?", style = Type.Label, color = Palette.Ink, modifier = Modifier.weight(1f).padding(start = 10.dp))
            Text(if (open) "–" else "+", style = Type.Title, color = Palette.InkSoft)
        }
        if (open) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                brands.forEachIndexed { i, (name, _) ->
                    val selected = i == pick
                    Text(
                        name, style = Type.Label, color = if (selected) Palette.OnInk else Palette.Ink,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (selected) Palette.Ink else Palette.veil(0.6f))
                            .pressable { pick = i }.padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
            Text(brands[pick].second, style = Type.Body, color = Palette.Ink, modifier = Modifier.padding(16.dp))
            Text("Or search Settings for \"usage access\".", style = Type.Small, color = Palette.InkMuted, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
        }
    }
}

@Composable
private fun Success(nightsFound: Int) {
    Text("You're all set 🎉", style = Type.Display, color = Palette.Ink, modifier = Modifier.enter(60))
    Text(
        if (nightsFound > 0) "We found ${if (nightsFound == 1) "1 night" else "$nightsFound nights"} from this week already. Each new night appears on Home in the morning."
        else "Your first night appears on Home tomorrow morning. Just put your phone down at bedtime.",
        style = Type.Body, color = Palette.InkSoft, modifier = Modifier.padding(top = 8.dp).enter(100),
    )
    Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Next("🌙", "Sleep as usual", "No buttons to press at bedtime.", Modifier.enter(140))
        Next("☀️", "Good-morning summary", "A notification with how long you slept.", Modifier.enter(180))
        Next("✍️", "Not right?", "Tap \"Not right\" on the card and that night is removed.", Modifier.enter(220))
    }
}

@Composable
private fun Next(emoji: String, title: String, text: String, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Palette.Card).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Palette.Accent), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 20.sp) }
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = Type.Heading, color = Palette.Ink)
            Text(text, style = Type.Small, color = Palette.InkSoft)
        }
    }
}
