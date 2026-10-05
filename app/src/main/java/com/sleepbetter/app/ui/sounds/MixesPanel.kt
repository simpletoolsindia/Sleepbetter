package com.sleepbetter.app.ui.sounds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepbetter.app.AppViewModel
import com.sleepbetter.app.data.SavedMix
import com.sleepbetter.app.share.MixSharing
import com.sleepbetter.app.ui.components.BentoCard
import com.sleepbetter.app.ui.components.CircleButton
import com.sleepbetter.app.ui.components.Glyph
import com.sleepbetter.app.ui.components.GlyphIcon
import com.sleepbetter.app.ui.components.PrimaryButton
import com.sleepbetter.app.ui.components.glyph
import com.sleepbetter.app.ui.components.pressable
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type
import com.sleepbetter.app.ui.theme.deep
import com.sleepbetter.app.ui.theme.tint
import com.sleepbetter.core.mix.Mix
import com.sleepbetter.core.mix.MixCategory
import com.sleepbetter.core.mix.MixCodec
import com.sleepbetter.app.ui.components.LocalBurst
import com.sleepbetter.app.ui.components.floaty
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.sp
import com.sleepbetter.core.mix.MixTemplates

/**
 * Mixes: your saved and received mixes (play, share, delete), then ready-made
 * templates by category. Save captures the sounds and places playing now.
 */
@Composable
fun MixesPanel(vm: AppViewModel) {
    val saved by vm.savedMixes.collectAsStateWithLifecycle()
    val state by vm.mix.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf<MixCategory?>(null) }
    var saving by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf<Mix?>(null) }
    val burst = LocalBurst.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Save mix", { saving = true }, Modifier.weight(1f), glyph = Glyph.PLUS)
            PrimaryButton("🎁 Get a mix", { importing = true }, Modifier.weight(1f), color = Palette.Card, textColor = Palette.Ink)
        }

        Text("Your mixes 💜", style = Type.Title, color = Palette.Ink)
        if (saved.isEmpty()) {
            BentoCard(Modifier.fillMaxWidth(), color = Palette.Accent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Make it yours", style = Type.Heading, color = Palette.Ink, modifier = Modifier.weight(1f))
                    Text("🎛️", fontSize = 26.sp, modifier = Modifier.floaty())
                }
                Text(
                    "Turn sounds on, place them where you like, then tap Save mix. Send any saved mix to a friend 💌",
                    style = Type.Body,
                    color = Palette.InkSoft,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        } else {
            saved.forEach { m ->
                SavedMixRow(
                    m,
                    playing = state.playing && state.active == m.mix.sounds,
                    onPlay = { vm.playMix(m.mix) },
                    onShare = {
                        sharing = m.mix
                        burst.fire(listOf("💌", "✈️", "✨"), Offset(0.8f, 0.45f), count = 10)
                    },
                    onDelete = { vm.deleteMix(m.id) },
                )
            }
        }

        Text("Templates 🎨", style = Type.Title, color = Palette.Ink, modifier = Modifier.padding(top = 4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { CategoryChip("✨ All", category == null) { category = null } }
            items(MixCategory.entries.toList()) { c -> CategoryChip("${c.emoji} ${c.label}", category == c) { category = c } }
        }
        MixTemplates.all.filter { category == null || it.category == category }.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { t ->
                    TemplateCard(t.mix, t.blurb, t.emoji, playing = state.playing && state.active == t.mix.sounds, Modifier.weight(1f)) {
                        vm.playMix(t.mix)
                        burst.fire(listOf(t.emoji, "✨", "🎶"), Offset(0.5f, 0.6f), count = 12)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    if (saving) {
        SaveMixDialog(
            enabled = state.active.isNotEmpty(),
            onDismiss = { saving = false },
            onSave = { name ->
                vm.saveCurrentMix(name)
                saving = false
                burst.fire(listOf("🎉", "✨", "💜", "🌙", "⭐"), Offset(0.5f, 0.35f), count = 22)
            },
        )
    }
    if (importing) {
        ImportDialog(onDismiss = { importing = false }, onImport = { text ->
            vm.offerImport(text).also {
                if (it) {
                    importing = false
                    burst.fire(listOf("🎁", "💌", "✨"), Offset(0.5f, 0.4f))
                }
            }
        })
    }
    sharing?.let { mix -> ShareSheet(mix) { sharing = null } }
}

@Composable
private fun CategoryChip(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = Type.Label,
        color = if (on) Palette.OnInk else Palette.Ink,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (on) Palette.Ink else Palette.Card)
            .pressable(role = Role.Tab, onClick = onClick)
            .semantics { selected = on }
            .padding(horizontal = 16.dp, vertical = 11.dp),
    )
}

@Composable
private fun SoundStack(mix: Mix, size: Int = 30) {
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        mix.layers.take(4).forEach { l ->
            Box(Modifier.size(size.dp).clip(CircleShape).background(Palette.Card).border(2.dp, l.sound.tint(), CircleShape), contentAlignment = Alignment.Center) {
                GlyphIcon(l.sound.glyph(), l.sound.deep(), size = (size * 0.55f).dp)
            }
        }
    }
}

