package com.sleepbetter.app.ui.recap

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.ui.components.AuroraBackground
import com.sleepbetter.app.ui.components.Critter
import com.sleepbetter.app.ui.components.CritterView
import com.sleepbetter.app.ui.components.DroppingNumerals
import com.sleepbetter.app.ui.components.KineticHeadline
import com.sleepbetter.app.ui.components.PillButton
import com.sleepbetter.app.ui.components.SoundChip
import com.sleepbetter.app.ui.components.rememberClock
import com.sleepbetter.app.ui.components.rememberReduceMotion
import com.sleepbetter.app.ui.theme.Display
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.app.ui.theme.color
import com.sleepbetter.core.sleep.NightTag
import com.sleepbetter.core.sleep.RiskLevel
import com.sleepbetter.core.sleep.SleepSession
import com.sleepbetter.core.sleep.Suggestions
import com.sleepbetter.core.sleep.Visitor
import com.sleepbetter.core.sleep.VisitorRules
import com.sleepbetter.core.sleep.clockDistance
import com.sleepbetter.core.sleep.minuteOfDay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.min

private val pageColors = listOf(
    listOf(Color(0xFF8E5CFF), Color(0xFFFF5CA8)),
    listOf(Color(0xFF3FA2FF), Color(0xFF6B5CFF)),
    listOf(Color(0xFF38D39F), Color(0xFFB4F06B)),
    listOf(Color(0xFF6B5CFF), Color(0xFF4FA7E8)),
    listOf(Color(0xFFFFB547), Color(0xFFFF6B6B)),
)
private val pageBases = listOf(Color(0xFF2A1670), Color(0xFF0E2E70), Color(0xFF0B4A39), Color(0xFF1B1A55), Color(0xFF7A3A16))
private const val PAGES = 5
private const val CHECK_IN_PAGE = 3

