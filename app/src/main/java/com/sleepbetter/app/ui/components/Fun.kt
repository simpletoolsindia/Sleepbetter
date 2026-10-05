package com.sleepbetter.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.core.audio.SoundId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Emoji for each sound, used on tiles, chips and confetti. */
fun SoundId.emoji(): String = when (this) {
    SoundId.RAIN -> "🌧️"
    SoundId.DOWNPOUR -> "☔"
    SoundId.THUNDER -> "⚡"
    SoundId.TENT -> "⛺"
    SoundId.CAR -> "🚗"
    SoundId.CAMPFIRE -> "🔥"
    SoundId.NIGHT_FOREST -> "🌲"
    SoundId.BIRDS -> "🐦"
    SoundId.WATER_DROPS -> "💧"
    SoundId.STREAM -> "🏞️"
    SoundId.BROWN_NOISE -> "🟤"
    SoundId.FOCUS_MUSIC -> "🎹"
    SoundId.SEA -> "🌊"
}

/** One shower of emoji confetti. Its start time is set on the first frame it is drawn. */
internal class Burst(val emojis: List<String>, val origin: Offset, val count: Int) {
    var start = 0L
    val parts = List(count) {
        val angle = (-PI / 2 + (Random.nextFloat() - 0.5f) * PI * 0.95f).toFloat()
        Part(
            emoji = emojis[it % emojis.size],
            angle = angle,
            speed = 900f + Random.nextFloat() * 900f,
            spin = (Random.nextFloat() - 0.5f) * 540f,
            size = 20f + Random.nextFloat() * 16f,
            delay = Random.nextFloat() * 0.12f,
        )
    }
}

internal class Part(val emoji: String, val angle: Float, val speed: Float, val spin: Float, val size: Float, val delay: Float)

/**
 * Emoji confetti for happy moments: saving a mix, a good night, a friend
 * saying hi. Call [fire] from anywhere under [EmojiBurstHost].
 */
class BurstState {
    internal val bursts = mutableStateListOf<Burst>()
    internal var still = false

    /** [origin] is where on screen it starts, as fractions of width and height. */
    fun fire(emojis: List<String>, origin: Offset = Offset(0.5f, 0.55f), count: Int = 16) {
        if (emojis.isEmpty()) return
        bursts += Burst(emojis, origin, if (still) 3 else count)
    }
}

val LocalBurst = staticCompositionLocalOf { BurstState() }

private const val LIFE = 1.6f
private const val GRAVITY = 2600f

/** Draws every running burst above [content]. Put it once near the root of the UI. */
@Composable
fun EmojiBurstHost(state: BurstState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val still = rememberReduceMotion()
    state.still = still
    val context = LocalContext.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state) {
        snapshotFlow { state.bursts.size }.collect { n ->
            if (n == 0) return@collect
            Haptics.tick(context)
            while (state.bursts.isNotEmpty()) {
                withFrameNanos { t ->
                    now = t
                    state.bursts.forEach { if (it.start == 0L) it.start = t }
                    state.bursts.removeAll { (t - it.start) / 1e9f > LIFE + 0.2f }
                }
            }
        }
    }
    Box(modifier.onSizeChanged { size = it }) {
        content()
        state.bursts.forEach { burst ->
            if (burst.start == 0L) return@forEach
            val t0 = (now - burst.start) / 1e9f
            burst.parts.forEach { p ->
                val t = t0 - p.delay
                if (t < 0f || t > LIFE) return@forEach
                val x = burst.origin.x * size.width + cos(p.angle) * p.speed * t
                val y = burst.origin.y * size.height + sin(p.angle) * p.speed * t + 0.5f * GRAVITY * t * t
                val fade = 1f - (t / LIFE).let { it * it }
                Text(
                    p.emoji,
                    fontSize = p.size.sp,
                    modifier = Modifier.graphicsLayer {
                        translationX = x - p.size
                        translationY = y - p.size
                        rotationZ = if (still) 0f else p.spin * t
                        alpha = fade
                        val pop = (t / 0.15f).coerceAtMost(1f)
                        scaleX = pop; scaleY = pop
                    },
                )
            }
        }
    }
}

/** A gentle idle bob, so resting characters and stickers never look frozen. */
fun Modifier.floaty(amplitude: Float = 6f, periodMs: Int = 2600, phase: Float = 0f): Modifier = composed {
    val still = rememberReduceMotion()
    if (still) return@composed this
    val t by rememberInfiniteTransition(label = "floaty").animateFloat(
        0f, 1f, infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob",
    )
    graphicsLayer {
        val wave = sin((t + phase) * PI.toFloat())
        translationY = -amplitude * density * wave
        rotationZ = (wave - 0.5f) * 3f
    }
}

