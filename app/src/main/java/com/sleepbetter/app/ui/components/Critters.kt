@file:Suppress("DEPRECATION") // quadraticBezierTo: renamed quadraticTo in newer Compose; this works in all versions.

package com.sleepbetter.app.ui.components

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** The sleepy crew. Drawn in code (120×100 design units) so they scale crisply and cost no assets. */
enum class Critter(val description: String) {
    PANDA("Pip the panda"),
    DINO("Dozy the dinosaur"),
    FOX("Ember the fox"),
    OWL("Hoot the owl"),
    SLOTH("Slo the sloth"),
    KOALA("A koala"),
    CAT("A cat"),
}

/** True when the user turned animations off in system settings. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * A character with idle life: breathing, blinking and an optional bob.
 * [beat] (0..1, from the music engine) makes the character nod on the beat.
 */
@Composable
fun CritterView(
    critter: Critter,
    modifier: Modifier = Modifier,
    sleeping: Boolean = false,
    headphones: Boolean = false,
    silhouette: Boolean = false,
    beat: Float? = null,
) {
    val still = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "critter")
    val breath by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(tween(if (sleeping) 2600 else 1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4200
                1f at 0
                1f at 3800
                0.1f at 3920
                1f at 4040
            },
        ),
        label = "blink",
    )
    Canvas(
        modifier
            .aspectRatio(1.2f)
            .semantics { contentDescription = critter.description },
    ) {
        val k = size.width / 120f
        val b = if (still) 1f else breath
        val nod = if (still || beat == null) 0f else kotlin.math.sin(beat * Math.PI.toFloat()) * 3f
        withTransform({
            scale(k, k, pivot = Offset.Zero)
            translate(top = nod)
            scale(b, 2f * b - 1f, pivot = Offset(60f, 95f)) // breathe: a little taller than wider
        }) {
            val eyes = if (still) 1f else blink
            if (silhouette) drawSilhouette(critter) else when (critter) {
                Critter.PANDA -> drawPanda(sleeping, headphones, eyes)
                Critter.DINO -> drawDino(sleeping, headphones, eyes)
                Critter.FOX -> drawFox(sleeping, eyes)
                Critter.OWL -> drawOwl(eyes)
                Critter.SLOTH -> drawSloth()
                Critter.KOALA -> drawKoala(sleeping, eyes)
                Critter.CAT -> drawCat(sleeping, eyes)
            }
        }
    }
}

private val Ink = Color(0xFF1B1840)
private val Blush = Color(0x8CFF9DB0)
private val Headphones = Color(0xFFFF7AD9)

private fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, color: Color) =
    drawOval(color, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2))

private fun DrawScope.dot(cx: Float, cy: Float, r: Float, color: Color) = drawCircle(color, r, Offset(cx, cy))

private fun DrawScope.line(color: Color, width: Float, build: Path.() -> Unit) =
    drawPath(Path().apply(build), color, style = Stroke(width, cap = StrokeCap.Round))

private fun DrawScope.shape(color: Color, build: Path.() -> Unit) = drawPath(Path().apply(build), color)

/** Two eyes that blink by squashing vertically around their centre. */
private fun DrawScope.blinkingEyes(lx: Float, rx: Float, y: Float, r: Float, color: Color, blink: Float) {
    scale(1f, blink, pivot = Offset(60f, y)) {
        dot(lx, y, r, color)
        dot(rx, y, r, color)
    }
}

private fun DrawScope.sleepyEyes(lx: Float, rx: Float, y: Float, color: Color, width: Float = 2.4f) {
    line(color, width) {
        moveTo(lx - 6f, y); quadraticBezierTo(lx, y + 4f, lx + 6f, y)
        moveTo(rx - 6f, y); quadraticBezierTo(rx, y + 4f, rx + 6f, y)
    }
}

