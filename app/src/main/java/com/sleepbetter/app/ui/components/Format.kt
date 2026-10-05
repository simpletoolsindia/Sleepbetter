package com.sleepbetter.app.ui.components

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

fun hm(minuteOfDay: Int): String = "%02d:%02d".format(Math.floorMod(minuteOfDay, 1440) / 60, Math.floorMod(minuteOfDay, 1440) % 60)

/** "42 min", "1 h 5 min". */
fun durationLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

/** "7h 12m" for big numerals. */
fun hoursMinutes(minutes: Int): String = "${minutes / 60}h ${minutes % 60}m"

fun greeting(now: LocalTime = LocalTime.now()): String = when (now.hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

fun todayLabel(date: LocalDate = LocalDate.now()): String =
    "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}"

fun clock(seconds: Int?): String {
    if (seconds == null) return "All night"
    return "%d:%02d:%02d".format(seconds / 3600, seconds % 3600 / 60, seconds % 60)
}
