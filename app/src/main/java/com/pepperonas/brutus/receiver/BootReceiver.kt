package com.pepperonas.brutus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pepperonas.brutus.scheduler.Rescheduler
import com.pepperonas.brutus.util.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms everything after a reboot. LOCKED_BOOT_COMPLETED arrives before the user has unlocked
 * the phone; the data lives in device-protected storage (see [Storage]), so alarms can be
 * registered — and ring — before the first unlock. On an install that has not migrated its data
 * yet, the locked pass does nothing and BOOT_COMPLETED does the work after unlocking.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return
        if (!Storage.isReady(context)) return

        // goAsync keeps the process alive until finish() — without it Android may
        // kill us mid-reschedule right after boot, silently dropping every alarm.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Rescheduler.rescheduleAll(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
