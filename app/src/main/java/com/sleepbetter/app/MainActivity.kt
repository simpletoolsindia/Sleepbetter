package com.sleepbetter.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.graphicsLayer
import com.sleepbetter.app.ui.components.BurstState
import com.sleepbetter.app.ui.components.EmojiBurstHost
import com.sleepbetter.app.ui.components.LocalBurst
import com.sleepbetter.app.ui.components.floaty
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.ui.checkin.CheckInScreen
import com.sleepbetter.app.ui.components.Glyph
import com.sleepbetter.app.ui.components.GlyphIcon
import com.sleepbetter.app.ui.components.pressable
import com.sleepbetter.app.ui.focus.FocusScreen
import com.sleepbetter.app.ui.friends.FriendsScreen
import com.sleepbetter.app.ui.home.HomeScreen
import com.sleepbetter.app.ui.insights.InsightsScreen
import com.sleepbetter.app.ui.sleep.SleepModeScreen
import com.sleepbetter.app.share.MixSharing
import com.sleepbetter.app.ui.sounds.IncomingMixDialog
import com.sleepbetter.app.ui.sounds.SoundsScreen
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.SleepBetterTheme
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.app.ui.winddown.WindDownScreen

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_DESTINATION = "destination"
    }

    private val vm: AppViewModel by viewModels()
    private val requested = mutableStateOf<Destination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        readDestination(intent)
        if (savedInstanceState == null) readSharedMix(intent)
        setContent {
            SleepBetterTheme {
                // Status and navigation bar icons follow the app's mode, not just the phone's.
                val dark = Palette.darkness > 0.5f
                LaunchedEffect(dark) {
                    val transparent = android.graphics.Color.TRANSPARENT
                    val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                SleepBetterUi(vm, requested.value) { requested.value = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readDestination(intent)
        readSharedMix(intent)
    }

    /** A mix shared by a friend (Quick Share, Bluetooth, a link or shared text). */
    private fun readSharedMix(intent: Intent?) {
        val text = MixSharing.textFrom(this, intent ?: return) ?: return
        if (vm.offerImport(text)) requested.value = Destination.SOUNDS
    }

    private fun readDestination(intent: Intent?) {
        val name = intent?.getStringExtra(EXTRA_DESTINATION) ?: return
        requested.value = runCatching { Destination.valueOf(name) }.getOrNull()
    }
}

private val tabs = listOf(Destination.HOME, Destination.SOUNDS, Destination.INSIGHTS, Destination.FRIENDS)
private val fullScreen = setOf(Destination.FOCUS, Destination.WIND_DOWN, Destination.SLEEP, Destination.CHECK_IN)

@Composable
fun SleepBetterUi(vm: AppViewModel, requested: Destination?, onRequestHandled: () -> Unit) {
    var dest by rememberSaveable { mutableStateOf(if (vm.repository.sleepStartedAt != null) Destination.SLEEP else Destination.HOME) }
    LaunchedEffect(requested) {
        if (requested != null) {
            dest = requested
            onRequestHandled()
        }
    }
    BackHandler(enabled = dest != Destination.HOME && dest != Destination.SLEEP) {
        dest = if (dest == Destination.CHECK_IN) Destination.INSIGHTS else Destination.HOME
    }

    val incoming by vm.incoming.collectAsStateWithLifecycle()
    incoming?.let { IncomingMixDialog(it, onAdd = vm::acceptIncoming, onDismiss = vm::dismissIncoming) }

    val burst = remember { BurstState() }
    CompositionLocalProvider(LocalBurst provides burst) {
    EmojiBurstHost(burst, Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize().background(Palette.Paper)) {
        AnimatedContent(
            targetState = dest,
            transitionSpec = {
                val from = tabs.indexOf(initialState)
                val to = tabs.indexOf(targetState)
                when {
                    // Between tabs: a shared-axis slide in the direction of travel.
                    from >= 0 && to >= 0 -> {
                        val dir = if (to > from) 1 else -1
                        (slideInHorizontally(spring(0.85f, 380f)) { it / 5 * dir } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(200)) { -it / 6 * dir } + fadeOut(tween(160)))
                    }
                    // Into a full-screen flow: rise up like a sheet.
                    targetState in fullScreen -> (slideInVertically(spring(0.8f, 300f)) { it / 3 } + fadeIn(tween(260))) togetherWith
                        (fadeOut(tween(180)) + scaleOut(tween(220), targetScale = 0.94f))
                    else -> (fadeIn(tween(260)) + scaleIn(spring(0.8f, 500f), initialScale = 1.04f)) togetherWith
                        (slideOutVertically(tween(220)) { it / 4 } + fadeOut(tween(180)))
                }
            },
            label = "destination",
        ) { d ->
            val padded = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            when (d) {
                Destination.HOME -> HomeScreen(
                    vm,
                    onWindDown = { dest = Destination.WIND_DOWN },
                    onFocus = { dest = Destination.FOCUS },
                    onSounds = { dest = Destination.SOUNDS },
                    onInsights = { dest = Destination.INSIGHTS },
                    onFriends = { dest = Destination.FRIENDS },
                    modifier = padded,
                )
                Destination.SOUNDS -> SoundsScreen(vm, padded)
                Destination.INSIGHTS -> InsightsScreen(vm, onWindDown = { dest = Destination.WIND_DOWN }, onFriends = { dest = Destination.FRIENDS }, modifier = padded)
                Destination.FRIENDS -> FriendsScreen(vm, padded)
                Destination.FOCUS -> FocusScreen(vm, onBack = { dest = Destination.HOME }, modifier = padded)
                Destination.WIND_DOWN -> WindDownScreen(vm, onBack = { dest = Destination.HOME }, onStartSleep = {
                    vm.startSleep()
                    dest = Destination.SLEEP
                })
                Destination.SLEEP -> SleepModeScreen(
                    vm,
                    onWake = { logged -> dest = if (logged) Destination.CHECK_IN else Destination.HOME },
                    onBack = { dest = Destination.WIND_DOWN },
                )
                Destination.CHECK_IN -> CheckInScreen(vm, onDone = { dest = Destination.INSIGHTS }, modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing))
            }
        }

        AnimatedVisibility(
            visible = dest !in fullScreen,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            NavBar(dest, onSelect = { dest = it }, onMoon = { dest = Destination.WIND_DOWN })
        }
    }
    }
    }
}

