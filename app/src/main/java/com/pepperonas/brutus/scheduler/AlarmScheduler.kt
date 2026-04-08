package com.pepperonas.brutus.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, alarm: AlarmEntity) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val nextTrigger = calculateNextTrigger(alarm)
        val intent = createPendingIntent(context, alarm)

        val clockInfo = AlarmManager.AlarmClockInfo(nextTrigger, intent)
        alarmManager.setAlarmClock(clockInfo, intent)
    }

    fun cancel(context: Context, alarm: AlarmEntity) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(createPendingIntent(context, alarm))
    }

    fun scheduleSnooze(context: Context, alarm: AlarmEntity) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerTime = System.currentTimeMillis() + alarm.snoozeDuration * 60_000L
        val intent = createPendingIntent(context, alarm)

        val clockInfo = AlarmManager.AlarmClockInfo(triggerTime, intent)
        alarmManager.setAlarmClock(clockInfo, intent)
    }

    private fun calculateNextTrigger(alarm: AlarmEntity): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.repeatDays == 0) {
            // One-shot: if time already passed today, schedule tomorrow
            if (target.before(now) || target == now) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // Find next matching day
        for (offset in 0..7) {
            val candidate = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }
            if (offset == 0 && candidate.before(now)) continue

            // Calendar: MONDAY=2, our bitmask: 0=Mon
            val calDay = candidate.get(Calendar.DAY_OF_WEEK)
            val dayIndex = when (calDay) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }
            if (alarm.isDayEnabled(dayIndex)) {
                return candidate.timeInMillis
            }
        }

        // Fallback: tomorrow
        target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis
    }

    private fun createPendingIntent(context: Context, alarm: AlarmEntity): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            action = "com.pepperonas.brutus.ALARM_TRIGGER"
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
