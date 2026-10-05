package com.sleepbetter.app.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sleepbetter.app.ui.components.Glyph
import com.sleepbetter.app.ui.components.GlyphIcon
import com.sleepbetter.app.ui.components.pressable
import com.sleepbetter.app.ui.theme.AppTheme
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type

/** Pick a colour theme. Each card previews the theme's dusk sky, hills and accents; tapping recolours the app at once. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSheet(current: AppTheme, onPick: (AppTheme) -> Unit, onDismiss: () -> Unit) {
    val sheetColor by animateColorAsState(Palette.Paper, label = "sheet")
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = sheetColor) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Colour theme", style = Type.Title, color = Palette.Ink)
            Text("Calm colours for day and night. Your sounds and sleep data stay the same.", style = Type.Body, color = Palette.InkSoft)
            AppTheme.entries.forEach { t -> ThemeRow(t, t == current) { onPick(t) } }
        }
    }
}

@Composable
private fun ThemeRow(theme: AppTheme, on: Boolean, onClick: () -> Unit) {
    val c = theme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.Card)
            .border(2.dp, if (on) c.accentDeep else Color.Transparent, RoundedCornerShape(24.dp))
            .pressable(role = Role.RadioButton, onClick = onClick)
            .semantics { selected = on }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A tiny dusk scene: sky gradient, two hills and the moon.
        Box(
            Modifier
                .size(width = 84.dp, height = 64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(c.duskTop, c.duskMid, c.duskLow))),
        ) {
            Box(Modifier.padding(start = 52.dp, top = 9.dp).size(14.dp).clip(CircleShape).background(Palette.Moon))
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(22.dp).clip(RoundedCornerShape(topStart = 40.dp, topEnd = 12.dp)).background(c.hillMid))
            Box(Modifier.align(Alignment.BottomEnd).size(width = 60.dp, height = 13.dp).clip(RoundedCornerShape(topStart = 30.dp)).background(c.hillNear))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(theme.label, style = Type.Heading, color = c.ink)
            Text(theme.blurb, style = Type.Small, color = c.inkMuted)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(c.paper, c.accent, c.accentDeep, c.sky, c.rose).forEach { swatch ->
                    Box(Modifier.size(16.dp).clip(CircleShape).background(swatch).border(1.dp, c.line, CircleShape))
                }
            }
        }
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(if (on) c.accentDeep else c.line),
            contentAlignment = Alignment.Center,
        ) { if (on) GlyphIcon(Glyph.CHECK, Color.White, size = 16.dp) }
    }
}
