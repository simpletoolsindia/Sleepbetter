package com.sleepbetter.app.data

import android.content.Context
import com.sleepbetter.core.audio.SoundId
import com.sleepbetter.core.mix.Mix
import com.sleepbetter.core.mix.MixLayer
import com.sleepbetter.core.sleep.AutoNight
import com.sleepbetter.core.sleep.AutoSleepDetector
import com.sleepbetter.core.sleep.NightTag
import com.sleepbetter.core.sleep.PhoneUse
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
import java.time.LocalDate
import java.time.ZoneId

data class SleepSettings(
    /** Minutes after midnight. */
    val bedtimeMinute: Int = 22 * 60 + 50,
    val remindersOn: Boolean = false,
    val windDownMinutes: Int = 30,
    /** Name of the colour theme (an AppTheme entry). */
    val theme: String = "MOON_MILK",
    /** AUTO, LIGHT or DARK. */
    val appearance: String = "AUTO",
    /** Work out nights from when the phone was put down and picked up. */
    val autoTrack: Boolean = false,
) {
    val bedtimeLabel: String get() = "%02d:%02d".format(bedtimeMinute / 60, bedtimeMinute % 60)
}

/** The latest night found from phone use, with the screen-on times around it for the timeline. */
data class AutoSummary(val night: AutoNight, val uses: List<PhoneUse>)

