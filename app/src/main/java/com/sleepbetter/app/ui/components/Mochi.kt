@file:Suppress("DEPRECATION") // quadraticBezierTo is renamed quadraticTo in newer Compose; this works in all versions.

package com.sleepbetter.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sleepbetter.core.sleep.Visitor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mochi: a soft cloud-blob who sleeps when you sleep. Every friend is a Mochi
 * with a different colour and a few features (ears, tufts, spikes), so the
 * whole cast reads as one family.
 */
enum class Species(val body: Color, val shade: Color, val description: String) {
    MOCHI(Color(0xFFFFFFFF), Color(0xFFE4DDF7), "Mochi"),
    FOX(Color(0xFFFFB98E), Color(0xFFF29A68), "Ember the fox"),
    OWL(Color(0xFFC9BCF7), Color(0xFFA996F0), "Hoot the owl"),
    DINO(Color(0xFFBFD8A9), Color(0xFF9DBF8A), "Dozy the dinosaur"),
    KOALA(Color(0xFFD9D6E2), Color(0xFFBDB8CC), "Koko the koala"),
    CAT(Color(0xFFFFE08A), Color(0xFFF2C95C), "Purr the cat"),
}

fun Visitor.species(): Species = when (this) {
    Visitor.PIP -> Species.MOCHI
    Visitor.EMBER -> Species.FOX
    Visitor.HOOT -> Species.OWL
    Visitor.DOZY -> Species.DINO
    Visitor.KOALA -> Species.KOALA
    Visitor.CAT -> Species.CAT
}

/** How Mochi feels. 0 = awful … 4 = great; values in between morph smoothly. */
enum class Mood(val label: String, val tint: Color) {
    AWFUL("Awful", Color(0xFFD9D6E2)),
    POOR("Poor", Color(0xFFFFC2A6)),
    OKAY("Okay", Color(0xFFFFE08A)),
    GOOD("Good", Color(0xFFBFD8A9)),
    GREAT("Great", Color(0xFFC9BCF7)),
}

/**
 * A living character. [mood] is 0..4 and morphs the face continuously, so a
 * slider can drive it. [beat] (0..1) makes it nod to music.
 */
@Composable
fun MochiView(
    modifier: Modifier = Modifier,
    species: Species = Species.MOCHI,
    sleeping: Boolean = false,
    mood: Float = 3f,
    headphones: Boolean = false,
    silhouette: Boolean = false,
    beat: Float? = null,
    tintBody: Color? = null,
) {
    val still = rememberReduceMotion()
    val t = rememberInfiniteTransition(label = "mochi")
    val breath by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(if (sleeping) 2800 else 2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val blink by t.animateFloat(
        1f, 1f,
        infiniteRepeatable(keyframes { durationMillis = 4400; 1f at 0; 1f at 4000; 0.08f at 4110; 1f at 4230 }),
        label = "blink",
    )
    val wobble by t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(6000)), label = "wobble")
    val moodAnim by animateFloatAsState(mood, spring(0.6f, 300f), label = "mood")
    val body = tintBody ?: species.body

    Canvas(
        modifier
            .aspectRatio(1.2f)
            .semantics { contentDescription = if (silhouette) "A friend yet to arrive" else species.description },
    ) {
        val k = size.width / 120f
        val b = if (still) 0f else breath
        val nod = if (still || beat == null) 0f else sin(beat * PI.toFloat()) * 3f
        withTransform({
            scale(k, k, pivot = Offset.Zero)
            translate(top = nod)
            scale(1f + b * 0.025f, 1f + b * 0.045f, pivot = Offset(60f, 96f))
        }) {
            drawCharacter(
                species = species,
                body = if (silhouette) Color(0xFFE6E0EA) else body,
                shade = if (silhouette) Color(0xFFD9D2E0) else species.shade,
                sleeping = sleeping,
                mood = moodAnim,
                blink = if (still) 1f else blink,
                wobble = if (still) 0f else wobble,
                headphones = headphones,
                face = !silhouette,
            )
        }
    }
}