/** A comic-style speech bubble with a little tail on the left that pops in when its text changes. */
@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier, color: Color = Palette.Card, textColor: Color = Palette.Ink) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(spring(0.5f, 500f), initialScale = 0.7f) + slideInVertically { it / 4 }) togetherWith fadeOut(tween(120))
        },
        label = "bubble",
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) { line ->
        Row(verticalAlignment = Alignment.Bottom) {
            Canvas(Modifier.size(10.dp, 14.dp)) {
                drawPath(Path().apply { moveTo(size.width, 0f); lineTo(0f, size.height); lineTo(size.width, size.height * 0.55f); close() }, color)
            }
            Text(
                line,
                style = Type.Label,
                color = textColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp))
                    .background(color)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

/**
 * Pico (and friends) chatting: a small character next to a speech bubble
 * that cycles through [lines]. The character hops each time a new line comes.
 */
@Composable
fun MochiSays(lines: List<String>, modifier: Modifier = Modifier, everyMs: Long = 5200, speakers: List<Species> = listOf(Species.PICO)) {
    if (lines.isEmpty()) return
    val still = rememberReduceMotion()
    var index by remember(lines) { mutableStateOf(0) }
    LaunchedEffect(lines, still) {
        if (still) return@LaunchedEffect
        while (true) {
            delay(everyMs)
            index = (index + 1) % lines.size
        }
    }
    val hop = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(index) {
        if (still) return@LaunchedEffect
        hop.snapTo(0f)
        hop.animateTo(1f, spring(0.4f, 300f))
    }
    val burst = LocalBurst.current
    // Friends take turns: each new line can come from a different one.
    val species = speakers.ifEmpty { listOf(Species.PICO) }.let { it[index % it.size] }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // Tap the character to hear the next line.
        MochiView(
            Modifier
                .size(58.dp)
                .pressable {
                    index = (index + 1) % lines.size
                    burst.fire(listOf("💜", "✨", species.emoji()), Offset(0.15f, 0.16f), count = 8)
                }
                .graphicsLayer { translationY = -14f * sin(hop.value * PI.toFloat()).coerceAtLeast(0f) },
            species = species,
            mood = 3.6f,
        )
        SpeechBubble(lines[index % lines.size], Modifier.padding(start = 2.dp))
    }
}

/** A pill with a pulsing emoji: streaks, badges and little wins. */
@Composable
fun EmojiChip(emoji: String, text: String, modifier: Modifier = Modifier, color: Color = Palette.Card, pulse: Boolean = true) {
    val still = rememberReduceMotion()
    val s by rememberInfiniteTransition(label = "chip").animateFloat(
        1f, 1.18f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
    )
    Row(
        modifier.clip(RoundedCornerShape(22.dp)).background(color).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 16.sp, modifier = Modifier.graphicsLayer { val k = if (still || !pulse) 1f else s; scaleX = k; scaleY = k })
        Text(text, style = Type.Label, color = Palette.Ink, modifier = Modifier.padding(start = 6.dp))
    }
}

/** Big emoji that bounces in whenever it changes (moods, results). */
@Composable
fun BouncyEmoji(emoji: String, modifier: Modifier = Modifier, sizeSp: Int = 44) {
    AnimatedContent(
        targetState = emoji,
        transitionSpec = { (scaleIn(spring(0.35f, 400f), initialScale = 0.3f) + fadeIn()) togetherWith fadeOut(tween(90)) },
        label = "emoji",
        modifier = modifier,
    ) { e -> Text(e, fontSize = sizeSp.sp) }
}

/** Slow drifting emoji in the background of a card (stars, Zs, leaves). Decorative only. */
@Composable
fun DriftingEmoji(emojis: List<String>, modifier: Modifier = Modifier, count: Int = 6) {
    val still = rememberReduceMotion()
    val seeds = remember(emojis) { List(count) { Triple(Random.nextFloat(), Random.nextFloat(), emojis[it % emojis.size]) } }
    val clock by rememberClock()
    var area by remember { mutableStateOf(IntSize.Zero) }
    Box(modifier.fillMaxSize().onSizeChanged { area = it }) {
        if (area == IntSize.Zero) return@Box
        val w = area.width.toFloat()
        val h = area.height.toFloat()
        seeds.forEachIndexed { i, (sx, sy, e) ->
            val t = if (still) 0f else clock
            Text(
                e,
                fontSize = (12 + (i % 3) * 4).sp,
                modifier = Modifier.graphicsLayer {
                    translationX = sx * (w - 40f) + sin(t * 0.5f + i) * 10f
                    // Rise slowly and wrap around, fading in at the bottom and out at the top.
                    val y = (((sy * h - t * (12f + i * 3f)) % h) + h) % h
                    translationY = y
                    val edge = (minOf(y, h - y) / (h * 0.18f)).coerceIn(0f, 1f)
                    alpha = 0.6f * edge
                    rotationZ = sin(t * 0.8f + i) * 12f
                },
            )
        }
    }
}

