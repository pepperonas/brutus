package com.pepperonas.brutus.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    /** Follow-up offsets after Ultra Hardcore main-alarm dismiss (in minutes). */
    val ULTRA_HARDCORE_FOLLOWUP_OFFSETS_MIN = intArrayOf(10, 15)

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

    /** Schedules a single Ultra Hardcore follow-up alarm (seq is 1 or 2). */
    fun scheduleFollowup(context: Context, alarm: AlarmEntity, seq: Int, triggerAt: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = createFollowupPendingIntent(context, alarm.id, seq)
        val clockInfo = AlarmManager.AlarmClockInfo(triggerAt, intent)
        alarmManager.setAlarmClock(clockInfo, intent)
    }

    fun cancelFollowup(context: Context, alarmId: Long, seq: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(createFollowupPendingIntent(context, alarmId, seq))
    }

    fun cancelAllFollowups(context: Context, alarmId: Long) {
        for (seq in 1..ULTRA_HARDCORE_FOLLOWUP_OFFSETS_MIN.size) {
            cancelFollowup(context, alarmId, seq)
        }
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
            action = ACTION_ALARM_TRIGGER
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createFollowupPendingIntent(
        context: Context,
        alarmId: Long,
        seq: Int,
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra(EXTRA_IS_FOLLOWUP, true)
            putExtra(EXTRA_FOLLOWUP_SEQ, seq)
            action = ACTION_ALARM_TRIGGER
        }
        return PendingIntent.getBroadcast(
            context,
            followupRequestCode(alarmId, seq),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Carve a separate request-code space so we never collide with real alarm IDs.
    // Practical alarms have small IDs (auto-increment from 1); this leaves > 2 billion IDs free.
    private fun followupRequestCode(alarmId: Long, seq: Int): Int =
        0x4F000000 or ((alarmId.toInt() and 0x00FFFFFF) shl 4) or (seq and 0xF)

    const val ACTION_ALARM_TRIGGER = "com.pepperonas.brutus.ALARM_TRIGGER"
    const val EXTRA_IS_FOLLOWUP = "is_followup"
    const val EXTRA_FOLLOWUP_SEQ = "followup_seq"
}
