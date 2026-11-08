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
        val prefs = Storage.prefs(context, PREFS)
        prefs.getString(KEY_QR, null)?.let { return it }
        val fresh = QrGenerator.generateData()
        prefs.edit().putString(KEY_QR, fresh).apply()
        return fresh
    }

    /** Only a code Brutus itself made ("brutus:<uuid>") — anything else could not be printed back. */
    fun isValid(data: String): Boolean = QR_FORMAT.matches(data)

    /** Takes over a code from a backup, so a printed copy keeps working on a new phone. */
    fun set(context: Context, data: String) {
        require(isValid(data)) { "not a Brutus QR code" }
        Storage.prefs(context, PREFS).edit().putString(KEY_QR, data).commit()
    }

    private val QR_FORMAT = Regex("^brutus:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
}
