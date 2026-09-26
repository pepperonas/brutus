package com.pepperonas.brutus.scheduler

import android.content.Context
import com.pepperonas.brutus.data.AlarmDatabase
import com.pepperonas.brutus.data.AlarmRepository
import com.pepperonas.brutus.util.AlarmNotifier
import com.pepperonas.brutus.util.RingingStore
import com.pepperonas.brutus.util.UltraHardcoreNotifier
import com.pepperonas.brutus.util.UltraHardcoreStore
import com.pepperonas.brutus.widget.NextAlarmWidget

/**
 * Brings every AlarmManager registration back in line with what the app believes is armed.
 *
 * AlarmManager forgets everything on reboot and on a force stop, cancels all alarms when the
 * exact-alarm permission is revoked, and holds absolute timestamps that a time-zone change turns
 * into the wrong wall-clock time. Each of those events calls this one function (BootReceiver,
 * SystemChangeReceiver, MainActivity on resume). It is idempotent: every registration uses a fixed
 * request code with FLAG_UPDATE_CURRENT, so running it twice never stacks alarms.
 */
object Rescheduler {

    suspend fun rescheduleAll(context: Context, now: Long = System.currentTimeMillis()) {
        val repo = AlarmRepository(AlarmDatabase.getInstance(context).alarmDao())

        // An occurrence that AlarmManager was given but that never rang (phone off, clock jumped
        // forward) is reported before the alarm is re-armed for its next occurrence.
        val expected = RingingStore.expected(context)
        val enabled = repo.getEnabledAlarms()
        enabled.forEach { alarm ->
            val due = expected[alarm.id]
            if (due != null && isMissed(due, now)) AlarmNotifier.postMissed(context, alarm, due)
        }
        enabled.forEach { AlarmScheduler.schedule(context, it) }
        // Past skips are spent; drop them so the store does not grow.
        RingingStore.skips(context).forEach { (id, at) -> if (at < now) RingingStore.clearSkip(context, id) }

        // Follow-ups belong to an alarm regardless of whether it is still enabled: a one-shot
        // Ultra Hardcore alarm disables itself the moment it fires, and its follow-ups are exactly
        // what must survive a reboot right after dismissing it.
        UltraHardcoreStore.listPending(context).forEach { p ->
            val alarm = if (p.triggerAt > now) repo.getById(p.alarmId) else null
            if (alarm == null) {
                UltraHardcoreStore.clearFollowup(context, p.alarmId, p.seq)
            } else {
                AlarmScheduler.scheduleFollowup(context, alarm, p.seq, p.triggerAt)
            }
        }
        // The reminder notification is the way into the anti-snooze task — repost it for every
        // alarm that still has follow-ups, drop it for the ones that no longer do.
        val stillPending = UltraHardcoreStore.pendingAlarmIds(context)
        stillPending.forEach { UltraHardcoreNotifier.post(context, it) }

        RingingStore.snoozes(context).forEach { s ->
            val alarm = if (s.triggerAt > now) repo.getById(s.alarmId) else null
            if (alarm != null) {
                AlarmScheduler.restoreSnooze(context, s, alarm)
            } else {
                RingingStore.clearSnooze(context, s.alarmId)
            }
        }

        NextAlarmWidget.refresh(context)
    }

    /**
     * An expected occurrence counts as missed once it is more than [MISSED_GRACE_MS] past — the
     * grace covers an alarm that is firing right now while this runs.
     */
    fun isMissed(dueAt: Long, now: Long): Boolean = dueAt < now - MISSED_GRACE_MS

    const val MISSED_GRACE_MS = 2 * 60_000L
}
