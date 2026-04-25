package com.pepperonas.brutus.util

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Helper around the SCHEDULE_EXACT_ALARM permission. Samsung's One UI revokes
 * this permission by default for third-party apps, which silently breaks
 * exact alarm scheduling. We surface the state in the UI and offer a deep
 * link to the system settings page so the user can fix it.
 */
object ExactAlarmPermission {

    /** True when we are allowed to use AlarmManager.setAlarmClock() exactly. */
    fun isGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java)
        return am?.canScheduleExactAlarms() == true
    }

    /**
     * Builds the Intent that takes the user to the per-app permission page.
     * On Android < 12 this returns null (no toggle exists; permission is
     * granted at install time).
     */
    fun settingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
