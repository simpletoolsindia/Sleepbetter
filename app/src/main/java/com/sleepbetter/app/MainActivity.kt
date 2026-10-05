package com.sleepbetter.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.ui.components.AuroraBackground
import com.sleepbetter.app.ui.components.glass
import com.sleepbetter.app.ui.components.rememberReduceMotion
import com.sleepbetter.app.ui.focus.FocusScreen
import com.sleepbetter.app.ui.recap.RecapScreen
import com.sleepbetter.app.ui.sleep.SleepModeScreen
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.SleepBetterTheme
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.app.ui.theme.uiColor
import com.sleepbetter.app.ui.tonight.TonightScreen
import com.sleepbetter.app.ui.visitors.VisitorsScreen
import com.sleepbetter.app.ui.winddown.WindDownScreen

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_DESTINATION = "destination"
    }

    private val vm: AppViewModel by viewModels()
    private val startDestination = mutableStateOf<Destination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        readDestination(intent)
        setContent {
            SleepBetterTheme {
                SleepBetterUi(vm, startDestination.value) { startDestination.value = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readDestination(intent)
    }

    private fun readDestination(intent: Intent?) {
        val name = intent?.getStringExtra(EXTRA_DESTINATION) ?: return
        startDestination.value = runCatching { Destination.valueOf(name) }.getOrNull()
    }
}

private val tabs = listOf(Destination.TONIGHT, Destination.FOCUS, Destination.WIND_DOWN, Destination.LAST_NIGHT, Destination.VISITORS)

@Composable
fun SleepBetterUi(vm: AppViewModel, requested: Destination?, onRequestHandled: () -> Unit) {
    var dest by rememberSaveable { mutableStateOf(if (vm.repository.sleepStartedAt != null) Destination.SLEEP else Destination.TONIGHT) }
    LaunchedEffect(requested) {
        if (requested != null) {
            dest = requested
            onRequestHandled()
        }
    }
    val mix by vm.mix.collectAsStateWithLifecycle()
    val still = rememberReduceMotion()

    BackHandler(enabled = dest != Destination.TONIGHT && dest != Destination.SLEEP) { dest = Destination.TONIGHT }

    Box(Modifier.fillMaxSize().background(Palette.Night)) {
        if (dest != Destination.SLEEP && dest != Destination.LAST_NIGHT) {
            AuroraBackground(mix.active.map { it.uiColor() }.take(3), still = still)
        }
        AnimatedContent(
            targetState = dest,
            transitionSpec = {
                (fadeIn() + scaleIn(spring(0.7f, 400f), initialScale = 0.96f)) togetherWith fadeOut()
            },
            label = "destination",
        ) { d ->
            when (d) {
                Destination.SLEEP -> Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                    SleepModeScreen(
                        vm,
                        onWake = { logged -> dest = if (logged) Destination.LAST_NIGHT else Destination.TONIGHT },
                        onBack = { dest = Destination.WIND_DOWN },
                    )
                }
                else -> Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    Box(Modifier.weight(1f)) {
                        when (d) {
                            Destination.TONIGHT -> TonightScreen(vm, onOpenVisitors = { dest = Destination.VISITORS })
                            Destination.FOCUS -> FocusScreen(vm)
                            Destination.WIND_DOWN -> WindDownScreen(vm, onStartSleep = {
                                vm.startSleep()
                                dest = Destination.SLEEP
                            })
                            Destination.LAST_NIGHT -> RecapScreen(
                                vm,
                                onOpenVisitors = { dest = Destination.VISITORS },
                                onOpenWindDown = { dest = Destination.WIND_DOWN },
                            )
                            Destination.VISITORS -> VisitorsScreen(vm)
                            Destination.SLEEP -> Unit
                        }
                    }
                    GlassNavBar(dest, onSelect = { dest = it })
                }
            }
        }
    }
}

@Composable
private fun GlassNavBar(current: Destination, onSelect: (Destination) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .glass(RoundedCornerShape(32.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEach { tab ->
            val on = tab == current
            Box(
                Modifier
                    .weight(if (on) 1.6f else 1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (on) Color.White else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .semantics { selected = on },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label,
                    style = Type.Small.copy(fontWeight = if (on) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal),
                    color = if (on) Color(0xFF0A0B1C) else Palette.InkSoft,
                    maxLines = 1,
                )
            }
        }
    }
}
