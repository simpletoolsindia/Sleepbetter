package com.sleepbetter.app.sleep

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleepbetter.app.SleepBetterApp
import com.sleepbetter.app.notify.Notifier
import com.sleepbetter.core.sleep.AutoSleepDetector
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Finds the last week's nights in the phone's screen history and stores them. */
object AutoSleepTracker {
    /** Blocking (reads usage history); call off the main thread. Does nothing unless tracking is on and allowed. */
    fun refresh(app: SleepBetterApp) {
        if (!app.repository.settings.value.autoTrack || !PhoneUsage.hasAccess(app)) return
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val mornings = (0L..6L).map { today.minusDays(it) }
        val uses = PhoneUsage.uses(app, AutoSleepDetector.windowFor(mornings.last(), zone).first, Instant.now())
        val nights = mornings.mapNotNull { AutoSleepDetector.detect(uses, it, zone) }
        app.repository.applyAutoNights(nights, uses)
    }
}

/**
 * Looks for last night a few times each morning (08:30, 10:30, 12:30, give or
 * take) and sends one "good morning" notification with it, unless the night
 * was already rated in the app. Inexact alarms: no special permission.
 */
object MorningCheck {
    private const val REQUEST = 71
    private val SLOTS = listOf(LocalTime.of(8, 30), LocalTime.of(10, 30), LocalTime.of(12, 30))

    fun schedule(context: Context) {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val next = (SLOTS.map { ZonedDateTime.of(now.toLocalDate(), it, zone) } +
            ZonedDateTime.of(now.toLocalDate().plusDays(1), SLOTS.first(), zone)).first { it.isAfter(now) }
        context.getSystemService(AlarmManager::class.java)
            .setWindow(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), 20 * 60 * 1000L, pendingIntent(context))
    }

    fun cancel(context: Context) = context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, REQUEST, Intent(context, MorningCheckReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class MorningCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as SleepBetterApp
        if (!app.repository.settings.value.autoTrack) return
        MorningCheck.schedule(context) // re-arm first, so one failure never stops the next check
        val pending = goAsync()
        Thread {
            try {
                AutoSleepTracker.refresh(app)
                val summary = app.repository.lastAuto.value
                val today = LocalDate.now()
                if (summary != null && summary.night.morning == today && app.repository.claimMorningNotice(today)) {
                    Notifier.morning(context, summary)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }
}