private val Ink = Color(0xFF2B2238)
private val Cheek = Color(0x66FF8FA8)

/** The blob: a soft superellipse with a flatter bottom, gently wobbling. */
internal fun blobPath(wobble: Float, cx: Float = 60f, cy: Float = 60f, rx: Float = 46f, ry: Float = 38f): Path {
    val path = Path()
    val steps = 48
    for (i in 0..steps) {
        val a = i / steps.toFloat() * 2f * PI.toFloat()
        val c = cos(a)
        val s = sin(a)
        // Superellipse-ish: push towards a rounded square, flatter at the bottom.
        val squish = 1f + 0.06f * (1f - c * c) * (if (s > 0) 1.4f else 0.6f)
        val w = 1f + 0.018f * sin(3f * a + wobble)
        val x = cx + rx * c * squish * w
        val y = cy + ry * s * (if (s > 0) 0.92f else 1.05f) * w
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

internal fun DrawScope.drawCharacter(
    species: Species,
    body: Color,
    shade: Color,
    sleeping: Boolean,
    mood: Float,
    blink: Float,
    wobble: Float,
    headphones: Boolean,
    face: Boolean = true,
) {
    // Features behind the body.
    when (species) {
        Species.FOX -> {
            drawPath(Path().apply { moveTo(26f, 36f); lineTo(30f, 6f); lineTo(52f, 26f); close() }, body)
            drawPath(Path().apply { moveTo(94f, 36f); lineTo(90f, 6f); lineTo(68f, 26f); close() }, body)
            drawPath(Path().apply { moveTo(31f, 30f); lineTo(33f, 14f); lineTo(45f, 25f); close() }, shade)
            drawPath(Path().apply { moveTo(89f, 30f); lineTo(87f, 14f); lineTo(75f, 25f); close() }, shade)
        }
        Species.OWL -> {
            drawPath(Path().apply { moveTo(24f, 34f); quadraticBezierTo(22f, 14f, 36f, 10f); quadraticBezierTo(34f, 22f, 42f, 28f); close() }, shade)
            drawPath(Path().apply { moveTo(96f, 34f); quadraticBezierTo(98f, 14f, 84f, 10f); quadraticBezierTo(86f, 22f, 78f, 28f); close() }, shade)
        }
        Species.DINO -> {
            for (i in 0..3) {
                val x = 36f + i * 16f
                drawPath(Path().apply { moveTo(x - 7f, 26f); quadraticBezierTo(x, 8f, x + 7f, 26f); close() }, shade)
            }
        }
        Species.KOALA -> {
            drawCircle(body, 17f, Offset(22f, 34f)); drawCircle(Color(0xFFF6C3D6), 9f, Offset(22f, 34f))
            drawCircle(body, 17f, Offset(98f, 34f)); drawCircle(Color(0xFFF6C3D6), 9f, Offset(98f, 34f))
        }
        Species.CAT -> {
            drawPath(Path().apply { moveTo(28f, 34f); lineTo(32f, 10f); lineTo(50f, 26f); close() }, body)
            drawPath(Path().apply { moveTo(92f, 34f); lineTo(88f, 10f); lineTo(70f, 26f); close() }, body)
        }
        Species.MOCHI -> Unit
    }

    // Soft shadow under the body, then the body with a shaded underside.
    drawOval(Color(0x1A2B2238), Offset(22f, 90f), Size(76f, 8f))
    val blob = blobPath(wobble)
    drawPath(blob, shade)
    withTransform({ translate(top = -3f); scale(0.97f, 0.95f, pivot = Offset(60f, 40f)) }) { drawPath(blobPath(wobble), body) }

    if (species == Species.OWL) drawOval(Color(0x66FFFFFF), Offset(42f, 62f), Size(36f, 26f))
    if (species == Species.CAT) {
        val whisker = Stroke(1.6f, cap = StrokeCap.Round)
        drawLine(Ink.copy(alpha = 0.5f), Offset(16f, 66f), Offset(30f, 68f), whisker.width, StrokeCap.Round)
        drawLine(Ink.copy(alpha = 0.5f), Offset(104f, 66f), Offset(90f, 68f), whisker.width, StrokeCap.Round)
    }
    if (!face) return

    // Face. Mood 0..4 bends the mouth from frown to big smile and changes the eyes.
    val smile = (mood - 2f) / 2f // -1..1
    val eyeY = 56f
    val lx = 45f
    val rx = 75f
    if (sleeping) {
        val s = Stroke(3f, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(lx - 6f, eyeY); quadraticBezierTo(lx, eyeY + 5f, lx + 6f, eyeY) }, Ink, style = s)
        drawPath(Path().apply { moveTo(rx - 6f, eyeY); quadraticBezierTo(rx, eyeY + 5f, rx + 6f, eyeY) }, Ink, style = s)
    } else if (mood > 3.5f) {
        // Delighted: happy closed arcs.
        val s = Stroke(3.2f, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(lx - 6f, eyeY + 2f); quadraticBezierTo(lx, eyeY - 5f, lx + 6f, eyeY + 2f) }, Ink, style = s)
        drawPath(Path().apply { moveTo(rx - 6f, eyeY + 2f); quadraticBezierTo(rx, eyeY - 5f, rx + 6f, eyeY + 2f) }, Ink, style = s)
    } else {
        scale(1f, blink, pivot = Offset(60f, eyeY)) {
            drawOval(Ink, Offset(lx - 4.5f, eyeY - 6f), Size(9f, 12f))
            drawOval(Ink, Offset(rx - 4.5f, eyeY - 6f), Size(9f, 12f))
            drawCircle(Color.White, 1.8f, Offset(lx + 1.5f, eyeY - 2.5f))
            drawCircle(Color.White, 1.8f, Offset(rx + 1.5f, eyeY - 2.5f))
        }
        if (mood < 1.5f) {
            // Tired lids for a rough night.
            val lid = Stroke(2.6f, cap = StrokeCap.Round)
            val droop = (1.5f - mood) * 4f
            drawLine(Ink, Offset(lx - 7f, eyeY - 6f - droop), Offset(lx + 6f, eyeY - 7f + droop), lid.width, StrokeCap.Round)
            drawLine(Ink, Offset(rx - 6f, eyeY - 7f + droop), Offset(rx + 7f, eyeY - 6f - droop), lid.width, StrokeCap.Round)
        }
    }
    drawCircle(Cheek, 6f, Offset(31f, 68f))
    drawCircle(Cheek, 6f, Offset(89f, 68f))
    if (sleeping) {
        drawOval(Ink, Offset(57f, 70f), Size(6f, 4f))
    } else {
        val mouthW = 9f + 3f * kotlin.math.abs(smile)
        val curve = 9f * smile
        drawPath(
            Path().apply { moveTo(60f - mouthW, 71f - curve * 0.2f); quadraticBezierTo(60f, 71f + curve, 60f + mouthW, 71f - curve * 0.2f) },
            Ink,
            style = Stroke(3f, cap = StrokeCap.Round),
        )
    }
    if (headphones) {
        val band = lerp(Color(0xFF6C55D9), body, 0.1f)
        drawPath(Path().apply { moveTo(14f, 56f); quadraticBezierTo(60f, -10f, 106f, 56f) }, band, style = Stroke(7f, cap = StrokeCap.Round))
        drawRoundRect(band, Offset(6f, 50f), Size(16f, 26f), androidx.compose.ui.geometry.CornerRadius(8f))
        drawRoundRect(band, Offset(98f, 50f), Size(16f, 26f), androidx.compose.ui.geometry.CornerRadius(8f))
    }
}
