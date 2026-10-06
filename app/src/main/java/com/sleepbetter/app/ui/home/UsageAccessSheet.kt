package com.sleepbetter.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepbetter.app.ui.components.PrimaryButton
import com.sleepbetter.app.ui.components.floaty
import com.sleepbetter.app.ui.components.pressable
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.app.ui.theme.Type

/** A short guide to Android's "Usage access" switch, shown before sending the user to Settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageAccessSheet(onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Palette.Paper) {
        UsageAccessGuide(onOpenSettings)
    }
}

@Composable
fun UsageAccessGuide(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    var help by remember { mutableStateOf(false) }
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔐", fontSize = 34.sp, modifier = Modifier.floaty(amplitude = 3f))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Set up auto tracking", style = Type.Title, color = Palette.Ink)
                Text("Takes about 20 seconds", style = Type.Small, color = Palette.InkMuted)
            }
        }
        Text(
            "Android keeps a record of when your screen turns on and off. SleepBetter needs your OK, called \"Usage access\", to read it.",
            style = Type.Body, color = Palette.InkSoft,
        )

        Step(1, "⚙️", "Tap Open settings below", "Android opens its Usage access screen.")
        Step(2, "🔎", "Find SleepBetter", "Tap it in the list of apps. If you land on SleepBetter's page already, skip this step.")
        Step(3, "🟢", "Switch on \"Permit usage access\"", "Some phones call it \"Allow usage tracking\".")
        Step(4, "↩️", "Come back here", "Press back. We pick up the change and look for your recent nights right away.")

        // Where the switch hides on popular phones.
        Text(
            if (help) "Hide the help ▲" else "Can't find it? ▼",
            style = Type.Label, color = Palette.AccentDeep,
            modifier = Modifier.pressable { help = !help }.padding(vertical = 6.dp),
        )
        AnimatedVisibility(help) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.Card).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Path("Pixel and most phones", "Settings › Apps › Special app access › Usage access")
                Path("Samsung", "Settings › Apps › ⋮ › Special access › Usage data access")
                Path("Xiaomi, Redmi, POCO", "Settings › Passwords & security › Privacy › Special app access › Apps with usage access")
                Path("OnePlus, Oppo, Realme", "Settings › Apps › Special app access › Usage access")
                Path("Vivo", "Settings › Apps › Special access › Usage access")
                Text("Or search Settings for \"usage access\".", style = Type.Small, color = Palette.InkSoft)
            }
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.Sky).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🛡️", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                "Only screen on/off times are used, never what you do in other apps, and nothing leaves your phone. You can switch it off anytime.",
                style = Type.Small, color = Palette.Ink,
            )
        }
        PrimaryButton("Open settings", onOpenSettings, Modifier.fillMaxWidth().padding(top = 4.dp))
    }
}

@Composable
private fun Step(n: Int, emoji: String, title: String, detail: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Palette.Accent), contentAlignment = Alignment.Center) {
            Text("$n", style = Type.Label, color = Palette.Ink)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("$emoji  $title", style = Type.Heading, color = Palette.Ink)
            Text(detail, style = Type.Small, color = Palette.InkSoft)
        }
    }
}

@Composable
private fun Path(phone: String, path: String) {
    Column {
        Text(phone, style = Type.Label, color = Palette.Ink)
        Text(path, style = Type.Small, color = Palette.InkSoft)
    }
}
