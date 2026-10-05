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
    }

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
        val names = engine.state.value.active.joinToString(", ") { it.label }.ifEmpty { "Nothing playing" }
        return NotificationCompat.Builder(this, SleepBetterApp.CHANNEL_PLAYBACK)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle(if (playing) "Your sounds are playing" else "Paused")
            .setContentText(names)
            .setLargeIcon(art)
            .setContentIntent(open)
            .setOngoing(playing)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, if (playing) "Pause" else "Play", toggle)
            .addAction(0, "Stop", stop)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1),
            )
            .build()
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
