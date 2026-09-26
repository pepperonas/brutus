package com.pepperonas.brutus.util

import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager

/**
 * Where everything the ringing path needs is stored: **device-protected storage**.
 *
 * After a reboot the phone stays locked until the user enters the PIN, and until then
 * credential-encrypted storage (the default for databases and SharedPreferences) cannot be
 * read — opening it throws. An alarm clock has to ring anyway, e.g. after an overnight OS
 * update. Alarm data is not secret, so it lives in device-protected storage, which is
 * available from `LOCKED_BOOT_COMPLETED` on.
 *
 * Installs from before v2.3.1 kept the data in credential storage; [migrateIfNeeded] moves it
 * once, the first time the app runs unlocked. Until that has happened, [isReady] is false and
 * the locked-boot path does nothing (the regular `BOOT_COMPLETED` after unlocking handles it).
 */
object Storage {

    const val DATABASE = "brutus_alarms.db"

    /** Every SharedPreferences file of the app — all of them move, so no store is left behind. */
    val PREFERENCE_FILES = listOf(
        "brutus_ultra_hardcore",
        "brutus_global",
        "brutus_updates",
        "brutus_timer",
        "brutus_world_clock",
        "brutus_ringing",
    )

    private const val MARKER_FILE = "brutus_storage"
    private const val KEY_MIGRATED = "migrated_v1"

    fun device(context: Context): Context {
        val app = context.applicationContext ?: context
        return if (app.isDeviceProtectedStorage) app else app.createDeviceProtectedStorageContext()
    }

    fun prefs(context: Context, name: String): SharedPreferences =
        device(context).getSharedPreferences(name, Context.MODE_PRIVATE)

    fun isUserUnlocked(context: Context): Boolean =
        context.getSystemService(UserManager::class.java)?.isUserUnlocked ?: true

    /** True once the data is in device-protected storage and may be read while locked. */
    fun isReady(context: Context): Boolean =
        prefs(context, MARKER_FILE).getBoolean(KEY_MIGRATED, false)

    /**
     * Moves the database and every preference file from credential to device-protected storage.
     * Must run before the first database or preference access of the process. Does nothing while
     * the user is locked (credential storage is unreadable then) or when it already ran.
     */
    fun migrateIfNeeded(context: Context) {
        if (isReady(context) || !isUserUnlocked(context)) return
        val app = context.applicationContext ?: context
        val device = device(app)
        if (app.getDatabasePath(DATABASE).exists()) {
            device.moveDatabaseFrom(app, DATABASE)
        }
        PREFERENCE_FILES.forEach { device.moveSharedPreferencesFrom(app, it) }
        prefs(app, MARKER_FILE).edit().putBoolean(KEY_MIGRATED, true).commit()
    }
}