/** "Last night" as tap-through story cards, in the spirit of Spotify Wrapped. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecapScreen(vm: AppViewModel, onOpenVisitors: () -> Unit, onOpenWindDown: () -> Unit, modifier: Modifier = Modifier) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val night = sessions.lastOrNull()
    if (night == null) {
        EmptyRecap(onOpenWindDown, modifier)
        return
    }
    val report = remember(sessions, settings) { vm.repository.report() }
    val steady = remember(sessions, settings) { vm.repository.totalSteadyNights() }
    val zone = ZoneId.systemDefault()
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
    val still = rememberReduceMotion()

    // Auto-advance every 5 s, except on the check-in card where the user answers.
    LaunchedEffect(pager.currentPage) {
        if (pager.currentPage == CHECK_IN_PAGE || pager.currentPage == PAGES - 1) return@LaunchedEffect
        delay(5000)
        pager.animateScrollToPage(pager.currentPage + 1)
    }

    Box(modifier.fillMaxSize()) {
        AuroraBackground(pageColors[pager.currentPage], base = pageBases[pager.currentPage], still = still)
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 64.dp, bottom = 24.dp),
            ) {
                when (page) {
                    0 -> HoursCard(night, report.averageMinutes)
                    1 -> BedtimeCard(night, settings.bedtimeMinute, settings.bedtimeLabel, zone)
                    2 -> WeekCard(sessions.takeLast(7), report.steadyNights, settings.bedtimeLabel)
                    3 -> CheckInCard(night) { rating, tags ->
                        vm.rateLastNight(rating, tags)
                        scope.launch { pager.animateScrollToPage(PAGES - 1) }
                    }
                    else -> LevelCard(vm, report.level, report.score, VisitorRules.arrivedAt(steady), settings.bedtimeLabel, onOpenVisitors)
                }
            }
        }
        StoryBars(pager.currentPage, Modifier.padding(horizontal = 16.dp, vertical = 18.dp))
    }
}

@Composable
private fun StoryBars(current: Int, modifier: Modifier) {
    val progress = remember(current) { Animatable(0f) }
    LaunchedEffect(current) { progress.animateTo(1f, tween(5000, easing = LinearEasing)) }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(PAGES) { i ->
            val fill = when {
                i < current -> 1f
                i == current -> progress.value
                else -> 0f
            }
            Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x4DFFFFFF))) {
                Box(Modifier.fillMaxWidth(fill).height(4.dp).background(Color.White))
            }
        }
    }
}

private val bigNumber = Type.Hero.copy(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 120.sp, lineHeight = 120.sp)
private val bigUnit = bigNumber.copy(fontSize = 54.sp)

@Composable
private fun ColumnScope.HoursCard(night: SleepSession, averageMinutes: Int) {
    val h = night.minutes / 60
    val m = night.minutes % 60
    Text("Last night you slept", style = Type.Body.copy(fontSize = 18.sp), color = Color.White)
    DroppingNumerals("${h}h ${m}m", bigNumber, smallStyle = bigUnit, modifier = Modifier.padding(top = 12.dp))
    val diff = night.minutes - averageMinutes
    val line = when {
        abs(diff) < 10 -> "Right on your weekly average."
        diff > 0 -> "That is $diff minutes more than your weekly average."
        else -> "That is ${-diff} minutes less than your weekly average."
    }
    Text(line, style = Type.Body.copy(fontSize = 20.sp, lineHeight = 28.sp), color = Color.White, modifier = Modifier.padding(top = 24.dp))
}

@Composable
private fun ColumnScope.BedtimeCard(night: SleepSession, targetMinute: Int, targetLabel: String, zone: ZoneId) {
    val bed = night.start.atZone(zone).toLocalTime()
    val off = clockDistance(bed.minuteOfDay(), targetMinute)
    val time by rememberClock()
    Box(Modifier.fillMaxWidth().height(80.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0 until 9) {
                val x = size.width * (i + 0.5f) / 9f
                val y = (time * 260f + i * 53f) % size.height
                drawLine(Color.White.copy(alpha = 0.5f), Offset(x, y), Offset(x - 2f, y + 22f), 2f, StrokeCap.Round)
            }
        }
    }
    Text("You went to bed at", style = Type.Body.copy(fontSize = 18.sp), color = Color.White)
    DroppingNumerals("%02d:%02d".format(bed.hour, bed.minute), bigNumber, modifier = Modifier.padding(top = 12.dp))
    val line = if (off <= 30) "$off minutes from your $targetLabel target. That counts as a steady night." else "$off minutes from your $targetLabel target."
    Text(line, style = Type.Body.copy(fontSize = 20.sp, lineHeight = 28.sp), color = Color.White, modifier = Modifier.padding(top = 24.dp))
}

@Composable
private fun ColumnScope.WeekCard(week: List<SleepSession>, steadyNights: Int, targetLabel: String) {
    KineticHeadline(if (steadyNights >= 5) "Your steadiest week yet." else "Your week, night by night.", Type.Hero)
    Row(Modifier.fillMaxWidth().padding(top = 36.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEachIndexed { i, s ->
            val hours = s.minutes / 60f
            val rise = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(150L * i)
                rise.animateTo(1f, spring(0.45f, 220f))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = rise.value.coerceIn(0f, 1f)
                        translationY = (1f - rise.value) * 60f
                    }
                    .semantics { contentDescription = "%.1f hours".format(hours) },
            ) {
                Moon(min(hours / 8f, 1f), Modifier.size(38.dp))
                Text("%.1f".format(hours), style = Type.Label, color = Color.White, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
    Text(
        "$steadyNights of ${week.size} bedtimes landed within 30 minutes of $targetLabel.",
        style = Type.Body.copy(fontSize = 20.sp, lineHeight = 28.sp),
        color = Color.White,
        modifier = Modifier.padding(top = 32.dp),
    )
}

/** A moon that is [fullness] lit: the score and each night shown as moon phases. */
@Composable
fun Moon(fullness: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        drawCircle(Color.White.copy(alpha = 0.16f), r)
        val lit = Path().apply { addOval(Rect(center, r)) }
        val shadow = Path().apply { addOval(Rect(Offset(center.x + 2f * r * fullness.coerceIn(0f, 1f) + 1f, center.y), r)) }
        drawPath(Path.combine(PathOperation.Difference, lit, shadow), Color(0xFFFFF3C4))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.CheckInCard(night: SleepSession, onSave: (Int?, Set<NightTag>) -> Unit) {
    var rating by remember(night) { mutableStateOf(night.rating) }
    var tags by remember(night) { mutableStateOf(night.tags) }
    KineticHeadline("How did you sleep?", Type.Hero)
    Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("Awful", "Poor", "Okay", "Good", "Great").forEachIndexed { i, label ->
            val value = i + 1
            val on = rating == value
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.RadioButton) { rating = value }
                    .semantics { selected = on }
                    .padding(6.dp),
            ) {
                Box(
                    Modifier.size(48.dp).background(if (on) Color.White else Color(0x33FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$value", style = Type.Section, color = if (on) Color(0xFF1B1A55) else Color.White)
                }
                Text(label, style = Type.Small, color = Color.White, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    Text("Anything different last night?", style = Type.Section, color = Color.White, modifier = Modifier.padding(top = 24.dp))
    FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NightTag.entries.forEach { tag ->
            SoundChip(tag.label, Color(0xFFB9A8FF), tag in tags, { tags = if (tag in tags) tags - tag else tags + tag })
        }
    }
    Text("After two weeks we show which of these change your sleep.", style = Type.Small, color = Palette.InkSoft, modifier = Modifier.padding(top = 10.dp))
    Spacer(Modifier.weight(1f))
    PillButton("Save", onClick = { onSave(rating, tags) }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ColumnScope.LevelCard(
    vm: AppViewModel,
    level: RiskLevel,
    score: Int,
    arrived: Visitor?,
    targetLabel: String,
    onOpenVisitors: () -> Unit,
) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val tip = remember(sessions) { Suggestions.forReport(vm.repository.report(), sessions.takeLast(7), targetLabel).first() }
    if (arrived != null) {
        KineticHeadline("Someone new moved in.", Type.Hero)
        val pop = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            delay(300)
            pop.animateTo(1f, spring(0.4f, 200f))
        }
        Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
            CritterView(
                critterFor(arrived),
                Modifier.size(180.dp).graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                    rotationZ = (1f - pop.value) * -20f
                },
            )
        }
        Text("Steady bedtimes brought ${arrived.displayName} to your island.", style = Type.Body.copy(fontSize = 18.sp), color = Color.White)
    } else {
        KineticHeadline("Your sleep this week", Type.Hero)
        Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Moon(score / 100f, Modifier.size(84.dp))
            Column(Modifier.padding(start = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(22.dp).background(level.color(), CircleShape), contentAlignment = Alignment.Center) {
                        Text(levelSymbol(level), style = Type.Label, color = Color(0xFF0F1133))
                    }
                    Text(level.label, style = Type.Section, color = Color.White, modifier = Modifier.padding(start = 8.dp))
                }
                Text("Sleep score $score of 100", style = Type.Body, color = Color.White)
            }
        }
    }
    Text(tip.title, style = Type.Section, color = Color.White, modifier = Modifier.padding(top = 20.dp))
    Text(tip.body, style = Type.Body, color = Palette.InkSoft)
    Spacer(Modifier.weight(1f))
    Text(Suggestions.DISCLAIMER, style = Type.Small, color = Palette.InkSoft)
    PillButton(if (arrived != null) "Say hello" else "See your visitors", onClick = onOpenVisitors, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
}

