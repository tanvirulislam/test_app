package com.personal.shortscounter.data

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

/**
 * Native storage shared by the accessibility service and the Flutter UI.
 * Counts are stored per day, keyed by the date in [TIMEZONE], so a new day
 * starts at zero without any alarm or background job.
 */
class CounterStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun today(): DayCount = countFor(currentDate())

    fun increment(): DayCount {
        val date = currentDate()
        val updated = DayCount(date, countFor(date).count + 1)
        prefs.edit().putInt(countKey(date), updated.count).apply()
        return updated
    }

    fun resetToday(): DayCount {
        val date = currentDate()
        prefs.edit().remove(countKey(date)).apply()
        return DayCount(date, 0)
    }

    var dailyLimit: Int
        get() = prefs.getInt(KEY_DAILY_LIMIT, DEFAULT_DAILY_LIMIT)
        set(value) = prefs.edit().putInt(KEY_DAILY_LIMIT, value).apply()

    var debugMode: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_MODE, value).apply()

    private fun countFor(date: LocalDate) = DayCount(date, prefs.getInt(countKey(date), 0))

    private fun currentDate(): LocalDate = LocalDate.now(ZoneId.of(TIMEZONE))

    private fun countKey(date: LocalDate) = "count_$date"

    companion object {
        /** Counts reset at midnight in this time zone. */
        const val TIMEZONE = "Asia/Dhaka"

        private const val PREFS_NAME = "shorts_counter_data"
        private const val KEY_DAILY_LIMIT = "settings_daily_limit"
        private const val KEY_DEBUG_MODE = "settings_debug_mode"
        private const val DEFAULT_DAILY_LIMIT = 30
    }
}
