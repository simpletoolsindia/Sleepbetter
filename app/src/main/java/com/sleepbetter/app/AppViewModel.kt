package com.sleepbetter.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sleepbetter.app.audio.TimerChoice
import com.sleepbetter.app.reminders.ReminderScheduler
import com.sleepbetter.app.ui.components.Haptics
import com.sleepbetter.core.mix.Mix
import com.sleepbetter.core.mix.MixCodec
import com.sleepbetter.core.mix.MixLayer
import com.sleepbetter.core.audio.SoundId
import com.sleepbetter.core.sleep.NightTag
import com.sleepbetter.core.sleep.SleepSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Destination {
    HOME, SOUNDS, INSIGHTS, FRIENDS, FOCUS, WIND_DOWN, SLEEP, CHECK_IN,
}

data class FocusState(
    val running: Boolean = false,
    val remainingSeconds: Int = FOCUS_SECONDS,
    /** 1-based index within a set of four. */
    val session: Int = 1,
)

const val FOCUS_SECONDS = 25 * 60

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SleepBetterApp
    val engine = app.engine
    val repository = app.repository

    val mix = engine.state
    val sessions = repository.sessions
    val settings = repository.settings
    val focusSessions = repository.focusSessions

    private val _focus = MutableStateFlow(FocusState())
    val focus: StateFlow<FocusState> = _focus.asStateFlow()
    private var focusJob: Job? = null

    /** The night just logged, for the morning story; null until the user wakes up in-app. */
    private val _lastNight = MutableStateFlow(sessions.value.lastOrNull())
    val lastNight: StateFlow<SleepSession?> = _lastNight.asStateFlow()

    fun toggleSound(id: SoundId) = engine.toggle(id)
    fun moveSound(id: SoundId, x: Float, y: Float) = engine.setPosition(id, x, y)
    val savedMixes = repository.savedMixes

    /** Turns on exactly this mix's sounds, each in its place. */
    fun applyMix(mix: Mix) {
        mix.layers.forEach { engine.setPosition(it.sound, it.x, it.y) }
        engine.setOnly(mix.sounds)
    }

    fun playMix(mix: Mix) {
        applyMix(mix)
        if (!this.mix.value.playing) engine.play()
    }

    /** The current sounds and their places, as a mix with [name]. */
    fun currentMix(name: String): Mix {
        val s = mix.value
        return Mix.of(name, s.active.map { id -> val p = s.positions[id] ?: (id.stageX to id.stageY); MixLayer(id, p.first, p.second) })
    }

    fun saveCurrentMix(name: String) = repository.saveMix(currentMix(name))
    fun deleteMix(id: String) = repository.deleteMix(id)

    /** A mix that arrived from someone else, waiting for the user to accept it. */
    private val _incoming = MutableStateFlow<Mix?>(null)
    val incoming: StateFlow<Mix?> = _incoming.asStateFlow()

    /** Looks for a mix in shared text, a link or a file's contents. Returns false if none was found. */
    fun offerImport(text: String): Boolean {
        val found = MixCodec.findIn(text) ?: return false
        _incoming.value = found
        return true
    }

    fun acceptIncoming(play: Boolean) {
        val m = _incoming.value ?: return
        repository.saveMix(m)
        if (play) playMix(m)
        _incoming.value = null
    }

    fun dismissIncoming() { _incoming.value = null }
    fun setTimer(choice: TimerChoice) = engine.setTimer(choice)
    fun togglePlay() = engine.togglePlay()
    fun extendTimer(seconds: Int) = engine.extendTimer(seconds)

    // Focus: 25 minutes with focus music, then a gentle buzz.
    fun toggleFocus() {
        if (_focus.value.running) {
            focusJob?.cancel()
            _focus.update { it.copy(running = false) }
            return
        }
        if (SoundId.FOCUS_MUSIC !in mix.value.active) engine.setActive(SoundId.FOCUS_MUSIC, true)
        if (!mix.value.playing) engine.play()
        _focus.update { it.copy(running = true) }
        focusJob = viewModelScope.launch {
            while (_focus.value.remainingSeconds > 0) {
                delay(1000)
                _focus.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            }
            Haptics.sessionDone(getApplication())
            repository.addFocusSession()
            _focus.update { FocusState(session = it.session % 4 + 1) }
        }
    }

    // Sleep.
    fun startSleep() {
        repository.startSleep()
        if (!mix.value.playing) engine.play()
    }

    /** Ends the night; returns false if it was too short to count. */
    fun wakeUp(): Boolean {
        engine.pause()
        val session = repository.endSleep()
        if (session != null) _lastNight.value = session
        return session != null
    }

    fun rateLastNight(rating: Int?, tags: Set<NightTag>) = repository.rateLastNight(rating, tags)

    fun shiftBedtime(minutes: Int) {
        repository.updateSettings { it.copy(bedtimeMinute = Math.floorMod(it.bedtimeMinute + minutes, 1440)) }
        rescheduleReminders()
    }

    fun setReminders(on: Boolean) {
        repository.updateSettings { it.copy(remindersOn = on) }
        rescheduleReminders()
    }

    private fun rescheduleReminders() {
        val s = settings.value
        if (s.remindersOn) ReminderScheduler.schedule(getApplication(), s.bedtimeMinute, s.windDownMinutes)
        else ReminderScheduler.cancel(getApplication())
    }
}
