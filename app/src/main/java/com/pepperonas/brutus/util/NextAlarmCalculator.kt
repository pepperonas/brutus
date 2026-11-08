package com.pepperonas.brutus.util

import android.content.Context
import com.pepperonas.brutus.R
import com.pepperonas.brutus.data.AlarmEntity
import java.util.Calendar

object NextAlarmCalculator {

    /**
     * Returns the alarm that rings soonest, or null if none. [skips] maps alarm id → an occurrence
     * the user dismissed early; that occurrence does not count as "next".
     */
    fun findNext(
        alarms: List<AlarmEntity>,
        now: Long = System.currentTimeMillis(),
        skips: Map<Long, Long> = emptyMap(),
        pausedUntil: Long? = null,
    ): AlarmEntity? {
        return alarms
            .filter { it.enabled }
            .mapNotNull { a -> nextTrigger(a, now, skips[a.id], pausedUntil)?.let { t -> a to t } }
            .minByOrNull { it.second }
            ?.first
    }

    /**
     * Next occurrence after [now], stepping past [skipped] if it is exactly that occurrence, and
     * not before [pausedUntil] (vacation mode) — the first occurrence at or after it.
     */
    fun nextTrigger(
        alarm: AlarmEntity,
        now: Long = System.currentTimeMillis(),
        skipped: Long?,
        pausedUntil: Long? = null,
    ): Long? {
        // nextTrigger(x) is strictly after x; one ms earlier lets an alarm at exactly pausedUntil count.
        val from = if (pausedUntil != null && pausedUntil > now) pausedUntil - 1 else now
        val next = nextTrigger(alarm, from) ?: return null
        return if (skipped != null && next == skipped) nextTrigger(alarm, skipped) else next
    }

    /** Next trigger as the whole app sees it: skips and vacation mode included. */
    fun upcoming(context: Context, alarm: AlarmEntity, now: Long = System.currentTimeMillis()): Long? =
        nextTrigger(alarm, now, RingingStore.skips(context)[alarm.id], VacationStore.pausedUntil(context, now))

    /** The alarm that rings soonest as the whole app sees it: skips and vacation mode included. */
    fun findNext(context: Context, alarms: List<AlarmEntity>, now: Long = System.currentTimeMillis()): AlarmEntity? =
        findNext(alarms, now, RingingStore.skips(context), VacationStore.pausedUntil(context, now))

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
            hours > 0 -> context.getString(
                R.string.countdown_hours,
                context.resources.getQuantityString(R.plurals.countdown_unit_hours, hours, hours),
                context.resources.getQuantityString(R.plurals.countdown_unit_minutes, minutes, minutes),
            )
            else -> context.getString(
                R.string.countdown_minutes,
                context.resources.getQuantityString(R.plurals.countdown_unit_minutes, minutes, minutes),
            )
        }
    }
}
