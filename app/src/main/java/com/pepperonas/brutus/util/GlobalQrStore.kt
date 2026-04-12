package com.pepperonas.brutus.util

import android.content.Context

/**
 * Single app-wide QR code. Generated lazily on first access and persisted
 * forever in SharedPreferences. All alarms share the same code, so one
 * printed copy is enough.
 */
object GlobalQrStore {
    private const val PREFS = "brutus_global"
    private const val KEY_QR = "qr_data"

    fun get(context: Context): String {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_QR, null)?.let { return it }
        val fresh = QrGenerator.generateData()
        prefs.edit().putString(KEY_QR, fresh).apply()
        return fresh
    }
}
