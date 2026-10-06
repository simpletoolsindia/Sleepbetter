package com.sleepbetter.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.sleepbetter.app.audio.AudioEngine
import com.sleepbetter.app.data.SleepRepository
import com.sleepbetter.app.sleep.MorningCheck

/** Holds the app-wide objects. Small enough that a DI framework is not worth it yet. */
class SleepBetterApp : Application() {
    companion object {
        const val CHANNEL_PLAYBACK = "playback"
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_TIMERS = "timers"
        const val CHANNEL_ALERTS = "alerts"
    }

    lateinit var engine: AudioEngine
        private set
    lateinit var repository: SleepRepository
        private set

    override fun onCreate() {
        super.onCreate()
        engine = AudioEngine(this)
        repository = SleepRepository(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PLAYBACK, getString(R.string.channel_playback), NotificationManager.IMPORTANCE_LOW),
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT),
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMERS, getString(R.string.channel_timers), NotificationManager.IMPORTANCE_LOW),
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_DEFAULT),
        )
        if (repository.settings.value.autoTrack) MorningCheck.schedule(this)
    }
}
