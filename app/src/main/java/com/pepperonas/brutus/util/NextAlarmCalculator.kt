package com.pepperonas.brutus.util

import android.content.Context
import com.pepperonas.brutus.R
import com.pepperonas.brutus.data.AlarmEntity
import java.util.Calendar

object NextAlarmCalculator {

    /** Returns the soonest trigger timestamp across all enabled alarms, or null if none. */
    fun findNext(alarms: List<AlarmEntity>, now: Long = System.currentTimeMillis()): AlarmEntity? {
        return alarms
            .filter { it.enabled }
            .mapNotNull { a -> nextTrigger(a, now)?.let { t -> a to t } }
            .minByOrNull { it.second }
            ?.first
    }

    fun nextTrigger(alarm: AlarmEntity, now: Long = System.currentTimeMillis()): Long? {
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        val target = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.repeatDays == 0) {
            if (!target.after(nowCal)) target.add(Calendar.DAY_OF_YEAR, 1)
            return target.timeInMillis
        }

        for (offset in 0..7) {
            val c = (target.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            if (offset == 0 && !c.after(nowCal)) continue

            val dayIndex = when (c.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> -1
            }
            if (dayIndex >= 0 && (alarm.repeatDays and (1 shl dayIndex)) != 0) {
                return c.timeInMillis
            }
        }
        return null
    }

    /** Localized "Alarm in 7 hours, 12 minutes" for the list header. */
    fun formatCountdown(context: Context, fromNow: Long, to: Long): String {
        val delta = (to - fromNow).coerceAtLeast(0)
        val totalMinutes = delta / 60_000L
        val days = (totalMinutes / (60 * 24)).toInt()
        val hours = ((totalMinutes / 60) % 24).toInt()
        val minutes = (totalMinutes % 60).toInt()
        return when {
            days > 0 -> context.resources.getQuantityString(
                R.plurals.countdown_days, days, days, hours
            )
            hours > 0 -> context.getString(R.string.countdown_hours, hours, minutes)
            else -> context.getString(R.string.countdown_minutes, minutes)
        }
    }
}
