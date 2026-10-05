package com.sleepbetter.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.app.ui.theme.deep
import com.sleepbetter.app.ui.theme.tint
import com.sleepbetter.core.audio.SoundId
import kotlin.math.sin

/** A soft bento block. Colour carries meaning; see Palette. */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    color: Color = Palette.Card,
    onClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .clip(shape)
            .background(color, shape)
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Palette.Ink,
    textColor: Color = Color.White,
    glyph: Glyph? = null,
) {
    Row(
        modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(color)
            .pressable(onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (glyph != null) {
            GlyphIcon(glyph, textColor, size = 20.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = Type.Label.copy(fontSize = Type.Heading.fontSize), color = textColor)
    }
}

@Composable
fun CircleButton(glyph: Glyph, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, bg: Color = Palette.Card, tint: Color = Palette.Ink, size: Dp = 48.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .pressable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { GlyphIcon(glyph, tint, size = size * 0.46f) }
}

/** Pill tabs; the selected one is a white chip on a soft track. */
@Composable
fun <T> SegmentedTabs(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier, track: Color = Color(0xFFEDE8EF)) {
    Row(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(track)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val on = option == selected
            val bg by animateColorAsState(if (on) Palette.Card else Color.Transparent, label = "tab")
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .pressable(role = Role.Tab) { onSelect(option) }
                    .semantics { this.selected = on },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), style = Type.Label, color = if (on) Palette.Ink else Palette.InkMuted)
            }
        }
    }
}

/** Chip list where the chosen item grows (Material 3 Expressive button-group feel). */
@Composable
fun <T> ChoiceRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier, onColor: Color = Palette.Ink) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            val on = option == selected
            val weight by animateFloatAsState(if (on) 1.7f else 1f, spring(0.55f, Spring.StiffnessLow), label = "w")
            val corner by animateDpAsState(if (on) 22.dp else 14.dp, spring(0.5f, Spring.StiffnessMediumLow), label = "c")
            val bg by animateColorAsState(if (on) onColor else Palette.Card, label = "bg")
            Box(
                Modifier
                    .weight(weight)
                    .heightIn(min = 46.dp)
                    .clip(RoundedCornerShape(corner))
                    .background(bg)
                    .pressable(role = Role.RadioButton) { onSelect(option) }
                    .semantics { this.selected = on },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), style = Type.Label, color = if (on) Color.White else Palette.InkSoft, maxLines = 1)
            }
        }
    }
}

/** A progress ring with rounded ends; [content] sits in the middle. */
@Composable
fun Ring(progress: Float, color: Color, modifier: Modifier = Modifier, track: Color = Color(0x1F2B2238), stroke: Dp = 10.dp, content: @Composable BoxScope.() -> Unit = {}) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = w / 2f
            val arcSize = Size(size.width - w, size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
            drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

/**
 * A sound tile: a squircle that fills with the sound's colour when on and
 * shows live level bars from the engine.
 */
@Composable
fun SoundTile(id: SoundId, on: Boolean, level: Float, time: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (on) id.tint() else Palette.Card, label = "bg")
    val corner by animateDpAsState(if (on) 22.dp else 30.dp, spring(0.5f, Spring.StiffnessMediumLow), label = "c")
    val shape = RoundedCornerShape(corner)
    Column(
        modifier
            .clip(shape)
            .background(bg, shape)
            .pressable(role = Role.Switch, onClick = onClick)
            .semantics { stateDescription = if (on) "On" else "Off" }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (on) Color.White.copy(alpha = 0.7f) else id.tint()),
                contentAlignment = Alignment.Center,
            ) { GlyphIcon(id.glyph(), id.deep(), size = 22.dp) }
            Spacer(Modifier.weight(1f))
            if (on) LevelBars(level, time, id.deep())
        }
        Text(id.label, style = Type.Label, color = Palette.Ink, maxLines = 1)
    }
}

@Composable
private fun LevelBars(level: Float, time: Float, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(16.dp)) {
        repeat(3) { i ->
            val h = 0.3f + 0.7f * (0.5f + 0.5f * sin(time * (5f + i * 1.7f) + i)) * (0.35f + level.coerceIn(0f, 1f) * 0.65f)
            Box(
                Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .graphicsLayer { scaleY = h; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) }
                    .background(color, RoundedCornerShape(2.dp)),
            )
        }
    }
}

/** Play button that morphs circle → rounded square and turns a quarter while playing. */
@Composable
fun MorphPlayButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, diameter: Dp = 64.dp, color: Color = Palette.Ink, iconColor: Color = Color.White) {
    val corner by animateDpAsState(if (playing) 22.dp else diameter / 2, spring(0.5f, Spring.StiffnessMediumLow), label = "corner")
    val turn by animateFloatAsState(if (playing) 90f else 0f, spring(0.5f, Spring.StiffnessLow), label = "turn")
    Box(
        modifier
            .size(diameter)
            .graphicsLayer { rotationZ = turn }
            .clip(RoundedCornerShape(corner))
            .background(color)
            .pressable(onClick = onClick)
            .semantics { contentDescription = if (playing) "Pause" else "Play" },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.graphicsLayer { rotationZ = -turn }) {
            GlyphIcon(if (playing) Glyph.PAUSE else Glyph.PLAY, iconColor, size = diameter * 0.4f)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = Type.Title, color = Palette.Ink, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                style = Type.Label,
                color = Palette.LavenderDeep,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).pressable(onClick = onAction).padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
    }
}

/** Status dot with a symbol, so level never depends on colour alone. */
@Composable
fun LevelBadge(text: String, bg: Color, fg: Color, glyph: Glyph, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(bg).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GlyphIcon(glyph, fg, size = 16.dp, strokeWidth = 2.4f)
        Text(text, style = Type.Label, color = fg)
    }
}

@Composable
fun Dots(count: Int, filled: Int, color: Color, modifier: Modifier = Modifier, empty: Color = Color(0x1F2B2238)) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i -> Box(Modifier.size(10.dp).background(if (i < filled) color else empty, CircleShape)) }
    }
}

@Composable
fun Grabber(modifier: Modifier = Modifier, color: Color = Color(0x66FFFFFF)) {
    Box(modifier.width(40.dp).height(5.dp).background(color, RoundedCornerShape(3.dp)))
}
