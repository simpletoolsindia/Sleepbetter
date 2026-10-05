package com.sleepbetter.app.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import androidx.core.content.ContextCompat
import com.sleepbetter.core.audio.Mixer
import com.sleepbetter.core.audio.SoundId
import com.sleepbetter.core.audio.Spatial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** How long the mix plays. Null minutes means until stopped. */
enum class TimerChoice(val label: String, val minutes: Int?) {
    MIN_30("30 m", 30),
    HOUR_1("1 h", 60),
    HOUR_2("2 h", 120),
    HOUR_8("8 h", 480),
    ALL_NIGHT("All night", null),
}

data class MixState(
    val active: Set<SoundId> = setOf(SoundId.RAIN, SoundId.TENT, SoundId.THUNDER, SoundId.NIGHT_FOREST),
    val positions: Map<SoundId, Pair<Float, Float>> = SoundId.entries.associateWith { it.stageX to it.stageY },
    val timer: TimerChoice = TimerChoice.HOUR_2,
    val playing: Boolean = false,
)

/** What the visuals need each frame. Read straight from the mixer; never blocks audio. */
data class EngineFrame(
    val levels: FloatArray = FloatArray(SoundId.entries.size),
    val thunderStrikes: Int = 0,
    val musicBeats: Int = 0,
    val beatPhase: Float = 0f,
    val remainingSeconds: Int? = null,
)

/**
 * Owns the [Mixer] and the [AudioTrack] thread. The UI changes the mix
 * through this class; playback in the background is kept alive by
 * [PlaybackService].
 */
class AudioEngine(private val context: Context) {
    private val sampleRate = 48_000
    private val mixer = Mixer(sampleRate)
    private val library = RecordingLibrary(context)
    private val loader = java.util.concurrent.Executors.newSingleThreadExecutor { Thread(it, "SleepBetterDecode") }
    private val delivered = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<SoundId, Boolean>())
    private val audioManager = context.getSystemService(AudioManager::class.java)

    private val _state = MutableStateFlow(MixState())
    val state: StateFlow<MixState> = _state.asStateFlow()

    @Volatile private var thread: Thread? = null
    @Volatile private var running = false
    private var focusRequest: AudioFocusRequest? = null

    init {
        val s = _state.value
        s.positions.forEach { (id, p) -> mixer.setPosition(id, p.first, p.second) }
        s.active.forEach { mixer.setActive(it, true) }
    }

    fun frame(): EngineFrame = EngineFrame(
        levels = mixer.levels.copyOf(),
        thunderStrikes = mixer.thunderStrikes,
        musicBeats = mixer.musicBeats,
        beatPhase = mixer.beatPhase,
        remainingSeconds = mixer.remainingSeconds,
    )

    fun toggle(id: SoundId) = setActive(id, id !in _state.value.active)

    fun setActive(id: SoundId, active: Boolean) {
        _state.update { it.copy(active = if (active) it.active + id else it.active - id) }
        if (active && library.has(id) && id !in delivered) {
            // Decode the real recording first (a fraction of a second), then start it.
            loader.execute {
                library.load(id)?.let { mixer.useRecording(id, it) }
                delivered += id
                if (id in _state.value.active) mixer.setActive(id, true)
            }
            return
        }
        mixer.setActive(id, active)
    }

    fun setOnly(ids: Set<SoundId>) {
        SoundId.entries.forEach { setActive(it, it in ids) }
    }

    /** Position on the Sound Stage, in the unit disk around the listener. */
    fun setPosition(id: SoundId, x: Float, y: Float) {
        val (cx, cy) = Spatial.clampToStage(x, y)
        mixer.setPosition(id, cx, cy)
        _state.update { it.copy(positions = it.positions + (id to (cx to cy))) }
    }

    fun setTimer(choice: TimerChoice) {
        _state.update { it.copy(timer = choice) }
        if (running) mixer.setTimer(choice.minutes?.times(60))
    }

    fun extendTimer(seconds: Int) = mixer.extendTimer(seconds)

    /** Starts playback via the foreground service so it survives the screen turning off. */
    fun play() {
        ContextCompat.startForegroundService(context, Intent(context, PlaybackService::class.java))
    }

    fun pause() {
        context.startService(Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_PAUSE))
    }

    fun togglePlay() = if (_state.value.playing) pause() else play()

    // Called by PlaybackService only.
    internal fun startAudio() {
        if (running) return
        requestFocus()
        mixer.setMasterVolume(0.8f)
        mixer.setTimer(_state.value.timer.minutes?.times(60))
        running = true
        _state.update { it.copy(playing = true) }
        thread = Thread({ audioLoop() }, "SleepBetterAudio").also { it.start() }
    }

    internal fun stopAudio() {
        if (!running) return
        running = false
        thread?.join(1000)
        thread = null
        abandonFocus()
        _state.update { it.copy(playing = false) }
    }

    /** Set by the service so it can stop itself when the sleep timer ends. */
    internal var onFinished: (() -> Unit)? = null

    private fun audioLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val frames = 960 // 20 ms
        val minBytes = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minBytes, frames * 2 * 4 * 4))
            .build()
        val buffer = FloatArray(frames * 2)
        track.play()
        try {
            while (running) {
                mixer.render(buffer, frames)
                track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                if (mixer.finished) {
                    onFinished?.invoke()
                    break
                }
            }
            // Short fade so pausing never clicks.
            mixer.setMasterVolume(0f)
            repeat(15) {
                mixer.render(buffer, frames)
                track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
            }
        } finally {
            track.stop()
            track.release()
        }
    }

    private fun requestFocus() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS -> pause()
                    // A notification or a short call: duck rather than stop, so sleepers are not woken.
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mixer.setMasterVolume(0.25f)
                    AudioManager.AUDIOFOCUS_GAIN -> mixer.setMasterVolume(0.8f)
                }
            }
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }
}