private fun DrawScope.headphones() {
    line(Headphones, 7f) { moveTo(16f, 54f); quadraticBezierTo(60f, -4f, 104f, 54f) }
    drawRoundRect(Headphones, Offset(8f, 50f), Size(16f, 26f), androidx.compose.ui.geometry.CornerRadius(7f))
    drawRoundRect(Headphones, Offset(96f, 50f), Size(16f, 26f), androidx.compose.ui.geometry.CornerRadius(7f))
}

internal fun DrawScope.drawPanda(sleeping: Boolean, headphones: Boolean, blink: Float) {
    dot(33f, 30f, 12f, Ink)
    dot(87f, 30f, 12f, Ink)
    oval(60f, 58f, 41f, 34f, Color(0xFFF7F7FB))
    rotate(-18f, pivot = Offset(43f, 58f)) { oval(43f, 58f, 11f, 8.5f, Ink) }
    rotate(18f, pivot = Offset(77f, 58f)) { oval(77f, 58f, 11f, 8.5f, Ink) }
    if (sleeping) sleepyEyes(43f, 77f, 59f, Color(0xFFF7F7FB)) else blinkingEyes(45f, 75f, 56f, 4f, Color.White, blink)
    oval(60f, 70f, 5f, 3.5f, Ink)
    if (!sleeping) line(Ink, 2.2f) { moveTo(53f, 76f); quadraticBezierTo(60f, 82f, 67f, 76f) }
    dot(30f, 72f, 5f, Blush)
    dot(90f, 72f, 5f, Blush)
    if (headphones) headphones()
}

private fun DrawScope.drawDino(sleeping: Boolean, headphones: Boolean, blink: Float) {
    val green = Color(0xFF7FCBA8)
    val spikes = Color(0xFF3F9A78)
    shape(spikes) {
        moveTo(34f, 32f); lineTo(42f, 18f); lineTo(50f, 31f); close()
        moveTo(56f, 28f); lineTo(64f, 12f); lineTo(72f, 27f); close()
        moveTo(78f, 32f); lineTo(86f, 18f); lineTo(94f, 32f); close()
    }
    oval(60f, 60f, 42f, 34f, green)
    if (sleeping) sleepyEyes(46f, 74f, 56f, Color(0xFF062A22), 3f) else blinkingEyes(46f, 74f, 56f, 5f, Color(0xFF062A22), blink)
    line(Color(0xFF062A22), 2.6f) { moveTo(52f, 72f); quadraticBezierTo(60f, 79f, 68f, 72f) }
    dot(36f, 68f, 5f, Blush)
    dot(84f, 68f, 5f, Blush)
    if (headphones) headphones()
}

internal fun DrawScope.drawFox(sleeping: Boolean, blink: Float) {
    val orange = Color(0xFFE58A4E)
    shape(orange) { moveTo(88f, 80f); quadraticBezierTo(118f, 74f, 116f, 46f); quadraticBezierTo(110f, 68f, 86f, 68f); close() }
    shape(orange) {
        moveTo(22f, 10f); lineTo(40f, 36f); lineTo(16f, 40f); close()
        moveTo(98f, 10f); lineTo(80f, 36f); lineTo(104f, 40f); close()
    }
    shape(Color(0xFF7A3F2A)) {
        moveTo(26f, 18f); lineTo(36f, 34f); lineTo(24f, 36f); close()
        moveTo(94f, 18f); lineTo(84f, 34f); lineTo(96f, 36f); close()
    }
    oval(60f, 62f, 42f, 36f, orange)
    shape(Color(0xFFF7F7FB)) { moveTo(20f, 66f); quadraticBezierTo(60f, 104f, 100f, 66f); quadraticBezierTo(80f, 92f, 60f, 92f); quadraticBezierTo(40f, 92f, 20f, 66f); close() }
    if (sleeping) sleepyEyes(44f, 76f, 57f, Ink) else blinkingEyes(44f, 76f, 56f, 5f, Ink, blink)
    oval(60f, 70f, 5f, 3.5f, Ink)
}

