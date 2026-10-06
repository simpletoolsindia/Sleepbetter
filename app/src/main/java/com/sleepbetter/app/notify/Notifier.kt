package com.sleepbetter.app.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.sleepbetter.app.MainActivity
import com.sleepbetter.app.R
import com.sleepbetter.app.SleepBetterApp
import com.sleepbetter.app.data.AutoSummary
import java.time.ZoneId

/**
 * The app's own notifications besides playback and bedtime reminders: the
 * focus countdown, "focus done", and the morning summary of an auto-tracked
 * night. Each quietly does nothing without the notification permission.
 */
object Notifier {
    private const val FOCUS_ID = 51
    private const val FOCUS_DONE_ID = 52
    private const val MORNING_ID = 53

    fun allowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun open(context: Context, destination: String, request: Int): PendingIntent = PendingIntent.getActivity(
        context, request,
        Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_DESTINATION, destination)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun pico(context: Context) =
        runCatching { ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)?.toBitmap(256, 256) }.getOrNull()

    /** The running (or paused) focus session, counting down in the notification shade. */
    @SuppressLint("MissingPermission")
    fun focus(context: Context, remainingSeconds: Int, session: Int, running: Boolean) {
        if (!allowed(context)) return
        val mmss = "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
        val builder = NotificationCompat.Builder(context, SleepBetterApp.CHANNEL_TIMERS)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle(if (running) "Focus 🎧 session $session of 4" else "Focus paused ⏸️ · $mmss left")
            .setContentText(if (running) "Stay with it. A break is coming up ☕" else "Tap to pick up where you left off")
            .setContentIntent(open(context, "FOCUS", 61))
            .setOngoing(running)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(25 * 60, 25 * 60 - remainingSeconds, false)
        if (running) {
            builder.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + remainingSeconds * 1000L)
                .setShowWhen(true)
        }
        NotificationManagerCompat.from(context).notify(FOCUS_ID, builder.build())
    }

    fun clearFocus(context: Context) = NotificationManagerCompat.from(context).cancel(FOCUS_ID)

    /** A session finished: celebrate and suggest the break. */
    @SuppressLint("MissingPermission")
    fun focusDone(context: Context, finishedSession: Int) {
        clearFocus(context)
        if (!allowed(context)) return
        val longBreak = finishedSession % 4 == 0
        val n = NotificationCompat.Builder(context, SleepBetterApp.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_moon)
            .setLargeIcon(pico(context))
            .setContentTitle("Focus session done 🎉")
            .setContentText(
                if (longBreak) "Four in a row, amazing! Take a longer 20 minute break 🌿"
                else "Nice work! Take a 5 minute break, then start the next one 🧃",
            )
            .setContentIntent(open(context, "FOCUS", 62))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(FOCUS_DONE_ID, n)
    }

    /** Good morning: last night as worked out from the phone, with a tap to rate it. */
    @SuppressLint("MissingPermission")
    fun morning(context: Context, summary: AutoSummary) {
        if (!allowed(context)) return
        val night = summary.night
        val zone = ZoneId.systemDefault()
        fun at(i: java.time.Instant) = i.atZone(zone).toLocalTime().let { "%02d:%02d".format(it.hour, it.minute) }
        val hours = "${night.minutes / 60}h ${night.minutes % 60}m"
        val checks = when (night.wakeUps) {
            0 -> "No phone checks in the night 🌟"
            1 -> "You checked your phone once in the night"
            else -> "You checked your phone ${night.wakeUps} times in the night"
        }
        val rate = open(context, "CHECK_IN", 63)
        val n = NotificationCompat.Builder(context, SleepBetterApp.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_moon)
            .setLargeIcon(pico(context))
            .setContentTitle("Good morning ☀️ You slept $hours")
            .setContentText("😴 ${at(night.asleep)} → ⏰ ${at(night.awake)}. How did you sleep?")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "😴 Asleep around ${at(night.asleep)}, up at ${at(night.awake)}.\n$checks.\nTap to rate your night.",
                ),
            )
            .setContentIntent(rate)
            .addAction(0, "Rate my night", rate)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(MORNING_ID, n)
    }
}
