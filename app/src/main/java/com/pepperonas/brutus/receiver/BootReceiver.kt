package com.pepperonas.brutus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.data.AlarmRepository
import com.pepperonas.brutus.scheduler.AlarmScheduler
import com.pepperonas.brutus.util.UltraHardcoreStore
import com.pepperonas.brutus.widget.NextAlarmWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val app = context.applicationContext as BrutusApplication
        val repo = AlarmRepository(app.database.alarmDao())

        // goAsync keeps the process alive until finish() — without it Android may
        // kill us mid-reschedule right after boot, silently dropping every alarm.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = repo.getEnabledAlarms()
                enabled.forEach { alarm ->
                    AlarmScheduler.schedule(context, alarm)
                }

                // Recover any Ultra Hardcore follow-ups whose trigger time is still in the future.
                // Past entries (probably already fired before reboot or expired) get cleaned out.
                val now = System.currentTimeMillis()
                val pending = UltraHardcoreStore.listPending(context)
                val byId = enabled.associateBy { it.id }
                pending.forEach { p ->
                    val alarm = byId[p.alarmId]
                    if (alarm == null || p.triggerAt <= now) {
                        UltraHardcoreStore.clearFollowup(context, p.alarmId, p.seq)
                    } else {
                        AlarmScheduler.scheduleFollowup(context, alarm, p.seq, p.triggerAt)
                    }
                }

                NextAlarmWidget.refresh(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
