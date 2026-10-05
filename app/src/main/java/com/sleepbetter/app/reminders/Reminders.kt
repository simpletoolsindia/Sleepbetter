package com.sleepbetter.app.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.sleepbetter.app.MainActivity
import com.sleepbetter.app.R
import com.sleepbetter.app.SleepBetterApp
import com.sleepbetter.app.ui.components.SceneFrames
import com.sleepbetter.core.audio.SoundId
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Bedtime reminders. They fire a little before wind-down time and do not need
 * to be exact, so they use an inexact window: no exact-alarm permission needed.
 */
object ReminderScheduler {
    private const val REQUEST = 41

    fun schedule(context: Context, bedtimeMinute: Int, windDownMinutes: Int) {
        val remindAt = Math.floorMod(bedtimeMinute - windDownMinutes, 1440)
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        var next = ZonedDateTime.of(LocalDate.now(zone), LocalTime.of(remindAt / 60, remindAt % 60), zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.setWindow(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            10 * 60 * 1000L,
            pendingIntent(context),
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
        context, REQUEST, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class ReminderReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission") // checked just below before notifying
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as SleepBetterApp
        val settings = app.repository.settings.value
        if (!settings.remindersOn) return
        // Re-arm for tomorrow first, so one failure never stops future reminders.
        ReminderScheduler.schedule(context, settings.bedtimeMinute, settings.windDownMinutes)

        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (android.os.Build.VERSION.SDK_INT >= 33 && !granted) return

        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_DESTINATION, "WIND_DOWN"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val pip = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)?.toBitmap(256, 256)
        val title = "Pico is getting sleepy 😴"
        val text = "Bedtime is ${settings.bedtimeLabel}. Your sounds are ready when you are 🌙"
        val builder = NotificationCompat.Builder(context, SleepBetterApp.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_moon)
            .setLargeIcon(pip)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Start wind-down", open)
        animatedScene(context, app.engine.state.value.active, title, text)?.let { big ->
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomBigContentView(big)
        }
        val notification = builder.build()
        NotificationManagerCompat.from(context).notify(42, notification)
    }
}

/**
 * The reminder's expanded view: a short flip-book of the dusk scene (rain
 * falling, stars twinkling, a lightning flash if Thunder is in the mix) that
 * the system plays in a loop. Frames keep the scene's own shape (3:2) so the
 * characters stay clear of the hills, and the view crops the edges. Small
 * RGB_565 frames keep it well under the size the system accepts. Null if rendering fails; the plain reminder is used then.
 */
private fun animatedScene(context: Context, active: Set<SoundId>, title: String, text: String): RemoteViews? = runCatching {
    val sounds = active.ifEmpty { setOf(SoundId.RAIN, SoundId.TENT, SoundId.NIGHT_FOREST) }
    // Eight frames 0.07 s apart: rain moves in small steps instead of jumping.
    val times = List(8) { 2f + it * 0.07f }
    val strike = if (SoundId.THUNDER in sounds) times[2] else -10f
    val frames = SceneFrames.frames(sounds, 270, 180, times, lightningAt = strike)
    RemoteViews(context.packageName, R.layout.notification_scene).apply {
        setTextViewText(R.id.scene_title, title)
        setTextViewText(R.id.scene_text, text)
        frames.forEach { frame ->
            addView(R.id.scene_flipper, RemoteViews(context.packageName, R.layout.notification_frame).apply { setImageViewBitmap(R.id.frame, frame) })
        }
    }
}.getOrNull()
