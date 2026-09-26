package com.pepperonas.brutus.timer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.MainActivity
import com.pepperonas.brutus.R
import com.pepperonas.brutus.ui.screens.formatCountdown
import com.pepperonas.brutus.util.Storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * The kitchen timer as a real alarm. Until v2.4.0 the countdown lived only in a ViewModel: leaving
 * the app with Back ended it silently, and in the background it rang late or never. Now the state is
 * persisted here, AlarmManager wakes the phone at the end ([TimerReceiver]), [TimerRingService]
 * rings, and an ongoing notification shows the countdown with Pause/Resume/Cancel.
 *
 * Times are elapsedRealtime (immune to clock changes). They mean nothing after a reboot, so the
 * boot count is stored too and a timer from before the reboot reads as idle.
 */
object TimerController {

    enum class Phase { IDLE, RUNNING, PAUSED, FINISHED }

    data class Snapshot(val phase: Phase, val endAt: Long = 0L, val remaining: Long = 0L)

    private const val PREFS = "brutus_timer_state"
    private const val KEY_PHASE = "phase"
    private const val KEY_END_AT = "end_at"
    private const val KEY_REMAINING = "remaining"
    private const val KEY_BOOT = "boot_count"

    const val NOTIFICATION_ID = 7001
    const val REQUEST_FIRE = 0x71000000

    private fun prefs(context: Context) = Storage.prefs(context, PREFS)

    private fun bootCount(context: Context): Int =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    fun snapshot(context: Context): Snapshot {
        val p = prefs(context)
        val phase = Phase.entries.firstOrNull { it.name == p.getString(KEY_PHASE, null) } ?: Phase.IDLE
        if (phase == Phase.IDLE) return Snapshot(Phase.IDLE)
        if (p.getInt(KEY_BOOT, -1) != bootCount(context)) return Snapshot(Phase.IDLE)
        return Snapshot(phase, p.getLong(KEY_END_AT, 0L), p.getLong(KEY_REMAINING, 0L))
    }

    private fun save(context: Context, s: Snapshot) {
        prefs(context).edit()
            .putString(KEY_PHASE, s.phase.name)
            .putLong(KEY_END_AT, s.endAt)
            .putLong(KEY_REMAINING, s.remaining)
            .putInt(KEY_BOOT, bootCount(context))
            .commit()
    }

    fun start(context: Context, totalMs: Long, now: Long = SystemClock.elapsedRealtime()) {
        val s = Snapshot(Phase.RUNNING, endAt = now + totalMs, remaining = totalMs)
        save(context, s)
        arm(context, s.endAt)
        notifyState(context, s, now)
    }

    fun pause(context: Context, now: Long = SystemClock.elapsedRealtime()) {
        val cur = snapshot(context)
        if (cur.phase != Phase.RUNNING) return
        paused(context, (cur.endAt - now).coerceAtLeast(0L), now)
    }

    /** Puts the timer into PAUSED with [remaining] left (also the undo path of a cancelled pause). */
    fun paused(context: Context, remaining: Long, now: Long = SystemClock.elapsedRealtime()) {
        val s = Snapshot(Phase.PAUSED, remaining = remaining)
        save(context, s)
        disarm(context)
        notifyState(context, s, now)
    }

    fun resume(context: Context, now: Long = SystemClock.elapsedRealtime()) {
        val cur = snapshot(context)
        if (cur.phase != Phase.PAUSED) return
        start(context, cur.remaining, now)
    }

    /** Stops everything — countdown, alarm registration, notification and a ringing finish tone. */
    fun cancel(context: Context) {
        save(context, Snapshot(Phase.IDLE))
        disarm(context)
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        context.stopService(Intent(context, TimerRingService::class.java))
    }

    /** The end was reached — by AlarmManager or by the screen's own ticker. Idempotent. */
    fun fired(context: Context) {
        val cur = snapshot(context)
        if (cur.phase != Phase.RUNNING) return
        save(context, Snapshot(Phase.FINISHED))
        disarm(context)
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        context.startForegroundService(Intent(context, TimerRingService::class.java))
    }

    fun changes(context: Context): Flow<Int> = callbackFlow {
        val p = prefs(context)
        var n = 0
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(++n) }
        p.registerOnSharedPreferenceChangeListener(listener)
        trySend(n)
        awaitClose { p.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // ---- AlarmManager ---------------------------------------------------------------------------

    private fun firePendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_FIRE,
        Intent(context, TimerReceiver::class.java).setAction(TimerReceiver.ACTION_FIRE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun arm(context: Context, endAt: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = firePendingIntent(context)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, endAt, pi)
        else am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, endAt, pi)
    }

    private fun disarm(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(firePendingIntent(context))
    }

    // ---- the ongoing notification ---------------------------------------------------------------

    private fun action(context: Context, action: String, label: Int): Notification.Action =
        Notification.Action.Builder(
            null, context.getString(label),
            PendingIntent.getBroadcast(
                context, action.hashCode(),
                Intent(context, TimerReceiver::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        ).build()

    internal fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, NOTIFICATION_ID,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notifyState(context: Context, s: Snapshot, now: Long) {
        val builder = Notification.Builder(context, BrutusApplication.CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STOPWATCH)
            .setContentIntent(openApp(context))
        when (s.phase) {
            Phase.RUNNING -> builder
                .setContentTitle(context.getString(R.string.notification_timer_running))
                // The chronometer counts in wall-clock time; derive the wall-clock end from now.
                .setWhen(System.currentTimeMillis() + (s.endAt - now))
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(action(context, TimerReceiver.ACTION_PAUSE, R.string.timer_pause))
                .addAction(action(context, TimerReceiver.ACTION_CANCEL, R.string.timer_abort))
            Phase.PAUSED -> builder
                .setContentTitle(context.getString(R.string.notification_timer_paused, formatCountdown(s.remaining)))
                .setShowWhen(false)
                .addAction(action(context, TimerReceiver.ACTION_RESUME, R.string.timer_resume))
                .addAction(action(context, TimerReceiver.ACTION_CANCEL, R.string.timer_abort))
            else -> return
        }
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, builder.build())
    }
}