/** Floating nav: four tabs and a raised moon in the middle that starts the bedtime flow. */
@Composable
private fun NavBar(current: Destination, onSelect: (Destination) -> Unit, onMoon: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(18.dp, RoundedCornerShape(36.dp), ambientColor = Color(0x332B2238), spotColor = Color(0x332B2238))
                .clip(RoundedCornerShape(36.dp))
                .background(Palette.Card)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            NavItem(Glyph.HOME, "Home", current == Destination.HOME) { onSelect(Destination.HOME) }
            NavItem(Glyph.SOUNDS, "Sounds", current == Destination.SOUNDS) { onSelect(Destination.SOUNDS) }
            Box(Modifier.size(64.dp))
            NavItem(Glyph.CHART, "Insights", current == Destination.INSIGHTS) { onSelect(Destination.INSIGHTS) }
            NavItem(Glyph.FRIENDS, "Friends", current == Destination.FRIENDS) { onSelect(Destination.FRIENDS) }
        }
        Box(
            Modifier
                .offset(y = (-26).dp)
                .floaty(amplitude = 2.5f, periodMs = 1800)
                .size(64.dp)
                .shadow(14.dp, CircleShape, ambientColor = Palette.AccentDeep, spotColor = Palette.AccentDeep)
                .clip(CircleShape)
                .background(Palette.DarkInk)
                .pressable(onClick = onMoon)
                .semantics { contentDescription = "Wind down and sleep" }
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(Glyph.MOON, Palette.Moon, size = 28.dp, strokeWidth = 2.2f)
        }
    }
}

@Composable
private fun NavItem(glyph: Glyph, label: String, on: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(if (on) Palette.Ink else Palette.InkMuted, label = "tint")
    val pill by animateColorAsState(if (on) Palette.Accent else Color.Transparent, label = "pill")
    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .pressable(role = Role.Tab, onClick = onClick)
            .semantics { selected = on }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The icon hops and wiggles when its tab becomes selected.
        val hop = remember { Animatable(0f) }
        LaunchedEffect(on) {
            if (on) {
                hop.snapTo(0f)
                hop.animateTo(1f, spring(0.3f, 380f))
            }
        }
        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(pill).padding(horizontal = 14.dp, vertical = 4.dp)) {
            GlyphIcon(
                glyph, tint, size = 22.dp,
                modifier = Modifier.graphicsLayer {
                    val k = kotlin.math.sin(hop.value * Math.PI.toFloat())
                    translationY = -8f * density * k
                    rotationZ = 10f * k * (if (hop.value < 0.5f) 1f else -1f)
                    scaleX = 1f + 0.15f * k; scaleY = 1f + 0.15f * k
                },
            )
        }
        Text(label, style = Type.Small, color = tint)
    }
}
