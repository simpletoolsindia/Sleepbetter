package com.sleepbetter.app.data

import android.content.Context
import com.sleepbetter.core.sleep.NightTag
import com.sleepbetter.core.sleep.SleepReport
import com.sleepbetter.core.sleep.SleepScoreCalculator
import com.sleepbetter.core.sleep.SleepSession
import com.sleepbetter.core.sleep.VisitorProgress
import com.sleepbetter.core.sleep.VisitorRules
import com.sleepbetter.core.sleep.clockDistance
import com.sleepbetter.core.sleep.bedtimeMinuteOfDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

data class SleepSettings(
    /** Minutes after midnight. */
    val bedtimeMinute: Int = 22 * 60 + 50,
    val remindersOn: Boolean = false,
    val windDownMinutes: Int = 30,
) {
    val bedtimeLabel: String get() = "%02d:%02d".format(bedtimeMinute / 60, bedtimeMinute % 60)
}

/**
 * Everything the app remembers, stored on the device only (no account, no
 * cloud). Kept deliberately simple: a small JSON list in SharedPreferences.
 */
class SleepRepository(context: Context) {
    private val prefs = context.getSharedPreferences("sleepbetter", Context.MODE_PRIVATE)
    private val zone: ZoneId get() = ZoneId.systemDefault()

    private val _sessions = MutableStateFlow(loadSessions())
    val sessions: StateFlow<List<SleepSession>> = _sessions.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SleepSettings> = _settings.asStateFlow()

    private val _focusSessions = MutableStateFlow(prefs.getInt("focus_sessions", 0))
    val focusSessions: StateFlow<Int> = _focusSessions.asStateFlow()

    /** When the user tapped "Start sleep mode", if they have not woken up yet. */
    val sleepStartedAt: Instant?
        get() = prefs.getLong("sleep_started", 0L).takeIf { it > 0 }?.let { Instant.ofEpochMilli(it) }

    fun startSleep(now: Instant = Instant.now()) {
        prefs.edit().putLong("sleep_started", now.toEpochMilli()).apply()
    }

    /** Ends the night started by [startSleep]. Nights shorter than an hour are treated as naps and not stored. */
    fun endSleep(now: Instant = Instant.now()): SleepSession? {
        val start = sleepStartedAt ?: return null
        prefs.edit().remove("sleep_started").apply()
        val session = SleepSession(start, now)
        if (session.minutes < 60) return null
        save(_sessions.value + session)
        return session
    }

    /** Morning check-in on the most recent night. */
    fun rateLastNight(rating: Int?, tags: Set<NightTag>) {
        val list = _sessions.value
        if (list.isEmpty()) return
        save(list.dropLast(1) + list.last().copy(rating = rating, tags = tags))
    }

    fun addFocusSession() {
        val n = _focusSessions.value + 1
        prefs.edit().putInt("focus_sessions", n).apply()
        _focusSessions.value = n
    }

    fun updateSettings(transform: (SleepSettings) -> SleepSettings) {
        val s = transform(_settings.value)
        prefs.edit()
            .putInt("bedtime", s.bedtimeMinute)
            .putBoolean("reminders", s.remindersOn)
            .putInt("wind_down", s.windDownMinutes)
            .apply()
        _settings.value = s
    }

    fun report(): SleepReport =
        SleepScoreCalculator(_settings.value.bedtimeMinute, zone = zone).report(_sessions.value)

    fun totalSteadyNights(): Int = _sessions.value.count {
        clockDistance(it.bedtimeMinuteOfDay(zone), _settings.value.bedtimeMinute) <= 30
    }

    fun visitorProgress(): VisitorProgress = VisitorRules.progress(totalSteadyNights(), _focusSessions.value)

    private fun save(list: List<SleepSession>) {
        val json = JSONArray()
        list.takeLast(400).forEach { s ->
            json.put(
                JSONObject()
                    .put("start", s.start.toEpochMilli())
                    .put("end", s.end.toEpochMilli())
                    .put("rating", s.rating ?: 0)
                    .put("tags", JSONArray(s.tags.map { it.name })),
            )
        }
        prefs.edit().putString("sessions", json.toString()).apply()
        _sessions.value = list
    }

    private fun loadSessions(): List<SleepSession> {
        val raw = prefs.getString("sessions", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val tags = o.optJSONArray("tags")
                SleepSession(
                    start = Instant.ofEpochMilli(o.getLong("start")),
                    end = Instant.ofEpochMilli(o.getLong("end")),
                    rating = o.optInt("rating", 0).takeIf { it in 1..5 },
                    tags = if (tags == null) emptySet() else (0 until tags.length())
                        .mapNotNull { runCatching { NightTag.valueOf(tags.getString(it)) }.getOrNull() }
                        .toSet(),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun loadSettings() = SleepSettings(
        bedtimeMinute = prefs.getInt("bedtime", 22 * 60 + 50),
        remindersOn = prefs.getBoolean("reminders", false),
        windDownMinutes = prefs.getInt("wind_down", 30),
    )
}
