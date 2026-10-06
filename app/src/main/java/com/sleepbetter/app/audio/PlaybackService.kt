package com.sleepbetter.app.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import androidx.core.app.NotificationCompat
import com.sleepbetter.app.ui.components.SceneFrames
import com.sleepbetter.core.audio.SoundId
import androidx.core.app.ServiceCompat
import com.sleepbetter.app.MainActivity
import com.sleepbetter.app.R
import com.sleepbetter.app.SleepBetterApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service (type mediaPlayback) that keeps the mix playing with the
 * screen off and shows lock-screen controls.
 */
class PlaybackService : Service() {
    companion object {
        const val ACTION_PAUSE = "com.sleepbetter.app.PAUSE"
        const val ACTION_PLAY = "com.sleepbetter.app.PLAY"
        const val ACTION_STOP = "com.sleepbetter.app.STOP"
        const val ACTION_EXTEND = "com.sleepbetter.app.EXTEND"
        private const val NOTIFICATION_ID = 7
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var session: MediaSessionCompat
    private val engine get() = (application as SleepBetterApp).engine

    override fun onCreate() {
        super.onCreate()
        session = MediaSessionCompat(this, "SleepBetter").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = engine.play() // via startForegroundService, see onStartCommand
                override fun onPause() = handle(ACTION_PAUSE)
                override fun onStop() = handle(ACTION_STOP)
            })
            isActive = true
        }
        engine.onFinished = { scope.launch { handle(ACTION_STOP) } }
        scope.launch {
            engine.state.collect { s ->
                if (s.active != artFor) updateArtwork(s.active)
                updateSession(s.playing)
                if (s.playing || hasStarted) postNotification(s.playing)
            }
        }
        // The sleep timer counts down in the mixer; keep the notification's countdown in step
        // with it (a new timer, +15 min from the app, or the moment playback started).
        scope.launch {
            while (true) {
                delay(2_000)
                val s = engine.state.value
                if (!s.playing || !hasStarted) continue
                val end = timerEnd()
                if ((end == null) != (shownEnd == null) || (end != null && kotlin.math.abs(end - shownEnd!!) > 3_000)) postNotification(true)
            }
        }
    }

    /** When the sleep timer will end (wall clock), or null with no timer. */
    private fun timerEnd(): Long? = engine.frame().remainingSeconds?.let { System.currentTimeMillis() + it * 1000L }

    /** The end time the posted notification is counting down to. */
    private var shownEnd: Long? = null

    private var hasStarted = false

    /** The scene for the current sounds, shown as the media artwork and the notification picture. */
    private var art: Bitmap? = null
    private var artFor: Set<SoundId>? = null

    private fun updateArtwork(active: Set<SoundId>) {
        artFor = active
        art = runCatching { SceneFrames.still(active, 360, 360) }.getOrNull()
        val names = active.joinToString(", ") { it.label }.ifEmpty { "SleepBetter" }
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, names)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "SleepBetter")
                .apply { art?.let { putBitmap(MediaMetadataCompat.METADATA_KEY_ART, it); putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it) } }
                .build(),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_PLAY
        if (action == ACTION_PLAY) {
            // Play always arrives via startForegroundService (from the app or the
            // notification), so promote the service right away, every time.
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(playing = true),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
            hasStarted = true
        }
        handle(action)
        return START_NOT_STICKY
    }

    private fun handle(action: String) {
        when (action) {
            ACTION_PLAY -> engine.startAudio()
            ACTION_PAUSE -> {
                engine.stopAudio()
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
            }
            ACTION_EXTEND -> engine.extendTimer(15 * 60)
            ACTION_STOP -> {
                hasStarted = false // so the state change below does not re-post a notification
                engine.stopAudio()
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun updateSession(playing: Boolean) {
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP)
                .setState(
                    if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1f,
                )
                .build(),
        )
    }

    private fun postNotification(playing: Boolean) {
        val nm = getSystemService(android.app.NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(playing))
    }

    private fun buildNotification(playing: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        // Resuming must start the service in the foreground; pausing only talks to the running service.
        val toggle = if (playing) {
            PendingIntent.getService(
                this, 1, Intent(this, PlaybackService::class.java).setAction(ACTION_PAUSE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        } else {
            PendingIntent.getForegroundService(
                this, 3, Intent(this, PlaybackService::class.java).setAction(ACTION_PLAY),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        val stop = PendingIntent.getService(
            this, 2, Intent(this, PlaybackService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val extend = PendingIntent.getService(
            this, 4, Intent(this, PlaybackService::class.java).setAction(ACTION_EXTEND), PendingIntent.FLAG_IMMUTABLE,
        )
        val names = engine.state.value.active.joinToString(", ") { it.label }.ifEmpty { "Nothing playing" }
        val left = engine.frame().remainingSeconds
        val end = if (playing) timerEnd() else null
        shownEnd = end
        val title = when {
            playing && left != null -> "Fading out in ${leftLabel(left)} 🌙"
            playing -> "Playing all night 🌙"
            left != null -> "Paused · ${leftLabel(left)} left on the timer"
            else -> "Paused"
        }
        val builder = NotificationCompat.Builder(this, SleepBetterApp.CHANNEL_PLAYBACK)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle(title)
            .setContentText(names)
            .setOnlyAlertOnce(true)
        if (end != null) {
            // The shade shows a live countdown to the fade-out.
            builder.setUsesChronometer(true).setChronometerCountDown(true).setWhen(end).setShowWhen(true).setSubText("Sleep timer")
        } else {
            builder.setShowWhen(false)
        }
        return builder
            .setLargeIcon(art)
            .setContentIntent(open)
            .setOngoing(playing)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, if (playing) "Pause" else "Play", toggle)
            .addAction(0, "Stop", stop)
            .apply { if (playing && left != null) addAction(0, "+15 min", extend) }
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1),
            )
            .build()
    }

    private fun leftLabel(seconds: Int): String {
        val m = (seconds + 59) / 60
        return if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        engine.onFinished = null
        engine.stopAudio()
        session.release()
        scope.cancel()
        super.onDestroy()
    }
}
