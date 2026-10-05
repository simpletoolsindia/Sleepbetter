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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.FOCUS_SECONDS
import com.sleepbetter.app.ui.components.BentoCard
import com.sleepbetter.app.ui.components.CircleButton
import com.sleepbetter.app.ui.components.Dots
import com.sleepbetter.app.ui.components.Glyph
import com.sleepbetter.app.ui.components.DriftingEmoji
import com.sleepbetter.app.ui.components.MochiView
import com.sleepbetter.app.ui.components.MorphPlayButton
import com.sleepbetter.app.ui.components.SoundTile
import com.sleepbetter.app.ui.components.rememberClock
import com.sleepbetter.app.ui.components.rememberEngineFrame
import com.sleepbetter.app.ui.components.rememberReduceMotion
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.core.audio.SoundId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** Focus: a 25-minute session. The ring of bars and Mochi move on the music's real beat. */
@Composable
fun FocusScreen(vm: AppViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val focus by vm.focus.collectAsStateWithLifecycle()
    val mix by vm.mix.collectAsStateWithLifecycle()
    val frame by rememberEngineFrame(vm.engine)
    val time by rememberClock()
    val still = rememberReduceMotion()
    val musicOn = SoundId.FOCUS_MUSIC in mix.active && mix.playing
    val beat = if (musicOn && !still) (1f - frame.beatPhase).pow(3) else 0f
    val level = frame.levels[SoundId.FOCUS_MUSIC.ordinal]
    val progress = 1f - focus.remainingSeconds / FOCUS_SECONDS.toFloat()

    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleButton(Glyph.BACK, "Back", onBack)
            Spacer(Modifier.weight(1f))
            Dots(4, focus.session, Palette.AccentDeep, Modifier.clearAndSetSemantics { contentDescription = "Session ${focus.session} of 4" })
        }
        Text("Deep work 🧠", style = Type.Display, color = Palette.Ink, modifier = Modifier.padding(top = 14.dp))
        Text("Session ${focus.session} of 4, then a 5-minute break", style = Type.Body, color = Palette.InkSoft)

        Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 8.dp), contentAlignment = Alignment.Center) {
            // Music notes float up while the focus music plays.
            if (musicOn) DriftingEmoji(listOf("🎵", "🎶", "✨"), count = 7)
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                val r = size.minDimension * 0.4f
                // Progress ring.
                drawArc(Palette.Line, 0f, 360f, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(14f))
                drawArc(Palette.AccentDeep, -90f, 360f * progress, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(14f, cap = StrokeCap.Round))
                // Bars outside the ring that bloom on every beat.
                val spin = if (still) 0f else time * 6f
                for (i in 0 until 36) {
                    val a = (i * 10f + spin) * PI.toFloat() / 180f
                    val wob = 0.5f + 0.5f * sin(time * 2f + i * 1.3f)
                    val len = 6f + 22f * (0.3f * wob + beat * 0.5f + level * 0.8f)
                    val r0 = r + 18f
                    drawLine(
                        Palette.Sky,
                        Offset(c.x + cos(a) * r0, c.y + sin(a) * r0),
                        Offset(c.x + cos(a) * (r0 + len), c.y + sin(a) * (r0 + len)),
                        6f, StrokeCap.Round,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MochiView(Modifier.size(96.dp), headphones = true, mood = 3.5f, beat = if (musicOn) frame.beatPhase else null)
                RollingTimer(focus.remainingSeconds)
            }
        }

        BentoCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Low tide", style = Type.Title, color = Palette.Ink)
                    Text("Soft piano and pads at 72 BPM, made live", style = Type.Small, color = Palette.InkMuted)
                }
                Spacer(Modifier.width(12.dp))
                MorphPlayButton(focus.running, vm::toggleFocus, color = Palette.AccentDeep)
            }
            Text("Add underneath", style = Type.Label, color = Palette.InkSoft, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(SoundId.RAIN, SoundId.STREAM, SoundId.BROWN_NOISE).forEach { id ->
                    SoundTile(id, id in mix.active, frame.levels[id.ordinal], time, { vm.toggleSound(id) }, Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** MM:SS where each digit rolls into place with a spring. */
@Composable
private fun RollingTimer(seconds: Int) {
    val text = "%02d:%02d".format(seconds / 60, seconds % 60)
    Row(Modifier.clearAndSetSemantics { contentDescription = "${seconds / 60} minutes ${seconds % 60} seconds left" }) {
        text.forEachIndexed { index, ch ->
            AnimatedContent(
                targetState = ch,
                transitionSpec = {
                    (slideInVertically(spring(0.6f, 400f)) { -it } + fadeIn()) togetherWith
                        (slideOutVertically(spring(0.6f, 400f)) { it } + fadeOut()) using SizeTransform(clip = true)
                },
                label = "digit$index",
            ) { c -> Text(c.toString(), style = Type.Numeral.copy(fontSize = 56.sp, lineHeight = 60.sp), color = Palette.Ink) }
        }
    }
}