data class SavedMix(val id: String, val mix: Mix)

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

    private val _savedMixes = MutableStateFlow(loadMixes())

    /** Mixes the user saved or received, newest first. */
    val savedMixes: StateFlow<List<SavedMix>> = _savedMixes.asStateFlow()

    private val _lastAuto = MutableStateFlow(loadLastAuto())

    /** The most recent night tracked from phone use, for the home card. */
    val lastAuto: StateFlow<AutoSummary?> = _lastAuto.asStateFlow()

    /**
     * Stores nights found from phone use. A night logged with sleep mode always
     * wins; a morning the user said was wrong is never filled in again. A night
     * found again with new times (it was looked at mid-night) replaces the old
     * guess and keeps its rating.
     */
    fun applyAutoNights(nights: List<AutoNight>, latestUses: List<PhoneUse>) {
        val dismissed = dismissedMornings()
        var list = _sessions.value
        var latest: AutoNight? = null
        for (n in nights.sortedBy { it.morning }) {
            if (n.morning.toString() in dismissed) continue
            val session = n.toSession()
            if (list.any { !it.auto && it.start.isBefore(session.end) && it.end.isAfter(session.start) }) continue
            val (from, to) = AutoSleepDetector.windowFor(n.morning, zone)
            val old = list.firstOrNull { it.auto && it.end.isAfter(from) && !it.end.isAfter(to) }
            list = list.filterNot { it === old } + session.copy(rating = old?.rating, tags = old?.tags ?: emptySet())
            latest = n
        }
        list = list.sortedBy { it.end }
        if (list != _sessions.value) save(list)
        if (latest != null && latest.morning >= (_lastAuto.value?.night?.morning ?: LocalDate.MIN)) {
            val (from, to) = AutoSleepDetector.windowFor(latest.morning, zone)
            storeLastAuto(AutoSummary(latest, latestUses.filter { it.end.isAfter(from) && it.start.isBefore(to) }))
        }
    }

    /** "That's not right": removes the latest tracked night and doesn't guess that morning again. */
    fun dismissLastAuto() {
        val summary = _lastAuto.value ?: return
        prefs.edit().putString("auto_dismissed", (dismissedMornings() + summary.night.morning.toString()).takeLast(60).joinToString(",")).apply()
        val (from, to) = AutoSleepDetector.windowFor(summary.night.morning, zone)
        save(_sessions.value.filterNot { it.auto && it.end.isAfter(from) && !it.end.isAfter(to) })
        storeLastAuto(null)
    }

    private fun dismissedMornings(): List<String> =
        prefs.getString("auto_dismissed", null)?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    private fun storeLastAuto(summary: AutoSummary?) {
        if (summary == null) {
            prefs.edit().remove("last_auto").apply()
        } else {
            val n = summary.night
            val json = JSONObject()
                .put("morning", n.morning.toString())
                .put("down", n.phoneDown.toEpochMilli())
                .put("up", n.pickedUp.toEpochMilli())
                .put("asleep", n.asleep.toEpochMilli())
                .put("wakeUps", n.wakeUps)
                .put("uses", JSONArray(summary.uses.takeLast(200).map { JSONArray().put(it.start.toEpochMilli()).put(it.end.toEpochMilli()) }))
            prefs.edit().putString("last_auto", json.toString()).apply()
        }
        _lastAuto.value = summary
    }

    private fun loadLastAuto(): AutoSummary? {
        val raw = prefs.getString("last_auto", null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            val uses = o.optJSONArray("uses") ?: JSONArray()
            AutoSummary(
                AutoNight(
                    morning = LocalDate.parse(o.getString("morning")),
                    phoneDown = Instant.ofEpochMilli(o.getLong("down")),
                    pickedUp = Instant.ofEpochMilli(o.getLong("up")),
                    asleep = Instant.ofEpochMilli(o.getLong("asleep")),
                    wakeUps = o.optInt("wakeUps", 0),
                ),
                (0 until uses.length()).map { i ->
                    val u = uses.getJSONArray(i)
                    PhoneUse(Instant.ofEpochMilli(u.getLong(0)), Instant.ofEpochMilli(u.getLong(1)))
                },
            )
        }.getOrNull()
    }

    fun saveMix(mix: Mix): SavedMix {
        val saved = SavedMix(java.util.UUID.randomUUID().toString(), mix)
        storeMixes(listOf(saved) + _savedMixes.value.filterNot { it.mix.name == mix.name })
        return saved
    }

    fun deleteMix(id: String) = storeMixes(_savedMixes.value.filterNot { it.id == id })

    private fun storeMixes(list: List<SavedMix>) {
        val json = JSONArray()
        list.take(100).forEach { m ->
            json.put(
                JSONObject().put("id", m.id).put("name", m.mix.name).put(
                    "layers",
                    JSONArray(m.mix.layers.map { JSONObject().put("s", it.sound.code).put("x", it.x.toDouble()).put("y", it.y.toDouble()) }),
                ),
            )
        }
        prefs.edit().putString("mixes", json.toString()).apply()
        _savedMixes.value = list
    }

    private fun loadMixes(): List<SavedMix> {
        val raw = prefs.getString("mixes", null) ?: return emptyList()
        val byCode = SoundId.entries.associateBy { it.code }
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val layers = o.getJSONArray("layers")
                val list = (0 until layers.length()).mapNotNull { j ->
                    val l = layers.getJSONObject(j)
                    byCode[l.optString("s")]?.let { MixLayer(it, l.optDouble("x", 0.0).toFloat(), l.optDouble("y", 0.0).toFloat()) }
                }
                if (list.isEmpty()) null else SavedMix(o.getString("id"), Mix.of(o.optString("name"), list))
            }
        }.getOrDefault(emptyList())
    }

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
        save(_sessions.value.filterNot { it.auto && it.start.isBefore(session.end) && it.end.isAfter(session.start) } + session)
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
            .putString("theme", s.theme)
            .putString("appearance", s.appearance)
            .putBoolean("auto_track", s.autoTrack)
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
                    .put("tags", JSONArray(s.tags.map { it.name }))
                    .put("auto", s.auto),
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
                    auto = o.optBoolean("auto", false),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun loadSettings() = SleepSettings(
        bedtimeMinute = prefs.getInt("bedtime", 22 * 60 + 50),
        remindersOn = prefs.getBoolean("reminders", false),
        windDownMinutes = prefs.getInt("wind_down", 30),
        theme = prefs.getString("theme", null) ?: "MOON_MILK",
        appearance = prefs.getString("appearance", null) ?: "AUTO",
        autoTrack = prefs.getBoolean("auto_track", false),
    )
}
