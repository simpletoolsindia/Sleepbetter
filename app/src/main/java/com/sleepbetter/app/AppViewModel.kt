package com.sleepbetter.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sleepbetter.app.audio.TimerChoice
import com.sleepbetter.app.reminders.ReminderScheduler
import com.sleepbetter.app.notify.Notifier
import com.sleepbetter.app.sleep.AutoSleepTracker
import com.sleepbetter.app.sleep.MorningCheck
import com.sleepbetter.app.sleep.PhoneUsage
import com.sleepbetter.app.ui.components.Haptics
import com.sleepbetter.app.ui.theme.AppTheme
import com.sleepbetter.app.ui.theme.Appearance
import com.sleepbetter.app.ui.theme.Palette
import com.sleepbetter.core.mix.Mix
import com.sleepbetter.core.mix.MixCodec
import com.sleepbetter.core.mix.MixLayer
import com.sleepbetter.core.audio.SoundId
import com.sleepbetter.core.sleep.NightTag
import com.sleepbetter.core.sleep.SleepSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Destination {
    HOME, SOUNDS, INSIGHTS, FRIENDS, FOCUS, WIND_DOWN, SLEEP, CHECK_IN, AUTO_SETUP,
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
            // Pausing the session pauses the music too.
            focusJob?.cancel()
            _focus.update { it.copy(running = false) }
            if (mix.value.playing) engine.pause()
            Notifier.focus(getApplication(), _focus.value.remainingSeconds, _focus.value.session, running = false)
            return
        }
        if (SoundId.FOCUS_MUSIC !in mix.value.active) engine.setActive(SoundId.FOCUS_MUSIC, true)
        if (!mix.value.playing) engine.play()
        _focus.update { it.copy(running = true) }
        Notifier.focus(getApplication(), _focus.value.remainingSeconds, _focus.value.session, running = true)
        focusJob = viewModelScope.launch {
            while (_focus.value.remainingSeconds > 0) {
                delay(1000)
                _focus.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
                // Refresh the shade every half minute (its progress bar; and if permission came late).
                if (_focus.value.remainingSeconds % 30 == 0) {
                    Notifier.focus(getApplication(), _focus.value.remainingSeconds, _focus.value.session, running = true)
                }
            }
            Haptics.sessionDone(getApplication())
            Notifier.focusDone(getApplication(), _focus.value.session)
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

    // Auto sleep tracking from phone use.
    val lastAuto = repository.lastAuto
    private val _usageAccess = MutableStateFlow(PhoneUsage.hasAccess(application))
    val usageAccess: StateFlow<Boolean> = _usageAccess.asStateFlow()

    /** Looks at the last week of screen-on history for nights not logged yet. Cheap; runs whenever the app comes back. */
    fun refreshAutoSleep() {
        _usageAccess.value = PhoneUsage.hasAccess(getApplication())
        if (!settings.value.autoTrack || !_usageAccess.value) return
        viewModelScope.launch(Dispatchers.Default) { AutoSleepTracker.refresh(app) }
    }

    /** Turns tracking on; returns true if Android's usage access still has to be allowed. */
    fun turnOnAutoSleep(): Boolean {
        repository.updateSettings { it.copy(autoTrack = true) }
        MorningCheck.schedule(getApplication())
        refreshAutoSleep()
        return !_usageAccess.value
    }

    fun openUsageAccess() = PhoneUsage.openSettings(getApplication())

    fun turnOffAutoSleep() {
        repository.updateSettings { it.copy(autoTrack = false) }
        MorningCheck.cancel(getApplication())
    }

    fun dismissAutoNight() = repository.dismissLastAuto()

    fun shiftBedtime(minutes: Int) {
        repository.updateSettings { it.copy(bedtimeMinute = Math.floorMod(it.bedtimeMinute + minutes, 1440)) }
        rescheduleReminders()
    }

    init {
        Palette.theme = AppTheme.entries.firstOrNull { it.name == settings.value.theme } ?: AppTheme.MOON_MILK
        Palette.appearance = Appearance.entries.firstOrNull { it.name == settings.value.appearance } ?: Appearance.AUTO
    }

    fun setAppearance(appearance: Appearance) {
        Palette.appearance = appearance
        repository.updateSettings { it.copy(appearance = appearance.name) }
    }

    fun setTheme(theme: AppTheme) {
        Palette.theme = theme
        repository.updateSettings { it.copy(theme = theme.name) }
    }

    override fun onCleared() {
        // The focus countdown lives here; don't leave a stale one in the shade.
        Notifier.clearFocus(getApplication())
        super.onCleared()
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
