package com.sleepbetter.app.sleep

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.sleepbetter.core.sleep.PhoneUse
import java.time.Instant

/**
 * When the phone's screen was on, from Android's own usage history (the
 * "Usage access" permission). Nothing leaves the phone: the times are only
 * used to work out when the user slept. Android keeps about a week of this
 * history, which is how far back nights can be found.
 */
object PhoneUsage {
    fun hasAccess(context: Context): Boolean = runCatching {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    /** Opens the system screen where the user allows usage access for SleepBetter. */
    fun openSettings(context: Context) {
        val list = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Android 10+ can open SleepBetter's own switch directly; older phones and some brands only show the list.
        val direct = Intent(list).setData(Uri.parse("package:${context.packageName}"))
        if (Build.VERSION.SDK_INT >= 29 && runCatching { context.startActivity(direct) }.isSuccess) return
        runCatching { context.startActivity(list) }
            .recoverCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    /**
     * Android 13+ blocks Usage access for apps installed from a file (not a
     * store) until the user allows "restricted settings" on the app's info
     * page. True when that block is likely: Android 13+ and no store installer.
     */
    fun likelyRestricted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        val installer = runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        }.getOrNull()
        return installer == null || installer !in STORES
    }

    private val STORES = setOf("com.android.vending", "com.sec.android.app.samsungapps", "com.huawei.appmarket", "com.xiaomi.market", "com.amazon.venezia")

    /** SleepBetter's App info page, where ⋮ › "Allow restricted settings" lives. */
    fun openAppInfo(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /** Screen-on stretches between [from] and [to]; empty without access. */
    fun uses(context: Context, from: Instant, to: Instant): List<PhoneUse> = runCatching {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val events = usm.queryEvents(from.toEpochMilli(), to.toEpochMilli())
        val e = UsageEvents.Event()
        val out = ArrayList<PhoneUse>()
        // Android 9+ reports the screen turning on and off. Older versions only
        // report apps coming to the front, which is close enough.
        val screenEvents = Build.VERSION.SDK_INT >= 28
        var onSince: Long? = null
        var openApps = 0
        var seenScreen = false
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val t = e.timeStamp
            if (screenEvents) {
                when (e.eventType) {
                    SCREEN_INTERACTIVE -> {
                        if (onSince == null) onSince = t
                        seenScreen = true
                    }
                    SCREEN_NON_INTERACTIVE -> {
                        // Turning off before any turn-on: the screen was already on at [from].
                        val start = onSince ?: if (!seenScreen) from.toEpochMilli() else null
                        start?.let { out += PhoneUse(Instant.ofEpochMilli(it), Instant.ofEpochMilli(t)) }
                        onSince = null
                        seenScreen = true
                    }
                }
            } else {
                when (e.eventType) {
                    MOVE_TO_FOREGROUND -> {
                        if (openApps++ == 0) onSince = t
                    }
                    MOVE_TO_BACKGROUND -> {
                        openApps = (openApps - 1).coerceAtLeast(0)
                        if (openApps == 0) {
                            onSince?.let { out += PhoneUse(Instant.ofEpochMilli(it), Instant.ofEpochMilli(t)) }
                            onSince = null
                        }
                    }
                }
            }
        }
        onSince?.let { out += PhoneUse(Instant.ofEpochMilli(it), to) }
        out
    }.getOrDefault(emptyList())

    // UsageEvents.Event.MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND (deprecated names, same values).
    private const val MOVE_TO_FOREGROUND = 1
    private const val MOVE_TO_BACKGROUND = 2

    // UsageEvents.Event.SCREEN_INTERACTIVE / SCREEN_NON_INTERACTIVE (API 28).
    private const val SCREEN_INTERACTIVE = 15
    private const val SCREEN_NON_INTERACTIVE = 16
}
