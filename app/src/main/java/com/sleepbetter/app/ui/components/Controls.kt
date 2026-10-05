package com.sleepbetter.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type

private val bouncy = spring<Dp>(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)

/** A sound chip: a coloured dot and a label. On = filled white, corners tighten with a spring. */
@Composable
fun SoundChip(label: String, color: Color, on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val corner by animateDpAsState(if (on) 12.dp else 22.dp, bouncy, label = "corner")
    val bg by animateColorAsState(if (on) Color.White else Color(0x0FFFFFFF), label = "bg")
    val dotScale by animateFloatAsState(if (on) 1.3f else 1f, spring(0.4f), label = "dot")
    val shape = RoundedCornerShape(corner)
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, if (on) Color.White else Palette.GlassEdge, shape)
            .clickable(role = Role.Switch, onClick = onClick)
            .semantics { stateDescription = if (on) "On" else "Off" }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(14.dp).scale(dotScale).background(color, CircleShape))
        Text(label, style = if (on) Type.Label else Type.Body, color = if (on) Color(0xFF0A0B1C) else Palette.Ink)
    }
}

/**
 * Material 3 Expressive style button group: the chosen option stretches wider
 * and rounds fully, the others stay compact.
 */
@Composable
fun <T> ExpressiveButtonGroup(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { i, option ->
            val isOn = option == selected
            val weight by animateFloatAsState(if (isOn) 2f else 1f, spring(0.55f, Spring.StiffnessLow), label = "w")
            val outer = if (isOn) 24.dp else 8.dp
            val left by animateDpAsState(if (i == 0 || isOn) 24.dp else outer, bouncy, label = "l")
            val right by animateDpAsState(if (i == options.lastIndex || isOn) 24.dp else outer, bouncy, label = "r")
            val bg by animateColorAsState(if (isOn) Color.White else Color(0x14FFFFFF), label = "bg")
            val shape = RoundedCornerShape(topStart = left, bottomStart = left, topEnd = right, bottomEnd = right)
            Box(
                Modifier
                    .weight(weight)
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .background(bg, shape)
                    .clickable(role = Role.RadioButton) { onSelect(option) }
                    .semantics { this.selected = isOn },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), style = Type.Label, color = if (isOn) Color(0xFF0A0B1C) else Palette.Ink)
            }
        }
    }
}

/** Play button that morphs from circle to rounded square, and turns a quarter, while playing. */
@Composable
fun MorphPlayButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, diameter: Dp = 68.dp, color: Color = Color.White) {
    val corner by animateDpAsState(if (playing) 22.dp else diameter / 2, bouncy, label = "corner")
    val turn by animateFloatAsState(if (playing) 90f else 0f, spring(0.5f, Spring.StiffnessLow), label = "turn")
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .size(diameter)
            .rotate(turn)
            .clip(shape)
            .background(color, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (playing) "Pause" else "Play" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(24.dp).rotate(-turn)) {
            val ink = Color(0xFF0A0B1C)
            if (playing) {
                drawRoundRect(ink, Offset(size.width * 0.2f, size.height * 0.18f), Size(size.width * 0.18f, size.height * 0.64f), CornerRadius(3f))
                drawRoundRect(ink, Offset(size.width * 0.62f, size.height * 0.18f), Size(size.width * 0.18f, size.height * 0.64f), CornerRadius(3f))
            } else {
                drawPath(
                    Path().apply {
                        moveTo(size.width * 0.3f, size.height * 0.18f)
                        lineTo(size.width * 0.82f, size.height * 0.5f)
                        lineTo(size.width * 0.3f, size.height * 0.82f)
                        close()
                    },
                    ink,
                )
            }
        }
    }
}

/** A plain pill button used for main actions. */
@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = true) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .then(if (filled) Modifier.background(Color.White, shape) else Modifier.border(1.5.dp, Color(0xB3FFFFFF), shape))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.Label, color = if (filled) Color(0xFF0A0B1C) else Color.White)
    }
}
