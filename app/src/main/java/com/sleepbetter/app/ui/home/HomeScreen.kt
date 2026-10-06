package com.sleepbetter.app.ui.home

import com.sleepbetter.app.ui.components.DinoWalker
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.ui.components.rememberAskNotifications
import com.sleepbetter.app.ui.components.BentoCard
import com.sleepbetter.app.ui.components.CircleButton
import com.sleepbetter.app.ui.components.DuskScene
import com.sleepbetter.app.ui.components.Glyph
import com.sleepbetter.app.ui.components.GlyphIcon
import com.sleepbetter.app.ui.components.LevelBadge
import com.sleepbetter.app.ui.components.MochiView
import com.sleepbetter.app.ui.components.MorphPlayButton
import com.sleepbetter.app.ui.components.PrimaryButton
import com.sleepbetter.app.ui.components.Ring
import com.sleepbetter.app.ui.components.SectionTitle
import com.sleepbetter.app.ui.components.Species
import com.sleepbetter.app.ui.components.durationLabel
import com.sleepbetter.app.ui.components.enter
import com.sleepbetter.app.ui.components.glyph
import com.sleepbetter.app.ui.components.greeting
import com.sleepbetter.app.ui.components.hoursMinutes
import com.sleepbetter.app.ui.components.pressable
import com.sleepbetter.app.ui.components.rememberClock
import com.sleepbetter.app.ui.components.rememberCountUp
import com.sleepbetter.app.ui.components.rememberEngineFrame
import com.sleepbetter.app.ui.components.rememberReduceMotion
import com.sleepbetter.app.ui.components.todayLabel
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.sleepbetter.app.ui.components.EmojiChip
import com.sleepbetter.app.ui.components.LocalBurst
import com.sleepbetter.app.ui.components.MochiSays
import com.sleepbetter.app.ui.components.species
import com.sleepbetter.app.ui.components.isDino
import com.sleepbetter.app.ui.components.floaty
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.app.ui.theme.deep
import com.sleepbetter.app.ui.theme.tint
import com.sleepbetter.core.mix.MixTemplate
import com.sleepbetter.core.mix.MixTemplates
import com.sleepbetter.core.sleep.RiskLevel
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onWindDown: () -> Unit,
    onFocus: () -> Unit,
    onSounds: () -> Unit,
    onInsights: () -> Unit,
    onFriends: () -> Unit,
    onCheckIn: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val mix by vm.mix.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val frame by rememberEngineFrame(vm.engine)
    val time by rememberClock()
    val still = rememberReduceMotion()
    val report = remember(sessions, settings) { vm.repository.report() }
    val steady = remember(sessions, settings) { vm.repository.totalSteadyNights() }
    val lastAuto by vm.lastAuto.collectAsStateWithLifecycle()
    val usageAccess by vm.usageAccess.collectAsStateWithLifecycle()

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalTime.now()
        }
    }
    var picking by remember { mutableStateOf(false) }
    if (picking) ThemeSheet(Palette.theme, vm::setTheme, Palette.appearance, vm::setAppearance) { picking = false }
    val untilBed = Math.floorMod(settings.bedtimeMinute - (now.hour * 60 + now.minute), 1440)

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(todayLabel(), style = Type.Small, color = Palette.InkMuted)
                    WavingEmoji(if (now.hour in 5..17) "👋" else "🌙", Modifier.padding(start = 6.dp))
                }
                Text(greeting(now), style = Type.Display, color = Palette.Ink)
            }
            CircleButton(Glyph.PALETTE, "Colour theme", { picking = true }, size = 44.dp)
            Spacer(Modifier.width(8.dp))
            // Steady bedtimes so far: a little flame that pulses.
            EmojiChip("🔥", "$steady", Modifier.pressable(onClick = onFriends))
        }

        // Pico chats: bedtime countdown, last night, a tip.
        val lastNight = sessions.lastOrNull()
        val lines = remember(untilBed, lastNight, steady) {
            buildList {
                add("Bedtime in ${durationLabel(untilBed)} 🌙")
                if (lastNight != null) add("Last night you slept ${hoursMinutes(lastNight.minutes)} 😴")
                if (steady > 0) add("$steady steady bedtimes so far! 🔥")
                add("Pick a mix below and relax 🎧")
                add("Dim the lights an hour before bed 💡")
                add("Lulu says: no coffee after 2 pm ☕🚫")
            }
        }
        // Friends you have take turns talking, dinos first.
        val speakers = remember(sessions, settings) {
            vm.repository.visitorProgress().unlocked.map { it.species() }.sortedByDescending { it.isDino }.take(6)
        }
        MochiSays(lines, Modifier.enter(40), speakers = speakers)

        // Tonight: the dusk scene shows exactly what is in the mix.
        Box(
            Modifier
                .enter(0)
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(32.dp)),
        ) {
            DuskScene(mix.active, frame, time, Modifier.fillMaxSize(), still = still)
            Column(Modifier.padding(22.dp)) {
                Text("Bedtime", style = Type.Label, color = Color.White.copy(alpha = 0.8f))
                Text(settings.bedtimeLabel, style = Type.Numeral, color = Color.White)
                Text("in ${durationLabel(untilBed)}", style = Type.Body, color = Color.White.copy(alpha = 0.85f))
            }
            Row(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryButton("Wind down", onWindDown, Modifier.weight(1f), color = Palette.Moon, textColor = Palette.DarkInk, glyph = Glyph.MOON)
                Spacer(Modifier.width(10.dp))
                MorphPlayButton(mix.playing, vm::togglePlay, diameter = 56.dp, color = Color.White, iconColor = Palette.DarkInk)
            }
        }

        Row(Modifier.enter(80), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScoreCard(report.score, report.level, report.enoughData, Modifier.weight(1f), onInsights)
            LastNightCard(sessions.lastOrNull()?.minutes, sessions.takeLast(7).map { it.minutes }, Modifier.weight(1f), onInsights)
        }

        var guide by remember { mutableStateOf(false) }
        val askNotifications = rememberAskNotifications()
        if (guide) UsageAccessSheet(onOpenSettings = { guide = false; vm.openUsageAccess() }, onDismiss = { guide = false })
        // Back from Settings with access allowed: a little celebration.
        val burst = LocalBurst.current
        var hadAccess by remember { mutableStateOf(usageAccess) }
        LaunchedEffect(usageAccess) {
            if (usageAccess && !hadAccess && settings.autoTrack) burst.fire(listOf("🎉", "😴", "✨", "🌙"), Offset(0.5f, 0.6f), count = 16)
            hadAccess = usageAccess
        }
        AutoSleepCard(
            enabled = settings.autoTrack,
            hasAccess = usageAccess,
            summary = lastAuto,
            onTurnOn = {
                askNotifications() // for the good-morning summary
                if (vm.turnOnAutoSleep()) guide = true
            },
            onOpenAccess = { guide = true },
            onTurnOff = vm::turnOffAutoSleep,
            onRate = onCheckIn,
            onNotRight = vm::dismissAutoNight,
            modifier = Modifier.enter(110),
        )

        Row(Modifier.enter(140), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoCard(Modifier.weight(1f), color = Palette.Sky, onClick = onFocus) {
                MochiView(Modifier.size(64.dp), headphones = true, mood = 3.5f)
                Text("Focus 🎧", style = Type.Heading, color = Palette.Ink, modifier = Modifier.padding(top = 6.dp))
                Text("25 min with music", style = Type.Small, color = Palette.InkSoft)
            }
            BentoCard(Modifier.weight(1f), color = Palette.Rose, onClick = onWindDown) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    BreathingRings(time, still)
                }
                Text("Breathe 🫧", style = Type.Heading, color = Palette.Ink, modifier = Modifier.padding(top = 6.dp))
                Text("4-7-8 to slow down", style = Type.Small, color = Palette.InkSoft)
            }
        }

        SectionTitle("Mixes for tonight", Modifier.enter(200), action = "All mixes", onAction = onSounds)
        LazyRow(Modifier.enter(220), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(MixTemplates.featured) { t ->
                val burst = LocalBurst.current
                PresetCard(t, playing = mix.playing && mix.active == t.mix.sounds) {
                    vm.playMix(t.mix)
                    burst.fire(listOf(t.emoji, "✨", "🎶"), Offset(0.5f, 0.82f), count = 12)
                }
            }
        }
        // Pico strolls along the bottom and waves at each end.
        DinoWalker(Species.PICO, Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun ScoreCard(score: Int, level: RiskLevel, enough: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shown = rememberCountUp(if (enough) score.toFloat() else 0f)
    BentoCard(modifier, color = if (enough) level.tint() else Palette.Card, onClick = onClick) {
        Text("Sleep score", style = Type.Label, color = Palette.InkSoft)
        Ring(
            progress = shown / 100f,
            color = if (enough) level.deep() else Palette.Line,
            modifier = Modifier.padding(top = 10.dp).size(92.dp),
            track = Palette.veil(0.6f),
        ) {
            Text(if (enough) shown.roundToInt().toString() else "–", style = Type.Title.copy(fontSize = 30.sp), color = Palette.Ink)
        }
        Spacer(Modifier.height(10.dp))
        if (enough) {
            LevelBadge(level.label, Palette.veil(0.7f), level.deep(), levelGlyph(level))
        } else {
            Text("After 3 nights", style = Type.Small, color = Palette.InkMuted)
        }
    }
}

fun levelGlyph(level: RiskLevel): Glyph = when (level) {
    RiskLevel.GOOD -> Glyph.CHECK
    RiskLevel.MEDIUM -> Glyph.MINUS
    RiskLevel.AT_RISK -> Glyph.SPARK
}

@Composable
private fun LastNightCard(minutes: Int?, week: List<Int>, modifier: Modifier, onClick: () -> Unit) {
    BentoCard(modifier, color = Palette.Accent, onClick = onClick) {
        Text("Last night", style = Type.Label, color = Palette.InkSoft)
        if (minutes == null) {
            Text("No nights yet", style = Type.Title, color = Palette.Ink, modifier = Modifier.padding(top = 10.dp))
            Text("Start sleep mode at bedtime and we'll keep track.", style = Type.Small, color = Palette.InkSoft, modifier = Modifier.padding(top = 4.dp))
        } else {
            Text(hoursMinutes(minutes), style = Type.Title.copy(fontSize = 30.sp), color = Palette.Ink, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(12.dp))
            WeekBars(week, Modifier.fillMaxWidth().height(56.dp))
            Text("Last ${week.size} nights", style = Type.Small, color = Palette.InkSoft, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** Rounded pill bars; full height = 9 hours, the dashed line marks 7 hours. */
@Composable
fun WeekBars(minutes: List<Int>, modifier: Modifier, bar: Color = Palette.AccentDeep, goalLine: Color = Palette.veil(0.9f)) {
    // Bars grow up one after another when the card appears.
    val still = rememberReduceMotion()
    val grow = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    Canvas(modifier.semantics { contentDescription = minutes.joinToString { hoursMinutes(it) } }) {
        val n = 7
        val gap = size.width * 0.04f
        val w = (size.width - gap * (n - 1)) / n
        val goalY = size.height * (1f - 7f / 9f)
        drawLine(goalLine, Offset(0f, goalY), Offset(size.width, goalY), 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        minutes.takeLast(n).forEachIndexed { i, m ->
            val p = ((grow.value * 1.6f) - i * 0.09f).coerceIn(0f, 1f)
            val ease = 1f - (1f - p) * (1f - p)
            val hgt = (m / 540f).coerceIn(0.08f, 1f) * size.height * ease.coerceAtLeast(0.02f)
            drawRoundRect(bar, Offset(i * (w + gap), size.height - hgt), Size(w, hgt), CornerRadius(w / 2f))
        }
    }
}

@Composable
private fun BreathingRings(time: Float, still: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val phase = if (still) 0.5f else 0.5f + 0.5f * kotlin.math.sin(time * 0.9f)
        val r = size.minDimension / 2f
        drawCircle(Palette.PeachDeep.copy(alpha = 0.18f), r * (0.6f + 0.4f * phase))
        drawCircle(Palette.PeachDeep.copy(alpha = 0.3f), r * (0.4f + 0.3f * phase))
        drawCircle(Palette.Card, r * 0.22f)
    }
}

@Composable
private fun PresetCard(template: MixTemplate, playing: Boolean, onClick: () -> Unit) {
    val sounds = template.mix.layers.map { it.sound }
    val tints = sounds.map { it.tint() }
    Column(
        Modifier
            .width(168.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Palette.Card)
            .pressable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(104.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(if (tints.size > 1) tints else tints + Palette.Accent)),
        ) {
            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                sounds.forEach { id ->
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Palette.Card), contentAlignment = Alignment.Center) {
                        GlyphIcon(id.glyph(), id.deep(), size = 18.dp)
                    }
                }
            }
            // The mix's emoji as a floating sticker.
            Text(
                template.emoji,
                fontSize = 30.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp).floaty(amplitude = 3f, phase = template.mix.name.length / 10f),
            )
            if (playing) {
                Box(Modifier.align(Alignment.BottomEnd).padding(10.dp)) {
                    MochiView(Modifier.size(44.dp), species = Species.MOCHI, sleeping = true)
                }
                EqualizerBadge(Modifier.align(Alignment.TopEnd).padding(10.dp))
            }
        }
        Text(template.mix.name, style = Type.Heading, color = Palette.Ink, maxLines = 1, modifier = Modifier.padding(start = 6.dp, top = 10.dp))
        Text(
            template.blurb,
            style = Type.Small,
            color = Palette.InkMuted,
            maxLines = 1,
            modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
        )
    }
}

