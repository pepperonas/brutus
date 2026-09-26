package com.pepperonas.brutus.util

import android.content.Context

/** App-wide settings that the alarm path reads — in device-protected storage like everything else. */
object AppSettings {
    private const val PREFS = "brutus_settings"
    private const val KEY_UPCOMING_LEAD = "upcoming_lead_minutes"

    /** Offered lead times for the "alarm soon" heads-up; 0 = off. */
    val UPCOMING_LEAD_OPTIONS = listOf(0, 30, 60, 120)
    const val DEFAULT_UPCOMING_LEAD = 60

    fun upcomingLeadMinutes(context: Context): Int =
        Storage.prefs(context, PREFS).getInt(KEY_UPCOMING_LEAD, DEFAULT_UPCOMING_LEAD)
            .takeIf { it in UPCOMING_LEAD_OPTIONS } ?: DEFAULT_UPCOMING_LEAD

    fun setUpcomingLeadMinutes(context: Context, minutes: Int) {
        require(minutes in UPCOMING_LEAD_OPTIONS) { "unsupported lead $minutes" }
        Storage.prefs(context, PREFS).edit().putInt(KEY_UPCOMING_LEAD, minutes).commit()
    }
}
