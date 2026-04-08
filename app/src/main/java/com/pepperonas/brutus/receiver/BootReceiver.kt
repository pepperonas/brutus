package com.pepperonas.brutus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.data.AlarmRepository
import com.pepperonas.brutus.scheduler.AlarmScheduler
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

        CoroutineScope(Dispatchers.IO).launch {
            repo.getEnabledAlarms().forEach { alarm ->
                AlarmScheduler.schedule(context, alarm)
            }
        }
    }
}