internal fun DrawScope.drawOwl(blink: Float) {
    shape(Color(0xFF6A5BD6)) {
        moveTo(30f, 26f); lineTo(40f, 40f); lineTo(26f, 42f); close()
        moveTo(90f, 26f); lineTo(80f, 40f); lineTo(94f, 42f); close()
    }
    oval(60f, 58f, 36f, 38f, Color(0xFF8C7CF0))
    oval(60f, 72f, 20f, 18f, Color(0xFFC9C1FF))
    dot(45f, 48f, 13f, Color.White)
    dot(75f, 48f, 13f, Color.White)
    blinkingEyes(45f, 75f, 49f, 6f, Ink, blink)
    shape(Color(0xFFF4B860)) { moveTo(56f, 60f); lineTo(60f, 67f); lineTo(64f, 60f); close() }
}

private fun DrawScope.drawSloth() {
    dot(60f, 55f, 40f, Color(0xFFB58B6A))
    oval(60f, 60f, 30f, 26f, Color(0xFFE8D2B8))
    line(Color(0xFF5B3E2B), 7f) {
        moveTo(34f, 54f); quadraticBezierTo(44f, 46f, 56f, 56f)
        moveTo(64f, 56f); quadraticBezierTo(76f, 46f, 86f, 54f)
    }
    sleepyEyes(47f, 73f, 56f, Color(0xFF2A1B12), 2.2f)
    oval(60f, 67f, 5f, 3.5f, Color(0xFF2A1B12))
    line(Color(0xFF2A1B12), 2f) { moveTo(54f, 75f); quadraticBezierTo(60f, 79f, 66f, 75f) }
}

internal fun DrawScope.drawKoala(sleeping: Boolean, blink: Float) {
    val grey = Color(0xFFA9A9C2)
    dot(28f, 40f, 20f, grey)
    dot(92f, 40f, 20f, grey)
    dot(28f, 40f, 11f, Color(0xFFF2C6D4))
    dot(92f, 40f, 11f, Color(0xFFF2C6D4))
    oval(60f, 60f, 38f, 34f, grey)
    if (sleeping) sleepyEyes(46f, 74f, 56f, Ink) else blinkingEyes(46f, 74f, 55f, 4.5f, Ink, blink)
    oval(60f, 68f, 8f, 10f, Color(0xFF3A3550))
}

internal fun DrawScope.drawCat(sleeping: Boolean, blink: Float) {
    val fur = Color(0xFFF2A65A)
    shape(fur) {
        moveTo(30f, 20f); lineTo(44f, 42f); lineTo(24f, 42f); close()
        moveTo(90f, 20f); lineTo(76f, 42f); lineTo(96f, 42f); close()
    }
    oval(60f, 60f, 38f, 34f, fur)
    line(Color(0xFFC97A33), 3f) { moveTo(52f, 30f); lineTo(54f, 40f); moveTo(60f, 28f); lineTo(60f, 40f); moveTo(68f, 30f); lineTo(66f, 40f) }
    if (sleeping) sleepyEyes(46f, 74f, 58f, Ink) else blinkingEyes(46f, 74f, 57f, 4.5f, Ink, blink)
    shape(Color(0xFFFF9DB0)) { moveTo(56f, 66f); lineTo(64f, 66f); lineTo(60f, 71f); close() }
}

private fun DrawScope.drawSilhouette(critter: Critter) {
    val c = Color(0xFF2A2E66)
    when (critter) {
        Critter.KOALA -> {
            dot(28f, 40f, 20f, c); dot(92f, 40f, 20f, c); oval(60f, 60f, 38f, 34f, c)
        }
        Critter.CAT -> {
            shape(c) {
                moveTo(30f, 20f); lineTo(44f, 42f); lineTo(24f, 42f); close()
                moveTo(90f, 20f); lineTo(76f, 42f); lineTo(96f, 42f); close()
            }
            oval(60f, 60f, 38f, 34f, c)
        }
        else -> oval(60f, 58f, 40f, 36f, c)
    }
}
