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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.graphics.drawscope.rotate
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
 * Pico: a mint baby dino with soft yellow back plates, who sleeps when you
 * sleep, and Lulu, a pink baby dino still wearing a bit of her eggshell.
 * Every friend shares the same shape (big head, small body,
 * thick soft outline) with its own colour and features, so the cast reads as
 * one family. Original designs, drawn in code.
 */
enum class Species(val body: Color, val shade: Color, val description: String) {
    PICO(Color(0xFFA3E4C8), Color(0xFF7DCBAA), "Pico the baby dino"),
    LULU(Color(0xFFFFC6D8), Color(0xFFF2A4BE), "Lulu the baby dino"),
    MOCHI(Color(0xFFFFFFFF), Color(0xFFE4DDF7), "Mochi the panda"),
    TOFFEE(Color(0xFFC99A6E), Color(0xFFAA7C54), "Toffee the capybara"),
    ELEPHANT(Color(0xFFC3CEEA), Color(0xFFA3B2DA), "Pebble the elephant"),
    FOX(Color(0xFFFFB98E), Color(0xFFF29A68), "Ember the fox"),
    OWL(Color(0xFFC9BCF7), Color(0xFFA996F0), "Hoot the owl"),
    DINO(Color(0xFFBFD8A9), Color(0xFF9DBF8A), "Dozy the dinosaur"),
    KOALA(Color(0xFFD9D6E2), Color(0xFFBDB8CC), "Koko the koala"),
    CAT(Color(0xFFFFE08A), Color(0xFFF2C95C), "Purr the cat"),
}

fun Visitor.species(): Species = when (this) {
    Visitor.PIP -> Species.PICO
    Visitor.LULU -> Species.LULU
    Visitor.PANDA -> Species.MOCHI
    Visitor.TOFFEE -> Species.TOFFEE
    Visitor.PEBBLE -> Species.ELEPHANT
    Visitor.EMBER -> Species.FOX
    Visitor.HOOT -> Species.OWL
    Visitor.DOZY -> Species.DINO
    Visitor.KOALA -> Species.KOALA
    Visitor.CAT -> Species.CAT
}

/** How Pico feels. 0 = awful … 4 = great; values in between morph smoothly. */
enum class Mood(val label: String, val tint: Color, val emoji: String, val confetti: List<String>) {
    AWFUL("Awful", Color(0xFFD9D6E2), "😫", listOf("🫂", "💜", "🌧️")),
    POOR("Poor", Color(0xFFFFC2A6), "😕", listOf("🫂", "🍵", "💜")),
    OKAY("Okay", Color(0xFFFFE08A), "😐", listOf("🌤️", "👍", "✨")),
    GOOD("Good", Color(0xFFBFD8A9), "🙂", listOf("😊", "🌿", "✨", "💚")),
    GREAT("Great", Color(0xFFC9BCF7), "🤩", listOf("🎉", "🌟", "✨", "💜", "😴")),
}

/**
 * A living character. [mood] is 0..4 and morphs the face continuously, so a
 * slider can drive it. [beat] (0..1) makes it nod to music.
 */
