package com.sleepbetter.app.ui.visitors

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.ui.components.CritterView
import com.sleepbetter.app.ui.components.KineticHeadline
import com.sleepbetter.app.ui.recap.critterFor
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.core.sleep.Visitor
import kotlinx.coroutines.launch

/** The collection: friends who moved onto your island, and who is coming next. */
@Composable
fun VisitorsScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val focusCount by vm.focusSessions.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val steady = remember(sessions, settings) { vm.repository.totalSteadyNights() }
    val progress = remember(steady, focusCount) { vm.repository.visitorProgress() }
    var selected by remember { mutableStateOf<Visitor?>(null) }

    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.padding(top = 12.dp))
        KineticHeadline("Island visitors", Type.Hero)
        Text(
            "Friends move onto your island when you keep a steady bedtime. They sleep when you sleep.",
            style = Type.Body,
            color = Palette.InkSoft,
            modifier = Modifier.padding(top = 8.dp),
        )

        progress.next?.let { next ->
            val needed = next.steadyNightsNeeded
            Column(
                Modifier
                    .padding(top = 18.dp)
                    .background(Palette.Sheet, RoundedCornerShape(26.dp))
                    .padding(18.dp),
            ) {
                Row {
                    Text("Next visitor", style = Type.Section, color = Palette.Ink, modifier = Modifier.weight(1f))
                    Text("$steady of $needed steady nights", style = Type.Small, color = Palette.InkSoft)
                }
                Row(
                    Modifier.padding(top = 12.dp).semantics { contentDescription = "$steady of $needed steady nights" },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val segments = needed.coerceAtMost(14)
                    repeat(segments) { i ->
                        val filled = i < steady * segments / needed
                        Box(Modifier.weight(1f).height(10.dp).background(if (filled) Palette.Lantern else Color(0xFF2E3270), RoundedCornerShape(5.dp)))
                    }
                }
                Text("A steady night means bed within 30 minutes of ${settings.bedtimeLabel}.", style = Type.Small, color = Palette.InkMuted, modifier = Modifier.padding(top = 10.dp))
            }
        }

        Text(
            selected?.blurb ?: "Tap a visitor to say hello.",
            style = Type.Body,
            color = Palette.Ink,
            modifier = Modifier.padding(top = 18.dp).heightIn(min = 48.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )

        Visitor.entries.chunked(2).forEach { row ->
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { v ->
                    VisitorCard(
                        visitor = v,
                        unlocked = v in progress.unlocked,
                        selected = selected == v,
                        modifier = Modifier.weight(1f),
                        onClick = { selected = v },
                    )
                }
            }
        }
        Spacer(Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun VisitorCard(visitor: Visitor, unlocked: Boolean, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val hop = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(if (selected) 20.dp else 28.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (unlocked) Palette.Sheet else Color.Transparent, shape)
            .border(1.5.dp, if (selected) Palette.Lantern else if (unlocked) Color.Transparent else Color(0xFF353A7C), shape)
            .clickable(enabled = unlocked, role = Role.Button) {
                onClick()
                scope.launch {
                    hop.snapTo(0f)
                    hop.animateTo(1f, spring(0.35f, 300f))
                }
            }
            .padding(vertical = 16.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CritterView(
            critterFor(visitor),
            Modifier.size(96.dp).graphicsLayer {
                // A quick hop: up and back down, squashing a little on landing.
                val p = hop.value
                translationY = -60f * kotlin.math.sin(p * Math.PI.toFloat()).coerceAtLeast(0f)
            },
            silhouette = !unlocked,
        )
        Text(if (unlocked) visitor.displayName else lockedName(visitor), style = Type.Section, color = Palette.Ink)
        Text(
            when {
                unlocked -> if (visitor == Visitor.PIP) "Here from day one" else "Lives on your island"
                visitor.focusSessionsNeeded > 0 -> "${visitor.focusSessionsNeeded} focus sessions"
                else -> "${visitor.steadyNightsNeeded} steady nights"
            },
            style = Type.Small,
            color = Palette.InkSoft,
        )
    }
}

private fun lockedName(visitor: Visitor) = when (visitor) {
    Visitor.KOALA, Visitor.CAT -> visitor.displayName
    else -> "Someone new"
}
