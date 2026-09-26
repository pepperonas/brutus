package com.pepperonas.brutus.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pepperonas.brutus.data.AlarmEntity
import android.os.Build
import com.pepperonas.brutus.receiver.AlarmReceiver
import com.pepperonas.brutus.util.NextAlarmCalculator
import com.pepperonas.brutus.util.RingingStore

object AlarmScheduler {

    /** Follow-up offsets after Ultra Hardcore main-alarm dismiss (in minutes). */
    val ULTRA_HARDCORE_FOLLOWUP_OFFSETS_MIN = intArrayOf(10, 15)

    /** Sunrise pre-alarm fires this many minutes before the main alarm. */
    const val SUNRISE_LEAD_MIN = 10

    fun schedule(context: Context, alarm: AlarmEntity) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val nextTrigger = calculateNextTrigger(alarm)
        val intent = createPendingIntent(context, alarm)

        setClock(alarmManager, nextTrigger, intent)

        // Always clear any previously armed sunrise first — if the new trigger is
        // less than SUNRISE_LEAD_MIN away we would otherwise leave a stale sunrise
        // registration (with an outdated mainTriggerAt) armed for the old time.
        cancelSunrise(context, alarm.id)
        if (alarm.sunriseEnabled) {
            val sunriseAt = nextTrigger - SUNRISE_LEAD_MIN * 60_000L
            if (sunriseAt > System.currentTimeMillis()) {
                val sunriseIntent = createSunrisePendingIntent(context, alarm.id, nextTrigger)
                if (canScheduleExact(alarmManager)) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, sunriseAt, sunriseIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, sunriseAt, sunriseIntent)
                }
            }
        }
    }

    fun cancel(context: Context, alarm: AlarmEntity) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(createPendingIntent(context, alarm))
        cancelSunrise(context, alarm.id)
    }

    fun cancelSunrise(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(createSunrisePendingIntent(context, alarmId, 0L))
    }

    /**
     * Snooze gets its own registration (request-code space [SNOOZE_CODE_BASE]) so it neither
     * replaces the alarm's next regular occurrence nor gets replaced by it, and is remembered in
     * [RingingStore] so a reboot during the snooze does not lose it. Snoozing a follow-up keeps
     * it a follow-up — otherwise the snooze would re-arm the whole Ultra Hardcore chain.
     */
    fun scheduleSnooze(
        context: Context,
        alarm: AlarmEntity,
        isFollowup: Boolean = false,
        followupSeq: Int = 0,
        now: Long = System.currentTimeMillis(),
    ): Long {
        val triggerAt = now + alarm.snoozeDuration * 60_000L
        registerSnooze(context, alarm.id, triggerAt, isFollowup, followupSeq)
        RingingStore.recordSnooze(context, RingingStore.Snooze(alarm.id, triggerAt, isFollowup, followupSeq))
        return triggerAt
    }

    /** Re-registers a remembered snooze (after a reboot or clock change). */
    fun restoreSnooze(context: Context, snooze: RingingStore.Snooze) =
        registerSnooze(context, snooze.alarmId, snooze.triggerAt, snooze.isFollowup, snooze.followupSeq)

    fun cancelSnooze(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(createSnoozePendingIntent(context, alarmId, false, 0))
        RingingStore.clearSnooze(context, alarmId)
    }

    private fun registerSnooze(
        context: Context, alarmId: Long, triggerAt: Long, isFollowup: Boolean, followupSeq: Int,
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        setClock(alarmManager, triggerAt, createSnoozePendingIntent(context, alarmId, isFollowup, followupSeq))
    }

    /** Schedules a single Ultra Hardcore follow-up alarm (seq is 1 or 2). */
    fun scheduleFollowup(context: Context, alarm: AlarmEntity, seq: Int, triggerAt: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        setClock(alarmManager, triggerAt, createFollowupPendingIntent(context, alarm.id, seq))
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

    /**
     * One definition of "next occurrence" for the whole app — the list header, the widget and
     * AlarmManager must agree. A time exactly equal to now is not "next" (it would fire again
     * immediately when the service reschedules a repeating alarm at its own trigger instant).
     */
    private fun calculateNextTrigger(alarm: AlarmEntity, now: Long = System.currentTimeMillis()): Long =
        NextAlarmCalculator.nextTrigger(alarm, now)
            ?: (now + 24 * 60 * 60_000L) // unreachable for a valid bitmask; never register in the past

    private fun canScheduleExact(alarmManager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * setAlarmClock throws SecurityException on Android 12/12L while the exact-alarm permission is
     * revoked (13+ keeps it through USE_EXACT_ALARM). An uncaught throw here kills the service while
     * it rings. Degrade to an inexact wake-up instead — late beats never; the alarm list shows the
     * "exact alarms disabled" banner, and SystemChangeReceiver re-registers everything exactly as
     * soon as the permission is back.
     */
    private fun setClock(alarmManager: AlarmManager, triggerAt: Long, intent: PendingIntent) {
        if (canScheduleExact(alarmManager)) {
            try {
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, intent), intent)
                return
            } catch (_: SecurityException) { /* fall through */ }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
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

    private fun createSunrisePendingIntent(
        context: Context,
        alarmId: Long,
        mainTriggerAt: Long,
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra(EXTRA_IS_SUNRISE, true)
            putExtra(EXTRA_MAIN_TRIGGER_AT, mainTriggerAt)
            action = ACTION_ALARM_TRIGGER
        }
        return PendingIntent.getBroadcast(
            context,
            sunriseRequestCode(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun sunriseRequestCode(alarmId: Long): Int =
        0x2D000000 or (alarmId.toInt() and 0x00FFFFFF)

    private fun createSnoozePendingIntent(
        context: Context,
        alarmId: Long,
        isFollowup: Boolean,
        followupSeq: Int,
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra(EXTRA_IS_SNOOZE, true)
            putExtra(EXTRA_IS_FOLLOWUP, isFollowup)
            putExtra(EXTRA_FOLLOWUP_SEQ, followupSeq)
            action = ACTION_ALARM_TRIGGER
        }
        return PendingIntent.getBroadcast(
            context,
            snoozeRequestCode(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    const val SNOOZE_CODE_BASE = 0x5A000000

    fun snoozeRequestCode(alarmId: Long): Int =
        SNOOZE_CODE_BASE or (alarmId.toInt() and 0x00FFFFFF)

    const val ACTION_ALARM_TRIGGER = "com.pepperonas.brutus.ALARM_TRIGGER"
    const val EXTRA_IS_FOLLOWUP = "is_followup"
    const val EXTRA_FOLLOWUP_SEQ = "followup_seq"
    const val EXTRA_IS_SUNRISE = "is_sunrise"
    const val EXTRA_MAIN_TRIGGER_AT = "main_trigger_at"
    const val EXTRA_IS_SNOOZE = "is_snooze"
}