private fun levelSymbol(level: RiskLevel) = when (level) {
    RiskLevel.GOOD -> "✓"
    RiskLevel.MEDIUM -> "–"
    RiskLevel.AT_RISK -> "!"
}

fun critterFor(visitor: Visitor): Critter = when (visitor) {
    Visitor.PIP -> Critter.PANDA
    Visitor.EMBER -> Critter.FOX
    Visitor.HOOT -> Critter.OWL
    Visitor.DOZY -> Critter.DINO
    Visitor.KOALA -> Critter.KOALA
    Visitor.CAT -> Critter.CAT
}

@Composable
private fun EmptyRecap(onOpenWindDown: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        CritterView(Critter.PANDA, Modifier.size(140.dp).align(Alignment.CenterHorizontally), sleeping = true)
        KineticHeadline("Your first story appears after your first night.", Type.Title, modifier = Modifier.padding(top = 20.dp))
        Text(
            "Start sleep mode at bedtime and tap \"I'm awake\" in the morning. We'll turn the night into a short story.",
            style = Type.Body,
            color = Palette.InkSoft,
            modifier = Modifier.padding(top = 8.dp),
        )
        PillButton("Go to wind down", onClick = onOpenWindDown, modifier = Modifier.fillMaxWidth().padding(top = 20.dp))
    }
}
