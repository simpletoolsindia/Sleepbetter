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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.core.sleep.Visitor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mochi: a round little rice-cake friend with a crescent-moon clip, who sleeps
 * when you sleep. Every friend shares Mochi's shape (big head, small body,
 * thick soft outline) with its own colour and features, so the cast reads as
 * one family. Original designs, drawn in code.
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

/** Sticker-style outline: a warm dark brown reads softer than black. */
private val Outline = Color(0xFF3A2622)
private val SilhouetteOutline = Color(0xFFCFC6D8)

/** The head: a soft superellipse with a flatter bottom, gently wobbling. */
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

/**
 * Draws one character in a 120 x 100 box: a big round head on a small body
 * with stubby arms and feet, thick warm outlines and big blush cheeks (the
 * kawaii sticker look). [withBody] = false draws the head alone, curled up.
 */
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
    withBody: Boolean = true,
) {
    val line = if (face) Outline else SilhouetteOutline
    val stroke = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun shape(path: Path, fill: Color) {
        drawPath(path, fill)
        drawPath(path, line, style = stroke)
    }
    fun oval(topLeft: Offset, size: Size, fill: Color) = shape(Path().apply { addOval(Rect(topLeft, size)) }, fill)

    // Ground shadow.
    drawOval(Color(0x1A2B2238), Offset(24f, 91f), Size(72f, 8f))

    if (withBody) {
        // Feet first, so the body sits on them and only their soles show.
        oval(Offset(39f, 86f), Size(16f, 10f), shade)
        oval(Offset(65f, 86f), Size(16f, 10f), shade)
        shape(
            Path().apply {
                moveTo(37f, 62f); lineTo(36f, 84f); quadraticBezierTo(36f, 93f, 46f, 93f)
                lineTo(74f, 93f); quadraticBezierTo(84f, 93f, 84f, 84f); lineTo(83f, 62f); close()
            },
            body,
        )
        if (species == Species.OWL) drawOval(Color(0x66FFFFFF), Offset(46f, 72f), Size(28f, 16f))
        // Stubby arms resting in front.
        oval(Offset(31f, 70f), Size(14f, 11f), body)
        oval(Offset(75f, 70f), Size(14f, 11f), body)
    }

    // Features behind the head, nudged up to sit on its crown.
    translate(top = -6f) {
        when (species) {
            Species.FOX -> {
                shape(Path().apply { moveTo(26f, 36f); lineTo(30f, 6f); lineTo(52f, 26f); close() }, body)
                shape(Path().apply { moveTo(94f, 36f); lineTo(90f, 6f); lineTo(68f, 26f); close() }, body)
                drawPath(Path().apply { moveTo(32f, 28f); lineTo(33f, 14f); lineTo(44f, 24f); close() }, shade)
                drawPath(Path().apply { moveTo(88f, 28f); lineTo(87f, 14f); lineTo(76f, 24f); close() }, shade)
            }
            Species.OWL -> {
                shape(Path().apply { moveTo(26f, 34f); quadraticBezierTo(22f, 14f, 36f, 10f); quadraticBezierTo(34f, 22f, 44f, 28f); close() }, shade)
                shape(Path().apply { moveTo(94f, 34f); quadraticBezierTo(98f, 14f, 84f, 10f); quadraticBezierTo(86f, 22f, 76f, 28f); close() }, shade)
            }
            Species.DINO -> for (i in 0..3) {
                val x = 38f + i * 15f
                shape(Path().apply { moveTo(x - 7f, 26f); quadraticBezierTo(x, 8f, x + 7f, 26f); close() }, shade)
            }
            Species.KOALA -> {
                oval(Offset(6f, 14f), Size(32f, 32f), body); drawCircle(Color(0xFFF6C3D6), 8f, Offset(22f, 30f))
                oval(Offset(82f, 14f), Size(32f, 32f), body); drawCircle(Color(0xFFF6C3D6), 8f, Offset(98f, 30f))
            }
            Species.CAT -> {
                shape(Path().apply { moveTo(28f, 34f); lineTo(32f, 10f); lineTo(50f, 26f); close() }, body)
                shape(Path().apply { moveTo(92f, 34f); lineTo(88f, 10f); lineTo(70f, 26f); close() }, body)
            }
            Species.MOCHI -> Unit
        }
    }

    // Head: shaded underside, the body colour on top, then one clean outline.
    val headY = if (withBody) 44f else 56f
    val head = blobPath(wobble, cy = headY, rx = 43f, ry = 34f)
    drawPath(head, shade)
    withTransform({ translate(top = -2.5f); scale(0.96f, 0.93f, pivot = Offset(60f, headY - 16f)) }) {
        drawPath(blobPath(wobble, cy = headY, rx = 43f, ry = 34f), body)
    }
    drawPath(head, line, style = stroke)

    if (species == Species.CAT) {
        drawLine(line.copy(alpha = 0.6f), Offset(12f, headY + 10f), Offset(25f, headY + 11f), 1.8f, StrokeCap.Round)
        drawLine(line.copy(alpha = 0.6f), Offset(108f, headY + 10f), Offset(95f, headY + 11f), 1.8f, StrokeCap.Round)
    }
    if (species == Species.MOCHI && face) {
        // Mochi's own mark: a little crescent-moon clip.
        val clip = Path.combine(
            PathOperation.Difference,
            Path().apply { addOval(Rect(Offset(86f, headY - 26f), 8f)) },
            Path().apply { addOval(Rect(Offset(90f, headY - 29f), 7f)) },
        )
        drawPath(clip, Color(0xFFFFD86B))
        drawPath(clip, line, style = Stroke(2f, join = StrokeJoin.Round))
    }
    if (!face) return

    // Face. Mood 0..4 bends the mouth from a frown to an open smile and changes the eyes.
    val smile = (mood - 2f) / 2f // -1..1
    val eyeY = headY + 2f
    val lx = 44f
    val rx = 76f
    val mouthY = eyeY + 11f
    if (sleeping) {
        val s = Stroke(3f, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(lx - 6f, eyeY); quadraticBezierTo(lx, eyeY + 5f, lx + 6f, eyeY) }, Outline, style = s)
        drawPath(Path().apply { moveTo(rx - 6f, eyeY); quadraticBezierTo(rx, eyeY + 5f, rx + 6f, eyeY) }, Outline, style = s)
    } else if (mood > 3.5f) {
        // Delighted: happy closed arcs.
        val s = Stroke(3.2f, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(lx - 6f, eyeY + 2f); quadraticBezierTo(lx, eyeY - 5f, lx + 6f, eyeY + 2f) }, Outline, style = s)
        drawPath(Path().apply { moveTo(rx - 6f, eyeY + 2f); quadraticBezierTo(rx, eyeY - 5f, rx + 6f, eyeY + 2f) }, Outline, style = s)
    } else {
        scale(1f, blink, pivot = Offset(60f, eyeY)) {
            drawOval(Outline, Offset(lx - 5f, eyeY - 6f), Size(10f, 12f))
            drawOval(Outline, Offset(rx - 5f, eyeY - 6f), Size(10f, 12f))
            drawCircle(Color.White, 2f, Offset(lx + 1.5f, eyeY - 2.5f))
            drawCircle(Color.White, 2f, Offset(rx + 1.5f, eyeY - 2.5f))
        }
        if (mood < 1.5f) {
            // Tired lids for a rough night.
            val droop = (1.5f - mood) * 4f
            drawLine(Outline, Offset(lx - 7f, eyeY - 7f - droop), Offset(lx + 6f, eyeY - 8f + droop), 2.6f, StrokeCap.Round)
            drawLine(Outline, Offset(rx - 6f, eyeY - 8f + droop), Offset(rx + 7f, eyeY - 7f - droop), 2.6f, StrokeCap.Round)
        }
    }
    val cheek = when (species) {
        Species.FOX, Species.CAT -> Color(0x99FF8A5C)
        else -> Color(0xB3FF9AA8)
    }
    drawCircle(cheek, 8f, Offset(29f, eyeY + 10f))
    drawCircle(cheek, 8f, Offset(91f, eyeY + 10f))

    val mouth = Stroke(2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when {
        sleeping -> drawOval(Outline, Offset(57.5f, mouthY - 1f), Size(5f, 4f))
        mood > 3.5f -> {
            // Open, happy mouth with a little tongue.
            val open = Path().apply { moveTo(53f, mouthY - 2f); lineTo(67f, mouthY - 2f); quadraticBezierTo(67f, mouthY + 8f, 60f, mouthY + 8f); quadraticBezierTo(53f, mouthY + 8f, 53f, mouthY - 2f); close() }
            drawPath(open, Outline)
            drawOval(Color(0xFFFF7A8A), Offset(56f, mouthY + 2.5f), Size(8f, 4.5f))
        }
        smile >= -0.2f -> {
            // The cat-like "ω" mouth; deeper when happier.
            val d = 3f + 2.5f * smile.coerceAtLeast(0f)
            drawPath(Path().apply { moveTo(53f, mouthY - 1f); quadraticBezierTo(56.5f, mouthY + d, 60f, mouthY - 1f); quadraticBezierTo(63.5f, mouthY + d, 67f, mouthY - 1f) }, Outline, style = mouth)
        }
        else -> drawPath(Path().apply { moveTo(54f, mouthY + 3f); quadraticBezierTo(60f, mouthY + 3f + 9f * smile, 66f, mouthY + 3f) }, Outline, style = mouth)
    }

    if (headphones) {
        val band = lerp(Palette.AccentDeep, body, 0.1f)
        drawPath(Path().apply { moveTo(17f, headY + 2f); quadraticBezierTo(60f, headY - 62f, 103f, headY + 2f) }, line, style = Stroke(9f, cap = StrokeCap.Round))
        drawPath(Path().apply { moveTo(17f, headY + 2f); quadraticBezierTo(60f, headY - 62f, 103f, headY + 2f) }, band, style = Stroke(5f, cap = StrokeCap.Round))
        shape(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(Offset(8f, headY - 8f), Size(16f, 24f)), androidx.compose.ui.geometry.CornerRadius(8f))) }, band)
        shape(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(Offset(96f, headY - 8f), Size(16f, 24f)), androidx.compose.ui.geometry.CornerRadius(8f))) }, band)
    }
}
