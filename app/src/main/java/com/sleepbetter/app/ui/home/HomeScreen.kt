package com.sleepbetter.app.ui.home

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

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalTime.now()
        }
    }
    val untilBed = Math.floorMod(settings.bedtimeMinute - (now.hour * 60 + now.minute), 1440)

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(todayLabel(), style = Type.Small, color = Palette.InkMuted)
                Text(greeting(now), style = Type.Display, color = Palette.Ink)
            }
            Row(
                Modifier.clip(RoundedCornerShape(22.dp)).background(Palette.Card).pressable(onClick = onFriends).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GlyphIcon(Glyph.MOON, Palette.LavenderDeep, size = 18.dp)
                Text("$steady", style = Type.Label, color = Palette.Ink)
            }
        }

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
                PrimaryButton("Wind down", onWindDown, Modifier.weight(1f), color = Palette.Moon, textColor = Palette.Ink, glyph = Glyph.MOON)
                Spacer(Modifier.width(10.dp))
                MorphPlayButton(mix.playing, vm::togglePlay, diameter = 56.dp, color = Color.White, iconColor = Palette.Ink)
            }
        }

        Row(Modifier.enter(80), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScoreCard(report.score, report.level, report.enoughData, Modifier.weight(1f), onInsights)
            LastNightCard(sessions.lastOrNull()?.minutes, sessions.takeLast(7).map { it.minutes }, Modifier.weight(1f), onInsights)
        }

        Row(Modifier.enter(140), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoCard(Modifier.weight(1f), color = Palette.Sky, onClick = onFocus) {
                MochiView(Modifier.size(64.dp), headphones = true, mood = 3.5f)
                Text("Focus", style = Type.Heading, color = Palette.Ink, modifier = Modifier.padding(top = 6.dp))
                Text("25 min with music", style = Type.Small, color = Palette.InkSoft)
            }
            BentoCard(Modifier.weight(1f), color = Palette.Rose, onClick = onWindDown) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    BreathingRings(time, still)
                }
                Text("Breathe", style = Type.Heading, color = Palette.Ink, modifier = Modifier.padding(top = 6.dp))
                Text("4-7-8 to slow down", style = Type.Small, color = Palette.InkSoft)
            }
        }

        SectionTitle("Mixes for tonight", Modifier.enter(200), action = "All mixes", onAction = onSounds)
        LazyRow(Modifier.enter(220), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(MixTemplates.featured) { t ->
                PresetCard(t, playing = mix.playing && mix.active == t.mix.sounds) { vm.playMix(t.mix) }
            }
        }
        Spacer(Modifier.height(120.dp))
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
            track = Color.White.copy(alpha = 0.6f),
        ) {
            Text(if (enough) shown.roundToInt().toString() else "–", style = Type.Title.copy(fontSize = 30.sp), color = Palette.Ink)
        }
        Spacer(Modifier.height(10.dp))
        if (enough) {
            LevelBadge(level.label, Color.White.copy(alpha = 0.7f), level.deep(), levelGlyph(level))
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
    BentoCard(modifier, color = Palette.Lavender, onClick = onClick) {
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
fun WeekBars(minutes: List<Int>, modifier: Modifier, bar: Color = Palette.LavenderDeep, goalLine: Color = Color.White) {
    Canvas(modifier.semantics { contentDescription = minutes.joinToString { hoursMinutes(it) } }) {
        val n = 7
        val gap = size.width * 0.04f
        val w = (size.width - gap * (n - 1)) / n
        val goalY = size.height * (1f - 7f / 9f)
        drawLine(goalLine, Offset(0f, goalY), Offset(size.width, goalY), 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        minutes.takeLast(n).forEachIndexed { i, m ->
            val hgt = (m / 540f).coerceIn(0.08f, 1f) * size.height
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
        drawCircle(Color.White, r * 0.22f)
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
                .background(Brush.linearGradient(if (tints.size > 1) tints else tints + Palette.Lavender)),
        ) {
            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                sounds.forEach { id ->
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        GlyphIcon(id.glyph(), id.deep(), size = 18.dp)
                    }
                }
            }
            if (playing) {
                Box(Modifier.align(Alignment.BottomEnd).padding(10.dp)) {
                    MochiView(Modifier.size(44.dp), species = Species.MOCHI, sleeping = true)
                }
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
