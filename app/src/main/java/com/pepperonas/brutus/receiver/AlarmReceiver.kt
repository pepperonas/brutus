package com.pepperonas.brutus.receiver

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.R
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
            showSunrise(context, alarmId, mainTriggerAt)
            return
        }

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra(AlarmScheduler.EXTRA_IS_FOLLOWUP, isFollowup)
            putExtra(AlarmScheduler.EXTRA_FOLLOWUP_SEQ, followupSeq)
            putExtra(AlarmScheduler.EXTRA_IS_SNOOZE, intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false))
            action = AlarmService.ACTION_START
        }
        context.startForegroundService(serviceIntent)
    }

    /**
     * The gentle pre-alarm — no service, no max volume. Android 10+ blocks activity starts from a
     * background receiver, so the canonical route is a full-screen-intent notification (the same
     * mechanism the ringing alarm uses); the direct start stays as a fallback for when the app is
     * in the foreground. The main alarm is a separate registration and fires on its own.
     */
    private fun showSunrise(context: Context, alarmId: Long, mainTriggerAt: Long) {
        val sunriseIntent = Intent(context, SunriseActivity::class.java).apply {
            putExtra(SunriseActivity.EXTRA_ALARM_ID, alarmId)
            putExtra(SunriseActivity.EXTRA_MAIN_TRIGGER_AT, mainTriggerAt)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = PendingIntent.getActivity(
            context,
            0x2E000000 or (alarmId.toInt() and 0x00FFFFFF),
            sunriseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, BrutusApplication.CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.sunrise_title))
            .setContentText(context.getString(R.string.sunrise_running))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .setAutoCancel(true)
            .setTimeoutAfter(AlarmScheduler.SUNRISE_LEAD_MIN * 60_000L)
            .build()
        try {
            context.getSystemService(NotificationManager::class.java)
                .notify(SunriseActivity.notificationIdForSunrise(alarmId), notification)
        } catch (_: SecurityException) { /* notifications blocked — the direct start below remains */ }
        try {
            context.startActivity(sunriseIntent)
        } catch (_: Exception) { /* background start blocked — the full-screen intent handles it */ }
    }
}
