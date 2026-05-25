package com.pepperonas.brutus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pepperonas.brutus.scheduler.AlarmScheduler
import com.pepperonas.brutus.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", -1)
        if (alarmId == -1L) return

        val isFollowup = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_FOLLOWUP, false)
        val followupSeq = intent.getIntExtra(AlarmScheduler.EXTRA_FOLLOWUP_SEQ, 0)

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra(AlarmScheduler.EXTRA_IS_FOLLOWUP, isFollowup)
            putExtra(AlarmScheduler.EXTRA_FOLLOWUP_SEQ, followupSeq)
            action = AlarmService.ACTION_START
        }
        context.startForegroundService(serviceIntent)
    }
}
