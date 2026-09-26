package com.pepperonas.brutus.scheduler

import android.content.Context
import com.pepperonas.brutus.data.AlarmDatabase
import com.pepperonas.brutus.data.AlarmRepository
import com.pepperonas.brutus.util.AlarmNotifier
import com.pepperonas.brutus.util.NextAlarmCalculator
import com.pepperonas.brutus.util.RingingStore
import com.pepperonas.brutus.widget.NextAlarmWidget

/** What the notification actions do. Kept out of the receivers so tests can call them directly. */
object AlarmActions {

    /**
     * "Dismiss early": the next occurrence does not ring. A one-shot alarm is simply switched off;
     * a repeating one remembers the skipped occurrence ([RingingStore.skip]) so a reboot or a
     * reschedule cannot bring it back, and is re-armed for the one after.
     */
    suspend fun skipNext(
        context: Context,
        alarmId: Long,
        solvedChallenge: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        val repo = AlarmRepository(AlarmDatabase.getInstance(context).alarmDao())
        val alarm = repo.getById(alarmId) ?: return false
        // A Hardcore alarm is only ever dismissed early through its challenge — never by a button.
        if (alarm.hardcoreEffective && !solvedChallenge) return false
        AlarmNotifier.cancelUpcoming(context, alarmId)
        if (alarm.repeatDays == 0) {
            AlarmScheduler.cancel(context, alarm)
            repo.setEnabled(alarmId, false)
        } else {
            val occurrence = NextAlarmCalculator.nextTrigger(alarm, now, RingingStore.skips(context)[alarmId]) ?: return false
            RingingStore.skip(context, alarmId, occurrence)
            AlarmScheduler.schedule(context, alarm)
        }
        NextAlarmWidget.refresh(context)
        return true
    }

    /** "Cancel snooze" — the snoozed alarm does not ring again (Hardcore alarms never offer it). */
    fun cancelSnooze(context: Context, alarmId: Long) {
        AlarmScheduler.cancelSnooze(context, alarmId)
        NextAlarmWidget.refresh(context)
    }
}
