package com.pepperonas.brutus.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pepperonas.brutus.scheduler.Rescheduler
import com.pepperonas.brutus.util.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-registers every alarm after events that make AlarmManager's state wrong: a time-zone or
 * clock change (registrations are absolute timestamps — a 07:00 alarm set in Berlin would ring at
 * 06:00 in London), the exact-alarm permission being granted again (revoking it cancels all
 * alarms), and an app update.
 */
class SystemChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        if (!Storage.isReady(context)) return // pre-migration and locked: BOOT_COMPLETED handles it
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Rescheduler.rescheduleAll(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        val HANDLED = setOf(
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
