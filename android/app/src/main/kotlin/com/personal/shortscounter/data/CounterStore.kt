package com.personal.shortscounter.data

import android.content.Context

/** Native storage shared by the accessibility service and the Flutter UI. */
class CounterStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var debugMode: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_MODE, value).apply()

    private companion object {
        const val PREFS_NAME = "shorts_counter_data"
        const val KEY_DEBUG_MODE = "settings_debug_mode"
    }
}