@Composable
private fun SavedMixRow(saved: SavedMix, playing: Boolean, onPlay: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    val tints = saved.mix.layers.map { it.sound.tint() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Palette.Card)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(if (tints.size > 1) tints else tints + Palette.Accent))
                .pressable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) { GlyphIcon(if (playing) Glyph.SOUNDS else Glyph.PLAY, Palette.Ink, size = 22.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(saved.mix.name, style = Type.Heading, color = Palette.Ink, maxLines = 1)
            Text(saved.mix.layers.joinToString(", ") { it.sound.label }, style = Type.Small, color = Palette.InkMuted, maxLines = 1)
        }
        CircleButton(Glyph.SHARE, "Share ${saved.mix.name}", onShare, bg = Palette.Accent, size = 44.dp)
        Spacer(Modifier.width(6.dp))
        CircleButton(Glyph.TRASH, "Delete ${saved.mix.name}", { confirmDelete = true }, bg = Palette.Line, tint = Palette.InkSoft, size = 44.dp)
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${saved.mix.name}\"?", style = Type.Title) },
            text = { Text("The sounds stay in the app; only this saved mix goes.", style = Type.Body) },
            confirmButton = { TextButton({ confirmDelete = false; onDelete() }) { Text("Delete", style = Type.Label, color = Palette.PeachDeep) } },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Keep it", style = Type.Label, color = Palette.Ink) } },
            containerColor = Palette.Card,
        )
    }
}

@Composable
private fun TemplateCard(mix: Mix, blurb: String, emoji: String, playing: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val tints = mix.layers.map { it.sound.tint() }
    Column(
        modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Palette.Card)
            .pressable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(if (tints.size > 1) tints else tints + Palette.Accent))
                .padding(10.dp),
        ) {
            SoundStack(mix)
            Text(emoji, fontSize = 28.sp, modifier = Modifier.align(Alignment.BottomStart).floaty(amplitude = 3f, phase = mix.name.length / 9f))
            Box(
                Modifier.align(Alignment.BottomEnd).size(34.dp).clip(CircleShape).background(if (playing) Palette.Ink else Palette.Card),
                contentAlignment = Alignment.Center,
            ) { GlyphIcon(if (playing) Glyph.SOUNDS else Glyph.PLAY, if (playing) Palette.OnInk else Palette.Ink, size = 16.dp) }
        }
        Text(mix.name, style = Type.Heading, color = Palette.Ink, maxLines = 2, modifier = Modifier.padding(start = 4.dp, top = 10.dp))
        Text(blurb, style = Type.Small, color = Palette.InkMuted, maxLines = 2, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
    }
}

@Composable
private fun SaveMixDialog(enabled: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (enabled) "Save this mix" else "Nothing playing yet", style = Type.Title) },
        text = {
            if (enabled) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(Mix.MAX_NAME) },
                    label = { Text("Name", style = Type.Small) },
                    placeholder = { Text("Sunday storm", style = Type.Body) },
                    singleLine = true,
                    textStyle = Type.Body,
                )
            } else {
                Text("Turn on at least one sound in Pick sounds, then save.", style = Type.Body)
            }
        },
        confirmButton = {
            if (enabled) TextButton({ onSave(name.ifBlank { "My mix" }) }) { Text("Save", style = Type.Label, color = Palette.AccentDeep) }
        },
        dismissButton = { TextButton(onDismiss) { Text(if (enabled) "Cancel" else "OK", style = Type.Label, color = Palette.Ink) } },
        containerColor = Palette.Card,
    )
}