/** Emoji for each friend, used in greetings and confetti. */
fun Species.emoji(): String = when (this) {
    Species.PICO -> "🦕"
    Species.LULU -> "🐣"
    Species.MOCHI -> "🐼"
    Species.REX -> "🦖"
    Species.TRIKE -> "🌋"
    Species.STEGO -> "💗"
    Species.BRONTO -> "🦕"
    Species.PTERO -> "🪽"
    Species.ANKY -> "🛡️"
    Species.TOFFEE -> "🦫"
    Species.ELEPHANT -> "🐘"
    Species.FOX -> "🦊"
    Species.OWL -> "🦉"
    Species.DINO -> "🦕"
    Species.KOALA -> "🐨"
    Species.CAT -> "🐱"
}

/**
 * A dino egg that is getting ready to hatch. It rocks more as [progress]
 * (0..1) grows, cracks appear past halfway, and near the end a pair of eyes
 * peeks out. Tapping it gives a big wobble.
 */
@Composable
fun DinoEgg(progress: Float, modifier: Modifier = Modifier, spots: Color = Color(0xFFA3E4C8)) {
    val still = rememberReduceMotion()
    val clock by rememberClock()
    val nudge = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val p = progress.coerceIn(0f, 1f)
    Canvas(
        modifier
            .pressable {
                scope.launch {
                    nudge.snapTo(1f)
                    nudge.animateTo(0f, tween(900))
                }
            }
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        val t = if (still) 0f else clock
        // Rock in little bursts, more often as hatching gets close.
        val burst = sin(t * (1.2f + 2f * p)).coerceAtLeast(0f).let { it * it * it }
        val angle = (3f + 12f * p) * burst * sin(t * 14f) + 18f * nudge.value * sin(t * 22f)
        val k = size.minDimension / 100f
        rotate(angle, pivot = Offset(size.width / 2f, size.height * 0.92f)) {
            scale(k, k, pivot = Offset.Zero) {
                val egg = Path().apply {
                    moveTo(50f, 6f)
                    cubicTo(78f, 6f, 90f, 52f, 86f, 70f)
                    cubicTo(82f, 90f, 66f, 96f, 50f, 96f)
                    cubicTo(34f, 96f, 18f, 90f, 14f, 70f)
                    cubicTo(10f, 52f, 22f, 6f, 50f, 6f)
                    close()
                }
                drawOval(Color(0x1A2B2238), Offset(20f, 90f), androidx.compose.ui.geometry.Size(60f, 8f))
                drawPath(egg, Color(0xFFFFFBF2))
                listOf(Triple(34f, 40f, 7f), Triple(62f, 30f, 5f), Triple(66f, 62f, 8f), Triple(36f, 72f, 5f)).forEach { (x, y, r) ->
                    drawCircle(spots, r, Offset(x, y))
                }
                drawPath(egg, Color(0xFF3A2622), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
                if (p > 0.5f) {
                    val crack = Path().apply { moveTo(26f, 50f); lineTo(36f, 44f); lineTo(42f, 54f); lineTo(52f, 46f); lineTo(58f, 55f); lineTo(68f, 47f); lineTo(76f, 52f) }
                    drawPath(crack, Color(0xFF3A2622), style = androidx.compose.ui.graphics.drawscope.Stroke(2.2f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                }
                if (p > 0.85f) {
                    // Someone is peeking out.
                    val blink = if ((t % 3.2f) < 0.12f) 0.15f else 1f
                    scale(1f, blink, pivot = Offset(50f, 52f)) {
                        drawOval(Color(0xFF3A2622), Offset(40f, 47f), androidx.compose.ui.geometry.Size(6f, 8f))
                        drawOval(Color(0xFF3A2622), Offset(54f, 47f), androidx.compose.ui.geometry.Size(6f, 8f))
                    }
                }
            }
        }
    }
}
