package com.sleepbetter.app.ui.tonight

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.audio.TimerChoice
import com.sleepbetter.app.ui.components.ExpressiveButtonGroup
import com.sleepbetter.app.ui.components.KineticHeadline
import com.sleepbetter.app.ui.components.MorphPlayButton
import com.sleepbetter.app.ui.components.SoundChip
import com.sleepbetter.app.ui.components.glass
import com.sleepbetter.app.ui.components.rememberClock
import com.sleepbetter.app.ui.components.rememberEngineFrame
import com.sleepbetter.app.ui.components.rememberReduceMotion
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.app.ui.theme.uiColor
import com.sleepbetter.core.audio.Preset
import com.sleepbetter.core.audio.SoundId
import kotlinx.coroutines.delay
import java.time.LocalTime

private enum class View(val label: String) { ISLAND("Island"), STAGE("Stage") }

/**
 * Tonight: the same mix seen two ways. Island shows what is playing as a
 * living scene; Stage lets you place each sound closer or farther, left or right.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TonightScreen(vm: AppViewModel, onOpenVisitors: () -> Unit, modifier: Modifier = Modifier) {
    val mix by vm.mix.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val frame by rememberEngineFrame(vm.engine)
    val time by rememberClock()
    val still = rememberReduceMotion()
    var view by rememberSaveable { mutableStateOf(View.ISLAND) }
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val steady = remember(sessions, settings) { vm.repository.totalSteadyNights() }
    val visitors = remember(steady) { vm.repository.visitorProgress().unlocked }

    // Minutes until bedtime, refreshed every half minute.
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
    ) {
        Spacer(Modifier.padding(top = 12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting(now), style = Type.Body, color = Palette.InkSoft)
                Text(
                    "Bedtime ${settings.bedtimeLabel}, ${formatIn(untilBed)}",
                    style = Type.Small,
                    color = Palette.InkMuted,
                )
            }
            Text(
                "$steady good nights",
                style = Type.Label,
                color = Palette.Ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .glass(RoundedCornerShape(22.dp))
                    .clickable(role = Role.Button, onClick = onOpenVisitors)
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
        Spacer(Modifier.padding(top = 14.dp))
        KineticHeadline(
            if (view == View.ISLAND) "Build tonight's island." else "Pull the rain a little closer.",
            Type.Hero,
        )
        Spacer(Modifier.padding(top = 14.dp))
        ExpressiveButtonGroup(View.entries.toList(), view, { it.label }, { view = it }, Modifier.fillMaxWidth())

        AnimatedContent(
            targetState = view,
            transitionSpec = {
                (fadeIn() + scaleIn(spring(0.6f, 300f), initialScale = 0.92f)) togetherWith (fadeOut() + scaleOut(targetScale = 1.05f))
            },
            label = "view",
        ) { v ->
            when (v) {
                View.ISLAND -> IslandScene(
                    active = mix.active,
                    frame = frame,
                    time = time,
                    visitors = visitors,
                    still = still,
                    modifier = Modifier.fillMaxWidth().aspectRatio(390f / 470f),
                )
                View.STAGE -> Column {
                    Spacer(Modifier.padding(top = 12.dp))
                    SoundStage(mix.active, mix.positions, frame, time, vm::moveSound)
                    Text(
                        stageCaption(mix.active, mix.positions),
                        style = Type.Body,
                        color = Palette.Ink,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }

        Text("Sounds", style = Type.Section, color = Palette.Ink, modifier = Modifier.padding(top = 18.dp))
        FlowRow(
            Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SoundId.entries.forEach { id ->
                SoundChip(id.label, id.uiColor(), id in mix.active, { vm.toggleSound(id) })
            }
        }

        Text("Mixes", style = Type.Section, color = Palette.Ink, modifier = Modifier.padding(top = 20.dp))
        LazyRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Preset.entries.toList()) { preset ->
                Text(
                    preset.label,
                    style = Type.Label,
                    color = Palette.Ink,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .glass(RoundedCornerShape(20.dp))
                        .clickable(role = Role.Button) { vm.applyPreset(preset) }
                        .heightIn(min = 44.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }

        Column(
            Modifier
                .padding(top = 22.dp, bottom = 24.dp)
                .glass(RoundedCornerShape(32.dp))
                .padding(16.dp),
        ) {
            ExpressiveButtonGroup(TimerChoice.entries.toList(), mix.timer, { it.label }, vm::setTimer, Modifier.fillMaxWidth())
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (mix.playing) "Playing your island" else "Ready to play", style = Type.Section, color = Palette.Ink)
                    Text(timerHint(mix.timer, now), style = Type.Small, color = Palette.InkSoft)
                }
                Spacer(Modifier.width(12.dp))
                MorphPlayButton(mix.playing, vm::togglePlay)
            }
        }
    }
}

private fun greeting(now: LocalTime) = when (now.hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

private fun formatIn(minutes: Int): String = when {
    minutes < 60 -> "in $minutes minutes"
    minutes % 60 == 0 -> "in ${minutes / 60} h"
    else -> "in ${minutes / 60} h ${minutes % 60} m"
}

private fun timerHint(choice: TimerChoice, now: LocalTime): String {
    val minutes = choice.minutes ?: return "Plays until you stop it"
    val end = now.plusMinutes(minutes.toLong())
    return "Fades out at %02d:%02d".format(end.hour, end.minute)
}