@Composable
fun MochiView(
    modifier: Modifier = Modifier,
    species: Species = Species.PICO,
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

/**
 * Pico and Lulu together: Lulu a little behind on the left, Pico in front.
 * Awake, a small heart floats up between them.
 */
@Composable
fun BestFriends(modifier: Modifier = Modifier, sleeping: Boolean = false, mood: Float = 3f) {
    val still = rememberReduceMotion()
    val rise by rememberInfiniteTransition(label = "heart").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing)), label = "rise",
    )
    Box(modifier.aspectRatio(1.7f).semantics(mergeDescendants = true) { contentDescription = "Pico and Lulu" }) {
        MochiView(Modifier.fillMaxWidth(0.58f).align(Alignment.BottomStart), species = Species.LULU, sleeping = sleeping, mood = mood)
        MochiView(Modifier.fillMaxWidth(0.58f).align(Alignment.BottomEnd), species = Species.PICO, sleeping = sleeping, mood = mood)
        if (!sleeping) {
            Canvas(Modifier.fillMaxWidth(0.14f).aspectRatio(1f).align(Alignment.TopCenter)) {
                val p = if (still) 0.3f else rise
                val k = size.width / 24f
                withTransform({
                    translate(top = size.height * 0.6f * (1f - p))
                    scale(k * (0.7f + 0.3f * p), k * (0.7f + 0.3f * p), pivot = Offset.Zero)
                }) {
                    val heart = Path().apply {
                        moveTo(12f, 21f); cubicTo(3f, 15f, 1f, 10f, 4f, 6f); cubicTo(7f, 2f, 11f, 4f, 12f, 7f)
                        cubicTo(13f, 4f, 17f, 2f, 20f, 6f); cubicTo(23f, 10f, 21f, 15f, 12f, 21f); close()
                    }
                    val alpha = if (still) 1f else (1f - p) * 1.4f
                    drawPath(heart, Color(0xFFFF8FA3).copy(alpha = alpha.coerceIn(0f, 1f)))
                    drawPath(heart, Outline.copy(alpha = alpha.coerceIn(0f, 1f)), style = Stroke(2f, join = StrokeJoin.Round))
                }
            }
        }
    }
}

private val Ink = Color(0xFF2B2238)

