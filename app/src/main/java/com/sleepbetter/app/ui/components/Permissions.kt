package com.sleepbetter.app.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.sleepbetter.app.notify.Notifier

/** Returns an action that asks for the notification permission (Android 13+) if it isn't granted yet. */
@Composable
fun rememberAskNotifications(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return { if (Build.VERSION.SDK_INT >= 33 && !Notifier.allowed(context)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}
