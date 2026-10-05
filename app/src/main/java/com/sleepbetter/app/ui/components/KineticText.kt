package com.sleepbetter.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A headline whose words rise into place one after another, a little rotated
 * and settling with a spring. Replays whenever [text] changes. Read out as one
 * heading by screen readers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KineticHeadline(text: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = Color.White) {
    val still = rememberReduceMotion()
    key(text) {
        FlowRow(
            modifier.clearAndSetSemantics { contentDescription = text; heading() },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            text.split(" ").forEachIndexed { i, word ->
                val progress = remember { Animatable(if (still) 1f else 0f) }
                LaunchedEffect(Unit) {
                    delay(90L * i)
                    progress.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 180f))
                }
                Text(
                    word,
                    style = style,
                    color = color,
                    modifier = Modifier.graphicsLayer {
                        val p = progress.value
                        alpha = p.coerceIn(0f, 1f)
                        translationY = (1f - p) * 40f
                        rotationZ = (1f - p) * 8f
                    },
                )
            }
        }
    }
}

/** Large numerals that drop in from above with a bounce, one character at a time. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DroppingNumerals(text: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = Color.White, smallStyle: TextStyle = style) {
    val still = rememberReduceMotion()
    FlowRow(modifier.clearAndSetSemantics { contentDescription = text }) {
        text.forEachIndexed { i, ch ->
            val progress = remember(text) { Animatable(if (still) 1f else 0f) }
            LaunchedEffect(text) {
                delay(110L * i)
                progress.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 220f))
            }
            Text(
                ch.toString(),
                style = if (ch.isDigit()) style else smallStyle,
                color = color,
                modifier = Modifier.graphicsLayer {
                    val p = progress.value
                    alpha = p.coerceIn(0f, 1f)
                    translationY = (1f - p) * -160f
                    rotationZ = (1f - p) * -14f
                },
            )
        }
    }
}