/** Sticker-style outline: a warm dark brown reads softer than black. */
private val Outline = Color(0xFF3A2622)
private val SilhouetteOutline = Color(0xFFCFC6D8)
private val Patch = Color(0xFF3B2F38)

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

    // Mochi is a panda: dark ears, eye patches, arms and feet.
    val panda = species == Species.MOCHI && face
    val limb = if (panda) Patch else body

    // Ground shadow.
    drawOval(Color(0x1A2B2238), Offset(24f, 91f), Size(72f, 8f))

    val dino = species == Species.PICO || species == Species.LULU
    if (withBody) {
        if (dino) {
            // A stubby tail curling out behind, with a tip that wags.
            val wag = 3f * sin(wobble * 2f)
            shape(Path().apply { moveTo(80f, 84f); quadraticBezierTo(100f, 88f, 106f + wag, 70f); quadraticBezierTo(98f, 80f, 81f, 72f); close() }, body)
        }
        // Feet first, so the body sits on them and only their soles show.
        oval(Offset(39f, 86f), Size(16f, 10f), if (panda) Patch else shade)
        oval(Offset(65f, 86f), Size(16f, 10f), if (panda) Patch else shade)
        shape(
            Path().apply {
                moveTo(37f, 62f); lineTo(36f, 84f); quadraticBezierTo(36f, 93f, 46f, 93f)
                lineTo(74f, 93f); quadraticBezierTo(84f, 93f, 84f, 84f); lineTo(83f, 62f); close()
            },
            body,
        )
        if (species == Species.OWL || dino) drawOval(Color(0x66FFFFFF), Offset(46f, 70f), Size(28f, 18f))
        // Stubby arms resting in front.
        oval(Offset(31f, 70f), Size(14f, 11f), limb)
        oval(Offset(75f, 70f), Size(14f, 11f), limb)
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
                shape(Path().apply { moveTo(x - 7f, 22f); quadraticBezierTo(x, -4f, x + 7f, 22f); close() }, shade)
            }
            Species.KOALA -> {
                oval(Offset(6f, 14f), Size(32f, 32f), body); drawCircle(Color(0xFFF6C3D6), 8f, Offset(22f, 30f))
                oval(Offset(82f, 14f), Size(32f, 32f), body); drawCircle(Color(0xFFF6C3D6), 8f, Offset(98f, 30f))
            }
            Species.CAT -> {
                shape(Path().apply { moveTo(28f, 34f); lineTo(32f, 10f); lineTo(50f, 26f); close() }, body)
                shape(Path().apply { moveTo(92f, 34f); lineTo(88f, 10f); lineTo(70f, 26f); close() }, body)
            }
            Species.TOFFEE -> {
                // Tiny capybara ears, set far back on the sides.
                oval(Offset(19f, 22f), Size(11f, 9f), shade)
                oval(Offset(90f, 22f), Size(11f, 9f), shade)
            }
            Species.PICO, Species.LULU -> {
                // Soft, rounded back plates peeking over the head.
                val plate = if (species == Species.PICO) Color(0xFFFFD98A) else Color(0xFFCDB9F7)
                for (i in 0..2) {
                    val x = 46f + i * 14f
                    val tall = if (i == 1) 20f else 15f
                    shape(Path().apply { moveTo(x - 7f, 24f); quadraticBezierTo(x - 6f, 24f - tall, x, 24f - tall); quadraticBezierTo(x + 6f, 24f - tall, x + 7f, 24f); close() }, if (face) plate else body)
                }
            }
            Species.ELEPHANT -> {
                // Big floppy ears that flap slowly.
                val flap = 1f + 0.06f * sin(wobble * 2f)
                scale(flap, 1f, pivot = Offset(36f, 40f)) {
                    oval(Offset(2f, 20f), Size(36f, 40f), body)
                    if (face) drawOval(Color(0xFFF6C3D6), Offset(9f, 28f), Size(20f, 24f))
                }
                scale(flap, 1f, pivot = Offset(84f, 40f)) {
                    oval(Offset(82f, 20f), Size(36f, 40f), body)
                    if (face) drawOval(Color(0xFFF6C3D6), Offset(91f, 28f), Size(20f, 24f))
                }
            }
            Species.MOCHI -> {
                // Round panda ears.
                oval(Offset(21f, 10f), Size(23f, 21f), limb)
                oval(Offset(76f, 10f), Size(23f, 21f), limb)
            }
        }
    }

    // Head: shaded underside, the body colour on top, then one clean outline.
    val headY = if (withBody) 44f else 56f
    // A capybara's head is wider and flatter than everyone else's.
    val headRx = if (species == Species.TOFFEE) 46f else 43f
    val headRy = if (species == Species.TOFFEE) 30f else 34f
    val head = blobPath(wobble, cy = headY, rx = headRx, ry = headRy)
    drawPath(head, shade)
    withTransform({ translate(top = -2.5f); scale(0.96f, 0.93f, pivot = Offset(60f, headY - 16f)) }) {
        drawPath(blobPath(wobble, cy = headY, rx = headRx, ry = headRy), body)
    }
    drawPath(head, line, style = stroke)

    if (species == Species.TOFFEE) {
        if (face) {
            // Toffee's own mark: a two-leaf sprout.
            drawLine(line, Offset(60f, headY - 29f), Offset(60f, headY - 38f), 2.2f, StrokeCap.Round)
            shape(Path().apply { moveTo(60f, headY - 37f); quadraticBezierTo(50f, headY - 46f, 47f, headY - 37f); quadraticBezierTo(53f, headY - 32f, 60f, headY - 37f); close() }, Color(0xFF8CC56B))
            shape(Path().apply { moveTo(60f, headY - 37f); quadraticBezierTo(70f, headY - 48f, 74f, headY - 39f); quadraticBezierTo(67f, headY - 32f, 60f, headY - 37f); close() }, Color(0xFFA8D98A))
        }
        // The big, blunt capybara snout: a wide rounded block with two nostrils.
        shape(
            Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(Offset(34f, headY + 1f), Size(52f, 26f)), androidx.compose.ui.geometry.CornerRadius(14f))) },
            if (face) Color(0xFFB98A61) else body,
        )
        if (face) {
            drawOval(Outline, Offset(51f, headY + 7f), Size(4.5f, 7f))
            drawOval(Outline, Offset(64.5f, headY + 7f), Size(4.5f, 7f))
        }
    }
    if (species == Species.LULU) {
        // Lulu's mark: a piece of her eggshell, still worn like a hat.
        val shell = Path().apply {
            moveTo(36f, headY - 21f)
            val tips = listOf(42f to -27f, 48f to -20f, 54f to -28f, 60f to -20f, 66f to -28f, 72f to -20f, 78f to -27f, 84f to -21f)
            tips.forEach { (x, dy) -> lineTo(x, headY + dy) }
            cubicTo(86f, headY - 46f, 34f, headY - 46f, 36f, headY - 21f)
            close()
        }
        shape(shell, if (face) Color(0xFFFFFBF2) else body)
        if (face) {
            drawCircle(Color(0xFFA3E4C8), 2f, Offset(50f, headY - 34f))
            drawCircle(Color(0xFFCDB9F7), 1.6f, Offset(66f, headY - 37f))
            drawCircle(Color(0xFFFFD98A), 1.8f, Offset(72f, headY - 30f))
        }
    }
    if (species == Species.ELEPHANT) {
        // Pebble's new thing: a striped nightcap with a pom-pom, tipped to one side.
        val cap = Path().apply { moveTo(40f, headY - 26f); quadraticBezierTo(62f, headY - 38f, 80f, headY - 30f); quadraticBezierTo(84f, headY - 46f, 94f, headY - 50f); quadraticBezierTo(70f, headY - 58f, 40f, headY - 26f); close() }
        shape(cap, if (face) Color(0xFF8DA6E6) else body)
        if (face) {
            drawLine(Color.White.copy(alpha = 0.7f), Offset(56f, headY - 37f), Offset(72f, headY - 34f), 3f, StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.7f), Offset(68f, headY - 45f), Offset(82f, headY - 42f), 3f, StrokeCap.Round)
        }
        oval(Offset(90f, headY - 56f), Size(11f, 11f), if (face) Color.White else body)
        // The trunk: a short curl that sways.
        val e = headY + 3f
        val sway = 2.5f * sin(wobble * 2f)
        shape(
            Path().apply {
                moveTo(54f, e + 4f); quadraticBezierTo(52f, e + 21f, 59f, e + 24f)
                quadraticBezierTo(66f + sway, e + 27f, 71f + sway, e + 20f)
                quadraticBezierTo(73f + sway, e + 15f, 68f + sway, e + 16f)
                quadraticBezierTo(65f + sway, e + 20f, 61.5f, e + 17f)
                quadraticBezierTo(60f, e + 11f, 66f, e + 4f); close()
            },
            body,
        )
    }
    if (species == Species.CAT) {
        drawLine(line.copy(alpha = 0.6f), Offset(12f, headY + 10f), Offset(25f, headY + 11f), 1.8f, StrokeCap.Round)
        drawLine(line.copy(alpha = 0.6f), Offset(108f, headY + 10f), Offset(95f, headY + 11f), 1.8f, StrokeCap.Round)
    }
    if (species == Species.MOCHI && face) {
        // Mochi's own mark: a little crescent-moon clip between the ears.
        val clip = Path.combine(
            PathOperation.Difference,
            Path().apply { addOval(Rect(Offset(60f, headY - 27f), 7f)) },
            Path().apply { addOval(Rect(Offset(63.5f, headY - 30f), 6f)) },
        )
        drawPath(clip, Color(0xFFFFD86B))
        drawPath(clip, line, style = Stroke(2f, join = StrokeJoin.Round))
    }
    if (!face) return

    // Face. Mood 0..4 bends the mouth from a frown to an open smile and changes the eyes.
    val smile = (mood - 2f) / 2f // -1..1
    // Baby-face proportions: big eyes set low and wide, a tiny mouth.
    val eyeY = headY + when (species) {
        Species.TOFFEE -> -8f
        Species.ELEPHANT -> 1f
        else -> 5f
    }
    val lx = if (species == Species.TOFFEE) 37f else 42f
    val rx = if (species == Species.TOFFEE) 83f else 78f
    val mouthY = eyeY + if (species == Species.TOFFEE) 27f else 10f
    if (panda) {
        // Droopy teardrop eye patches, tilted outwards: sleepy and soft.
        rotate(22f, pivot = Offset(lx, eyeY)) { drawOval(Patch, Offset(lx - 9.5f, eyeY - 9f), Size(19f, 22f)) }
        rotate(-22f, pivot = Offset(rx, eyeY)) { drawOval(Patch, Offset(rx - 9.5f, eyeY - 9f), Size(19f, 22f)) }
    }
    // On a dark patch the eye lines turn light.
    val eyeLine = if (panda) Color(0xFFFFF6EC) else Outline
    if (sleeping) {
        val s = Stroke(3f, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(lx - 6f, eyeY); quadraticBezierTo(lx, eyeY + 5f, lx + 6f, eyeY) }, eyeLine, style = s)
        drawPath(Path().apply { moveTo(rx - 6f, eyeY); quadraticBezierTo(rx, eyeY + 5f, rx + 6f, eyeY) }, eyeLine, style = s)
    } else if (mood > 3.5f) {
        // Delighted: happy closed arcs.
        val s = Stroke(3.2f, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(lx - 6f, eyeY + 2f); quadraticBezierTo(lx, eyeY - 5f, lx + 6f, eyeY + 2f) }, eyeLine, style = s)
        drawPath(Path().apply { moveTo(rx - 6f, eyeY + 2f); quadraticBezierTo(rx, eyeY - 5f, rx + 6f, eyeY + 2f) }, eyeLine, style = s)
    } else {
        scale(1f, blink, pivot = Offset(60f, eyeY)) {
            for (x in listOf(lx, rx)) {
                // A light ring separates the pupil from a panda patch.
                if (panda) drawOval(Color(0xFFFFF6EC), Offset(x - 6.5f, eyeY - 7.5f), Size(13f, 15f))
                drawOval(Outline, Offset(x - 5.5f, eyeY - 6.5f), Size(11f, 13f))
                drawCircle(Color.White, 2.4f, Offset(x + 1.8f, eyeY - 2.6f))
                drawCircle(Color.White, 1.1f, Offset(x - 2f, eyeY + 2.8f))
            }
        }
        if (mood < 1.5f) {
            // Tired lids for a rough night.
            val droop = (1.5f - mood) * 4f
            // Inner ends raised: tired and a bit sad, never cross.
            drawLine(Outline, Offset(lx - 7f, eyeY - 7f + droop * 0.4f), Offset(lx + 6f, eyeY - 9f - droop), 2.6f, StrokeCap.Round)
            drawLine(Outline, Offset(rx - 6f, eyeY - 9f - droop), Offset(rx + 7f, eyeY - 7f + droop * 0.4f), 2.6f, StrokeCap.Round)
        }
    }
    val cheek = when (species) {
        Species.FOX, Species.CAT, Species.TOFFEE -> Color(0x99FF8A5C)
        else -> Color(0xB3FF9AA8)
    }
    drawCircle(cheek, 8f, Offset(26f, eyeY + 9f))
    drawCircle(cheek, 8f, Offset(94f, eyeY + 9f))

    val mouth = Stroke(2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when {
        species == Species.ELEPHANT -> Unit // the trunk is the face
        species == Species.TOFFEE && !sleeping && mood <= 3.5f ->
            drawPath(Path().apply { moveTo(55f, mouthY); quadraticBezierTo(60f, mouthY + 3f + 2f * smile.coerceAtLeast(-1f), 65f, mouthY) }, Outline, style = mouth)
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
            drawPath(Path().apply { moveTo(55f, mouthY - 1f); quadraticBezierTo(57.5f, mouthY + d, 60f, mouthY - 1f); quadraticBezierTo(62.5f, mouthY + d, 65f, mouthY - 1f) }, Outline, style = mouth)
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
