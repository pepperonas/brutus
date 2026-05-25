package com.pepperonas.brutus.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Aggressive battery managers (Xiaomi, Huawei, Samsung) routinely kill background
 * apps — which silently drops scheduled alarms. We surface the "App is restricted"
 * state in the UI and deep-link to the system page where the user can whitelist
 * Brutus.
 */
object BatteryOptimizationPermission {

    /** True when Brutus is allowed to run without battery-saver interference. */
    fun isIgnoring(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Returns the Intent that brings up the system dialog asking the user to
     * whitelist this app. Falls back to the general battery-optimization
     * settings list if the targeted dialog is unavailable on the device.
     */
    fun settingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun fallbackSettingsIntent(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
