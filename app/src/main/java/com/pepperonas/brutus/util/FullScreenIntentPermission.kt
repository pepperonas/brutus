package com.pepperonas.brutus.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Starting with Android 14 (API 34), the USE_FULL_SCREEN_INTENT permission is
 * no longer granted by default to apps outside the "Calling" and "Alarm Clock"
 * categories — even though it's declared in the manifest. Without it, Android
 * silently downgrades full-screen alarm activities to plain heads-up
 * notifications, so the user can keep using whatever app they were in
 * when the alarm fires.
 *
 * This helper surfaces the state in the UI so the user can fix it with one tap.
 */
object FullScreenIntentPermission {

    /** True when our notifications are allowed to launch full-screen activities. */
    fun isGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val nm = context.getSystemService(NotificationManager::class.java) ?: return true
        return nm.canUseFullScreenIntent()
    }

    /**
     * Deep-links to the per-app full-screen-intent permission page on API 34+.
     * Returns null on older versions because the permission is granted at
     * install time and has no settings toggle.
     */
    fun settingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
