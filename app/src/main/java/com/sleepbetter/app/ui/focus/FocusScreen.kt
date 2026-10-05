package com.sleepbetter.app.ui.focus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.ui.components.Critter
import com.sleepbetter.app.ui.components.CritterView
import com.sleepbetter.app.ui.components.KineticHeadline
import com.sleepbetter.app.ui.components.MorphPlayButton
import com.sleepbetter.app.ui.components.SoundChip
import com.sleepbetter.app.ui.components.glass
import com.sleepbetter.app.ui.components.rememberClock
import com.sleepbetter.app.ui.components.rememberEngineFrame
import com.sleepbetter.app.ui.components.rememberReduceMotion
import com.sleepbetter.app.ui.theme.Display
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.app.ui.theme.uiColor
import com.sleepbetter.core.audio.SoundId
import kotlin.math.pow
import kotlin.math.sin

private val Mint = Color(0xFF7BF2C9)

/** Focus: 25-minute sessions with generative music; the visuals move on the music's real beat. */
@Composable
fun FocusScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val focus by vm.focus.collectAsStateWithLifecycle()
    val mix by vm.mix.collectAsStateWithLifecycle()
    val frame by rememberEngineFrame(vm.engine)
    val time by rememberClock()
    val still = rememberReduceMotion()
    val musicOn = SoundId.FOCUS_MUSIC in mix.active && mix.playing
    // Sharp attack on each beat, easing out until the next.
    val beatPulse = if (musicOn && !still) (1f - frame.beatPhase).pow(3) else 0f
    val level = frame.levels[SoundId.FOCUS_MUSIC.ordinal]

    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.padding(top = 12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Deep work", style = Type.Body, color = Palette.InkSoft)
                Text("Session ${focus.session} of 4", style = Type.Title, color = Palette.Ink)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.clearAndSetSemantics { contentDescription = "Session ${focus.session} of 4" }) {
                (1..4).forEach { i ->
                    val c = when {
                        i < focus.session -> Mint
                        i == focus.session -> Color.White
                        else -> Color(0x33FFFFFF)
                    }
                    Box(Modifier.size(12.dp).background(c, CircleShape))
                }
            }
        }

        Box(Modifier.fillMaxWidth().aspectRatio(1.05f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                val base = size.minDimension * 0.42f
                drawCircle(Mint.copy(alpha = 0.25f + 0.5f * beatPulse), base * (1f + 0.05f * beatPulse), c, style = Stroke(1.5f))
                drawCircle(Mint.copy(alpha = 0.15f + 0.4f * beatPulse), base * 0.74f * (1f + 0.08f * beatPulse), c, style = Stroke(1.5f))
                val spin = if (still) 0f else time * 9f
                for (i in 0 until 24) {
                    val wobble = 0.5f + 0.5f * sin(time * 2.2f + i * 1.7f)
                    val h = base * (0.12f + 0.16f * wobble * (0.4f + level * 2f) + 0.1f * beatPulse)
                    rotate(spin + i * 15f, c) {
                        drawLine(Mint, Offset(c.x, c.y - base * 0.6f), Offset(c.x, c.y - base * 0.6f - h), 6f, StrokeCap.Round)
                    }
                }
            }
            Box(Modifier.size(150.dp).background(Color(0xBF081417), CircleShape), contentAlignment = Alignment.Center) {
                CritterView(Critter.DINO, Modifier.size(104.dp), headphones = true, beat = if (musicOn) frame.beatPhase else null)
            }
        }

        RollingTimer(focus.remainingSeconds, Modifier.align(Alignment.CenterHorizontally))
        Text(
            "until a 5-minute break. A gentle buzz tells you when.",
            style = Type.Body,
            color = Palette.InkSoft,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp),
        )

        Column(
            Modifier
                .padding(top = 22.dp)
                .glass(RoundedCornerShape(30.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    KineticHeadline("Low tide", Type.Section)
                    Text("Generated piano and pads at 72 BPM", style = Type.Small, color = Palette.InkSoft)
                }
                Spacer(Modifier.width(12.dp))
                MorphPlayButton(focus.running, vm::toggleFocus, color = Mint)
            }
            Text("Add underneath", style = Type.Label, color = Palette.Ink, modifier = Modifier.padding(top = 14.dp))
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(SoundId.RAIN, SoundId.BROWN_NOISE, SoundId.STREAM).forEach { id ->
                    SoundChip(id.label, id.uiColor(), id in mix.active, { vm.toggleSound(id) })
                }
            }
        }
        Spacer(Modifier.padding(bottom = 24.dp))
    }
}

/** MM:SS where each digit rolls into place with a spring when it changes. */
@Composable
private fun RollingTimer(seconds: Int, modifier: Modifier = Modifier) {
    val text = "%02d:%02d".format(seconds / 60, seconds % 60)
    val style = Type.Hero.copy(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 92.sp, lineHeight = 96.sp)
    Row(modifier.clearAndSetSemantics { contentDescription = "${seconds / 60} minutes ${seconds % 60} seconds left" }) {
        text.forEachIndexed { index, ch ->
            AnimatedContent(
                targetState = ch,
                transitionSpec = {
                    (slideInVertically(spring(0.6f, 400f)) { -it } + fadeIn()) togetherWith
                        (slideOutVertically(spring(0.6f, 400f)) { it } + fadeOut()) using SizeTransform(clip = true)
                },
                label = "digit$index",
            ) { c ->
                Text(c.toString(), style = style, color = if (c == ':') Palette.InkMuted else Palette.Ink)
            }
        }
    }
}
