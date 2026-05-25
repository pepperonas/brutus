package com.pepperonas.brutus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pepperonas.brutus.SunriseActivity
import com.pepperonas.brutus.scheduler.AlarmScheduler
import com.pepperonas.brutus.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", -1)
        if (alarmId == -1L) return

        val isFollowup = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_FOLLOWUP, false)
        val followupSeq = intent.getIntExtra(AlarmScheduler.EXTRA_FOLLOWUP_SEQ, 0)
        val isSunrise = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SUNRISE, false)
        val mainTriggerAt = intent.getLongExtra(AlarmScheduler.EXTRA_MAIN_TRIGGER_AT, 0L)

        if (isSunrise) {
            // Launch the gentle pre-alarm activity directly — no service, no max volume,
            // no wake lock. The main alarm is a separate setAlarmClock registration that
            // fires when its time comes regardless of whether the sunrise screen is open.
            val sunriseIntent = Intent(context, SunriseActivity::class.java).apply {
                putExtra(SunriseActivity.EXTRA_ALARM_ID, alarmId)
                putExtra(SunriseActivity.EXTRA_MAIN_TRIGGER_AT, mainTriggerAt)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(sunriseIntent)
            return
        }

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra(AlarmScheduler.EXTRA_IS_FOLLOWUP, isFollowup)
            putExtra(AlarmScheduler.EXTRA_FOLLOWUP_SEQ, followupSeq)
            action = AlarmService.ACTION_START
        }
        context.startForegroundService(serviceIntent)
    }
}
