package com.pepperonas.brutus.util

import android.content.Context

/**
 * Persists the pending Ultra Hardcore follow-up alarms across reboots.
 *
 * After the main alarm is dismissed, two follow-up alarms are scheduled (T+10 min and T+15 min).
 * AlarmManager registrations don't survive reboot, so we mirror the schedule here and let
 * BootReceiver re-register any follow-up whose trigger time is still in the future.
 *
 * Storage format (one key per pending follow-up):
 *   uhc_followup_{alarmId}_{seq} = triggerTimeMillis
 * The matching anti-snooze task target step count is stored at:
 *   uhc_step_target_{alarmId} = stepCount (default 30)
 *
 * When a follow-up fires or the anti-snooze task is completed, the entries are cleared.
 */
object UltraHardcoreStore {

    private const val PREFS = "brutus_ultra_hardcore"
    private const val KEY_FOLLOWUP_PREFIX = "uhc_followup_"
    private const val KEY_STEP_TARGET_PREFIX = "uhc_step_target_"
    private const val KEY_BASE_STEPS_PREFIX = "uhc_base_steps_"

    /** Anti-snooze step challenge target. Walking ~30 steps from bed is enough to wake up. */
    const val DEFAULT_STEP_TARGET = 30

    data class Pending(val alarmId: Long, val seq: Int, val triggerAt: Long)

    fun recordFollowup(context: Context, alarmId: Long, seq: Int, triggerAt: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putLong("$KEY_FOLLOWUP_PREFIX${alarmId}_$seq", triggerAt).apply()
    }

    fun clearFollowup(context: Context, alarmId: Long, seq: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove("$KEY_FOLLOWUP_PREFIX${alarmId}_$seq").apply()
    }

    fun clearAllFor(context: Context, alarmId: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        prefs.all.keys.toList()
            .filter { it.startsWith("$KEY_FOLLOWUP_PREFIX${alarmId}_") }
            .forEach { editor.remove(it) }
        editor.remove("$KEY_STEP_TARGET_PREFIX$alarmId")
        editor.remove("$KEY_BASE_STEPS_PREFIX$alarmId")
        editor.apply()
    }

    fun listPending(context: Context): List<Pending> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.all.entries
            .mapNotNull { (key, value) ->
                if (!key.startsWith(KEY_FOLLOWUP_PREFIX)) return@mapNotNull null
                val suffix = key.removePrefix(KEY_FOLLOWUP_PREFIX)
                val parts = suffix.split("_")
                if (parts.size != 2) return@mapNotNull null
                val alarmId = parts[0].toLongOrNull() ?: return@mapNotNull null
                val seq = parts[1].toIntOrNull() ?: return@mapNotNull null
                val triggerAt = (value as? Long) ?: return@mapNotNull null
                Pending(alarmId, seq, triggerAt)
            }
    }

    /** Returns the set of alarm IDs that still have at least one pending follow-up. */
    fun pendingAlarmIds(context: Context): Set<Long> =
        listPending(context).map { it.alarmId }.toSet()

    fun setStepTarget(context: Context, alarmId: Long, target: Int) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt("$KEY_STEP_TARGET_PREFIX$alarmId", target).apply()
    }

    fun stepTarget(context: Context, alarmId: Long): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt("$KEY_STEP_TARGET_PREFIX$alarmId", DEFAULT_STEP_TARGET)
    }

    /**
     * Captures the cumulative step counter at the moment Ultra Hardcore is armed,
     * so the task screen can show progress as a delta even if reopened later.
     */
    fun setBaselineSteps(context: Context, alarmId: Long, baseline: Float) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putFloat("$KEY_BASE_STEPS_PREFIX$alarmId", baseline).apply()
    }

    fun baselineSteps(context: Context, alarmId: Long): Float {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getFloat("$KEY_BASE_STEPS_PREFIX$alarmId", -1f)
    }
}
