package com.sleepbetter.app.ui.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.ui.components.BentoCard
import com.sleepbetter.app.ui.components.Glyph
import com.sleepbetter.app.ui.components.GlyphIcon
import com.sleepbetter.app.ui.components.LevelBadge
import com.sleepbetter.app.ui.components.MochiAndToffee
import com.sleepbetter.app.ui.components.MochiView
import com.sleepbetter.app.ui.components.PrimaryButton
import com.sleepbetter.app.ui.components.Ring
import com.sleepbetter.app.ui.components.SegmentedTabs
import com.sleepbetter.app.ui.components.enter
import com.sleepbetter.app.ui.components.hoursMinutes
import com.sleepbetter.app.ui.components.rememberCountUp
import com.sleepbetter.app.ui.components.species
import com.sleepbetter.app.ui.home.levelGlyph
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.app.ui.theme.deep
import com.sleepbetter.app.ui.theme.tint
import com.sleepbetter.core.sleep.SleepSession
import com.sleepbetter.core.sleep.Suggestions
import com.sleepbetter.core.sleep.VisitorRules
import kotlinx.coroutines.delay
import java.time.ZoneId
import kotlin.math.roundToInt

private enum class Range(val label: String) { NIGHT("Last night"), WEEK("This week") }

/** Insights: last night or the week at a glance, your level, and what to try next. */
@Composable
fun InsightsScreen(vm: AppViewModel, onWindDown: () -> Unit, onFriends: () -> Unit, modifier: Modifier = Modifier) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var range by rememberSaveable { mutableStateOf(Range.NIGHT) }
    val night = sessions.lastOrNull()

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Insights 📊", style = Type.Display, color = Palette.Ink, modifier = Modifier.padding(top = 12.dp))
        if (night == null) {
            BentoCard(Modifier.fillMaxWidth().enter(0), color = Palette.Accent) {
                MochiAndToffee(Modifier.width(200.dp).align(Alignment.CenterHorizontally), sleeping = true)
                Text("Your first insights appear after your first night.", style = Type.Title, color = Palette.Ink, modifier = Modifier.padding(top = 10.dp))
                Text("Start sleep mode at bedtime and swipe up when you wake. The rest is automatic.", style = Type.Body, color = Palette.InkSoft, modifier = Modifier.padding(top = 6.dp))
                PrimaryButton("Wind down", onWindDown, Modifier.fillMaxWidth().padding(top = 16.dp), glyph = Glyph.MOON)
            }
            Spacer(Modifier.height(120.dp))
            return@Column
        }
        val report = remember(sessions, settings) { vm.repository.report() }
        val steady = remember(sessions, settings) { vm.repository.totalSteadyNights() }
        val arrived = remember(steady) { VisitorRules.arrivedAt(steady) }
        val tips = remember(sessions, settings) { Suggestions.forReport(report, sessions.takeLast(7), settings.bedtimeLabel) }

        if (arrived != null) {
            val pop = remember { Animatable(0f) }
            LaunchedEffect(arrived) {
                delay(250)
                pop.animateTo(1f, spring(0.4f, 200f))
            }
            BentoCard(Modifier.fillMaxWidth(), color = Palette.Butter, onClick = onFriends) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MochiView(
                        Modifier.size(84.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationZ = (1f - pop.value) * -20f },
                        species = arrived.species(),
                        mood = 4f,
                    )
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("${arrived.displayName} moved in", style = Type.Title, color = Palette.Ink)
                        Text("Steady bedtimes brought a new friend. Say hello.", style = Type.Small, color = Palette.InkSoft)
                    }
                }
            }
        }

        SegmentedTabs(Range.entries.toList(), range, { it.label }, { range = it }, Modifier.fillMaxWidth())

        when (range) {
            Range.NIGHT -> NightView(night)
            Range.WEEK -> WeekView(sessions.takeLast(7), report.averageMinutes, report.steadyNights, settings.bedtimeLabel)
        }

        // Level: score ring + word + icon, never colour alone.
        val shown = rememberCountUp(if (report.enoughData) report.score.toFloat() else 0f)
        BentoCard(Modifier.fillMaxWidth(), color = if (report.enoughData) report.level.tint() else Palette.Card) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ring(shown / 100f, if (report.enoughData) report.level.deep() else Palette.Line, Modifier.size(96.dp), track = Color.White.copy(alpha = 0.6f)) {
                    Text(if (report.enoughData) shown.roundToInt().toString() else "–", style = Type.Title.copy(fontSize = 30.sp), color = Palette.Ink)
                }
                Column(Modifier.padding(start = 16.dp)) {
                    Text("Sleep score", style = Type.Label, color = Palette.InkSoft)
                    if (report.enoughData) {
                        LevelBadge(report.level.label, Color.White.copy(alpha = 0.7f), report.level.deep(), levelGlyph(report.level), Modifier.padding(top = 6.dp))
                        Text("${report.steadyNights} steady bedtimes this week", style = Type.Small, color = Palette.InkSoft, modifier = Modifier.padding(top = 6.dp))
                    } else {
                        Text("Log ${3 - report.nights} more nights to see your level", style = Type.Body, color = Palette.Ink, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        Text("Try next", style = Type.Title, color = Palette.Ink)
        tips.forEachIndexed { i, tip ->
            BentoCard(Modifier.fillMaxWidth().enter(60 * i)) {
                Row {
                    Box(Modifier.size(40.dp).background(listOf(Palette.Accent, Palette.Sky, Palette.Rose, Palette.Sage)[i % 4], CircleShape), contentAlignment = Alignment.Center) {
                        GlyphIcon(Glyph.SPARK, Palette.Ink, size = 18.dp)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(tip.title, style = Type.Heading, color = Palette.Ink)
                        Text(tip.body, style = Type.Body, color = Palette.InkSoft, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
        Text(Suggestions.DISCLAIMER, style = Type.Small, color = Palette.InkMuted)
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun NightView(night: SleepSession) {
    val zone = ZoneId.systemDefault()
    val from = night.start.atZone(zone).toLocalTime()
    val to = night.end.atZone(zone).toLocalTime()
    val minutes = rememberCountUp(night.minutes.toFloat()).roundToInt()
    BentoCard(Modifier.fillMaxWidth(), color = Palette.Accent) {
        Text("You slept", style = Type.Label, color = Palette.InkSoft)
        Text(hoursMinutes(minutes), style = Type.Numeral, color = Palette.Ink)
        Text("%02d:%02d to %02d:%02d".format(from.hour, from.minute, to.hour, to.minute), style = Type.Body, color = Palette.InkSoft)
        Text("Your night, estimated", style = Type.Heading, color = Palette.Ink, modifier = Modifier.padding(top = 16.dp))
        Text("From your sleep times. Connect a watch for measured stages.", style = Type.Small, color = Palette.InkSoft)
        EstimatedCycles(night.minutes, Modifier.fillMaxWidth().height(120.dp).padding(top = 10.dp))
    }
}

/**
 * Estimated 90-minute cycles as rounded blocks: deep sleep heavier early,
 * REM longer towards morning. Clearly an estimate, not a measurement.
 */
@Composable
private fun EstimatedCycles(minutes: Int, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val cycles = (minutes / 90f).coerceIn(1f, 6f)
    Canvas(modifier.semantics { contentDescription = "About ${cycles.roundToInt()} estimated sleep cycles" }) {
        val rows = listOf("Light", "Deep", "REM")
        val labelW = 46.dp.toPx()
        val rowH = size.height / 3f
        rows.forEachIndexed { r, name ->
            drawText(measurer, name, Offset(0f, r * rowH + rowH / 2f - 9.dp.toPx()), style = Type.Small.copy(color = Palette.InkSoft))
        }
        val w = size.width - labelW
        val n = cycles.toInt().coerceAtLeast(1)
        val cw = w / cycles
        for (c in 0 until n) {
            val x0 = labelW + c * cw
            val deepShare = (0.35f - c * 0.07f).coerceAtLeast(0.05f)
            val remShare = (0.12f + c * 0.06f).coerceAtMost(0.4f)
            val lightShare = 1f - deepShare - remShare
            var x = x0
            listOf(0 to lightShare * 0.6f, 1 to deepShare, 0 to lightShare * 0.4f, 2 to remShare).forEach { (row, share) ->
                val bw = cw * share - 3f
                val color = when (row) {
                    0 -> lerp(Palette.Accent, Palette.AccentDeep, 0.35f)
                    1 -> Palette.AccentDeep
                    else -> Color(0xFFE88BB4)
                }
                if (bw > 2f) drawRoundRect(color, Offset(x, row * rowH + rowH * 0.18f), Size(bw, rowH * 0.64f), CornerRadius(rowH * 0.32f))
                x += cw * share
            }
        }
    }
}

@Composable
private fun WeekView(week: List<SleepSession>, averageMinutes: Int, steadyNights: Int, target: String) {
    val measurer = rememberTextMeasurer()
    val zone = ZoneId.systemDefault()
    BentoCard(Modifier.fillMaxWidth()) {
        Text("Average", style = Type.Label, color = Palette.InkSoft)
        Text(hoursMinutes(averageMinutes), style = Type.Numeral, color = Palette.Ink)
        Text("$steadyNights of ${week.size} bedtimes within 30 min of $target", style = Type.Body, color = Palette.InkSoft)
        val grow = remember { Animatable(0f) }
        LaunchedEffect(week) { grow.animateTo(1f, spring(0.6f, 120f)) }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .padding(top = 18.dp)
                .semantics { contentDescription = week.joinToString { hoursMinutes(it.minutes) } },
        ) {
            val n = 7
            val gap = 10.dp.toPx()
            val labelH = 20.dp.toPx()
            val chartH = size.height - labelH - 18.dp.toPx()
            val barW = (size.width - gap * (n - 1)) / n
            val goalY = 18.dp.toPx() + chartH * (1f - 7f / 10f)
            drawLine(Palette.InkMuted, Offset(0f, goalY), Offset(size.width, goalY), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            week.forEachIndexed { i, s ->
                val hours = s.minutes / 60f
                val bh = chartH * (hours / 10f).coerceIn(0.06f, 1f) * grow.value
                val x = i * (barW + gap)
                val top = 18.dp.toPx() + chartH - bh
                val color = when {
                    hours >= 7f -> Palette.Sage
                    hours >= 6f -> Palette.Butter
                    else -> Palette.Peach
                }
                drawRoundRect(color, Offset(x, top), Size(barW, bh), CornerRadius(barW / 2f))
                val value = measurer.measure("%.1f".format(hours), Type.Small.copy(color = Palette.Ink))
                drawText(value, topLeft = Offset(x + (barW - value.size.width) / 2f, top - value.size.height - 2f))
                val day = s.start.atZone(zone).dayOfWeek.name.take(2).lowercase().replaceFirstChar { it.uppercaseChar() }
                val label = measurer.measure(day, Type.Small.copy(color = Palette.InkMuted))
                drawText(label, topLeft = Offset(x + (barW - label.size.width) / 2f, size.height - label.size.height))
            }
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Legend(Palette.Sage, "7 h or more")
            Legend(Palette.Butter, "6 to 7 h")
            Legend(Palette.Peach, "Under 6 h")
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(label, style = Type.Small, color = Palette.InkSoft, modifier = Modifier.padding(start = 6.dp))
    }
}
