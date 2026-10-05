package com.sleepbetter.app.ui.friends

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.ui.components.BentoCard
import com.sleepbetter.app.ui.components.Species
import com.sleepbetter.app.ui.components.MochiView
import com.sleepbetter.app.ui.components.enter
import com.sleepbetter.app.ui.components.pressable
import com.sleepbetter.app.ui.components.species
import com.sleepbetter.app.ui.components.LocalBurst
import com.sleepbetter.app.ui.components.SpeechBubble
import com.sleepbetter.app.ui.components.emoji
import com.sleepbetter.app.ui.components.floaty
import androidx.compose.ui.geometry.Offset
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.core.sleep.Visitor
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** Friends: the Mochi family that moves in as your bedtimes get steady. */
@Composable
fun FriendsScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val focusCount by vm.focusSessions.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val steady = remember(sessions, settings) { vm.repository.totalSteadyNights() }
    val progress = remember(steady, focusCount) { vm.repository.visitorProgress() }
    var selected by remember { mutableStateOf<Visitor?>(null) }
    val burst = LocalBurst.current

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Friends 🐾", style = Type.Display, color = Palette.Ink, modifier = Modifier.padding(top = 12.dp))
        Text("Keep a steady bedtime and new friends move in. They sleep when you sleep.", style = Type.Body, color = Palette.InkSoft)

        progress.next?.let { next ->
            BentoCard(Modifier.fillMaxWidth().enter(0), color = Palette.Butter) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MochiView(Modifier.size(64.dp), species = next.species(), silhouette = true)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Someone is on the way 🎁", style = Type.Heading, color = Palette.Ink)
                        Text(
                            if (progress.nightsToNext == 1) "1 more steady night" else "${progress.nightsToNext} more steady nights",
                            style = Type.Small,
                            color = Palette.InkSoft,
                        )
                    }
                }
                val needed = next.steadyNightsNeeded
                Row(
                    Modifier.padding(top = 14.dp).semantics { contentDescription = "$steady of $needed steady nights" },
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    val segments = needed.coerceAtMost(14)
                    repeat(segments) { i ->
                        val filled = i < steady * segments / needed
                        Box(Modifier.weight(1f).height(10.dp).background(if (filled) Palette.Ink else Color.White.copy(alpha = 0.7f), RoundedCornerShape(5.dp)))
                    }
                }
                Text("Steady means in bed within 30 min of ${settings.bedtimeLabel}.", style = Type.Small, color = Palette.InkSoft, modifier = Modifier.padding(top = 8.dp))
            }
        }

        // The friend you tapped says hello in a speech bubble.
        val chosen = selected
        if (chosen == null) {
            Text("Tap a friend to say hello 👋", style = Type.Body, color = Palette.Ink, modifier = Modifier.heightIn(min = 44.dp))
        } else {
            Row(Modifier.heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                MochiView(Modifier.size(56.dp), species = chosen.species(), mood = 4f)
                SpeechBubble("${chosen.species().emoji()} ${chosen.blurb}", Modifier.weight(1f))
            }
        }

        Visitor.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { v ->
                    FriendCard(v, v in progress.unlocked, selected == v, Modifier.weight(1f)) {
                        selected = v
                        burst.fire(listOf("💖", "💕", v.species().emoji(), "✨"), Offset(0.5f, 0.55f), count = 14)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun FriendCard(visitor: Visitor, unlocked: Boolean, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val hop = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val species = visitor.species()
    val bg = when {
        !unlocked -> Palette.Card
        species == Species.MOCHI -> Palette.Accent // Mochi is white; a white card would hide it
        else -> lerp(Color.White, species.body, 0.35f)
    }
    Column(
        modifier
            .clip(RoundedCornerShape(if (selected) 22.dp else 28.dp))
            .background(bg)
            .pressable(enabled = unlocked) {
                onClick()
                scope.launch {
                    hop.snapTo(0f)
                    hop.animateTo(1f, spring(0.35f, 300f))
                }
            }
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MochiView(
            Modifier.size(104.dp).then(if (unlocked) Modifier.floaty(amplitude = 3f, phase = visitor.ordinal * 0.37f) else Modifier).graphicsLayer { translationY = -50f * sin(hop.value * PI.toFloat()).coerceAtLeast(0f) },
            species = species,
            silhouette = !unlocked,
            mood = 3.5f,
        )
        Text(if (unlocked) "${species.emoji()} ${visitor.displayName}" else "🔒 Someone new", style = Type.Heading, color = Palette.Ink, modifier = Modifier.padding(top = 6.dp))
        Text(
            when {
                unlocked -> if (visitor.steadyNightsNeeded == 0 && visitor.focusSessionsNeeded == 0) "Here from day one" else "Lives with you"
                visitor.focusSessionsNeeded > 0 -> "${visitor.focusSessionsNeeded} focus sessions"
                else -> "${visitor.steadyNightsNeeded} steady nights"
            },
            style = Type.Small,
            color = Palette.InkSoft,
        )
    }
}