@Suppress("DEPRECATION") // LocalClipboardManager: simple and synchronous; fine for reading a short code.
@Composable
private fun ImportDialog(onDismiss: () -> Unit, onImport: (String) -> Boolean) {
    var text by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a friend's mix", style = Type.Title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Mixes sent by Quick Share or Bluetooth open in SleepBetter by themselves. You can also paste a mix link here.",
                    style = Type.Body,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; failed = false },
                    label = { Text("Mix link or code", style = Type.Small) },
                    textStyle = Type.Body,
                    isError = failed,
                    supportingText = { if (failed) Text("That doesn't look like a SleepBetter mix.", style = Type.Small) },
                )
                TextButton({ clipboard.getText()?.text?.let { text = it; failed = false } }) {
                    Text("Paste from clipboard", style = Type.Label, color = Palette.AccentDeep)
                }
            }
        },
        confirmButton = { TextButton({ failed = !onImport(text) }) { Text("Add", style = Type.Label, color = Palette.AccentDeep) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", style = Type.Label, color = Palette.Ink) } },
        containerColor = Palette.Card,
    )
}

/** Share a mix: Quick Share (Wi-Fi), Bluetooth, any app, or copy the link. */
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareSheet(mix: Mix, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Palette.Paper) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SoundStack(mix, size = 40)
            Text("Share \"${mix.name}\"", style = Type.Title, color = Palette.Ink)
            Text("Your friend opens it with SleepBetter and the mix is added for them.", style = Type.Body, color = Palette.InkSoft)
            ShareOption(Glyph.WIFI, "Quick Share 📶", "Wi-Fi, to a phone nearby", Palette.Sky) {
                MixSharing.share(context, mix, MixSharing.Route.QUICK_SHARE); onDismiss()
            }
            ShareOption(Glyph.BLUETOOTH, "Bluetooth 🔵", "Send the mix file over Bluetooth", Palette.Accent) {
                MixSharing.share(context, mix, MixSharing.Route.BLUETOOTH); onDismiss()
            }
            ShareOption(Glyph.SHARE, "Other apps", "Messages, email and more", Palette.Rose) {
                MixSharing.share(context, mix, MixSharing.Route.ANY_APP); onDismiss()
            }
            ShareOption(Glyph.LINK, if (copied) "Link copied" else "Copy link", "Paste it anywhere", Palette.Butter) {
                clipboard.setText(AnnotatedString(MixCodec.link(mix)))
                copied = true
            }
        }
    }
}

@Composable
private fun ShareOption(glyph: Glyph, title: String, subtitle: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.Card)
            .pressable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) { GlyphIcon(glyph, Palette.Ink, size = 22.dp) }
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, style = Type.Heading, color = Palette.Ink)
            Text(subtitle, style = Type.Small, color = Palette.InkMuted)
        }
    }
}

/** Shown when a mix arrives from someone else: preview it, then add it (and optionally play). */
@Composable
fun IncomingMixDialog(mix: Mix, onAdd: (play: Boolean) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("A mix for you 🎁", style = Type.Title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SoundStack(mix, size = 40)
                Text(mix.name, style = Type.Heading, color = Palette.Ink)
                Text(mix.layers.joinToString(", ") { it.sound.label }, style = Type.Body, color = Palette.InkSoft)
            }
        },
        confirmButton = { TextButton({ onAdd(true) }) { Text("Add and play", style = Type.Label, color = Palette.AccentDeep) } },
        dismissButton = {
            Row {
                TextButton(onDismiss) { Text("Not now", style = Type.Label, color = Palette.InkSoft) }
                TextButton({ onAdd(false) }) { Text("Add", style = Type.Label, color = Palette.Ink) }
            }
        },
        containerColor = Palette.Card,
    )
}
