package com.pepperonas.brutus.util

import android.content.Context

/**
 * Simple SharedPreferences-backed store for the user's selected time-zone
 * IDs in the World Clock screen. Persists as a newline-separated list.
 */
object WorldClockStore {

    private const val PREFS = "brutus_world_clock"
    private const val KEY_ZONES = "zones"
    private val DEFAULTS = listOf("Europe/Berlin", "America/New_York", "Asia/Tokyo")

    fun load(context: Context): List<String> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ZONES, null)
        if (raw == null) {
            save(context, DEFAULTS)
            return DEFAULTS
        }
        return raw.split("\n").filter { it.isNotBlank() }
    }

    fun save(context: Context, zones: List<String>) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ZONES, zones.joinToString("\n")).apply()
    }
}