/** An emoji that waves hello every few seconds. */
@Composable
private fun WavingEmoji(emoji: String, modifier: Modifier = Modifier) {
    val still = rememberReduceMotion()
    val angle = remember { Animatable(0f) }
    LaunchedEffect(still) {
        if (still) return@LaunchedEffect
        while (true) {
            repeat(2) {
                angle.animateTo(18f, tween(140))
                angle.animateTo(-10f, tween(140))
            }
            angle.animateTo(0f, tween(160))
            delay(3200)
        }
    }
    Text(emoji, fontSize = 14.sp, modifier = modifier.graphicsLayer { rotationZ = angle.value; transformOrigin = TransformOrigin(0.7f, 0.9f) })
}

/** Three little bars that bounce while a mix is playing. */
@Composable
private fun EqualizerBadge(modifier: Modifier = Modifier) {
    val time by rememberClock()
    val still = rememberReduceMotion()
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(Palette.Card).padding(horizontal = 7.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(3) { i ->
            val h = if (still) 0.6f else 0.35f + 0.65f * kotlin.math.abs(kotlin.math.sin(time * (5f + i * 1.7f) + i))
            Box(Modifier.width(3.dp).height((12 * h).dp).clip(RoundedCornerShape(2.dp)).background(Palette.AccentDeep))
        }
    }
}
