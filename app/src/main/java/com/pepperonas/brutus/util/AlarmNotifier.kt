package com.pepperonas.brutus.util

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.MainActivity
import com.pepperonas.brutus.R
import com.pepperonas.brutus.TestAlarmActivity
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.receiver.NotificationActionReceiver
import java.util.Date

/**
 * The quiet notifications around an alarm: the snooze countdown, the heads-up before it rings, and
 * the note that an alarm was missed. None of them can switch a Hardcore alarm off with one tap —
 * that always goes through its challenge.
 */
object AlarmNotifier {

    fun snoozeId(alarmId: Long): Int = 4000 + (alarmId.toInt() and 0xFFFF)
    fun upcomingId(alarmId: Long): Int = 5000 + (alarmId.toInt() and 0xFFFF)
    fun missedId(alarmId: Long): Int = 6000 + (alarmId.toInt() and 0xFFFF)

    private fun nm(context: Context) = context.getSystemService(NotificationManager::class.java)

    private fun time(context: Context, at: Long): String = DateFormat.getTimeFormat(context).format(Date(at))

    private fun openApp(context: Context, requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context, requestCode,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun action(context: Context, action: String, alarmId: Long, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, NotificationActionReceiver::class.java).setAction(action).putExtra("alarm_id", alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Countdown to the snoozed alarm; "Cancel snooze" only when the alarm is not Hardcore. */
    fun postSnooze(context: Context, alarm: AlarmEntity, triggerAt: Long) {
        val builder = Notification.Builder(context, BrutusApplication.CHANNEL_UPCOMING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notification_snooze_title, time(context, triggerAt)))
            .setContentText(alarm.label.ifBlank { context.getString(R.string.app_name) })
            .setWhen(triggerAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(openApp(context, snoozeId(alarm.id)))
        if (!alarm.hardcoreEffective) {
            builder.addAction(
                Notification.Action.Builder(
                    null, context.getString(R.string.notification_snooze_action),
                    action(context, NotificationActionReceiver.ACTION_CANCEL_SNOOZE, alarm.id, snoozeId(alarm.id)),
                ).build()
            )
        }
        nm(context).notify(snoozeId(alarm.id), builder.build())
    }

    fun cancelSnooze(context: Context, alarmId: Long) = nm(context).cancel(snoozeId(alarmId))

    /**
     * "Alarm at 07:00" with a countdown. The action dismisses this one occurrence early; for a
     * Hardcore alarm it opens the alarm's own challenges instead and only skips once they are solved.
     */
    fun postUpcoming(context: Context, alarm: AlarmEntity, triggerAt: Long) {
        val id = upcomingId(alarm.id)
        val act = if (alarm.hardcoreEffective) {
            val intent = TestAlarmActivity.intentFor(context, alarm)
                .putExtra(TestAlarmActivity.EXTRA_EARLY_DISMISS_ALARM_ID, alarm.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Notification.Action.Builder(
                null, context.getString(R.string.notification_upcoming_solve),
                PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
            ).build()
        } else {
            Notification.Action.Builder(
                null, context.getString(R.string.notification_upcoming_skip),
                action(context, NotificationActionReceiver.ACTION_SKIP_NEXT, alarm.id, id),
            ).build()
        }
        val notification = Notification.Builder(context, BrutusApplication.CHANNEL_UPCOMING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notification_upcoming_title, time(context, triggerAt)))
            .setContentText(alarm.label.ifBlank { context.getString(R.string.notification_upcoming_text) })
            .setWhen(triggerAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setTimeoutAfter((triggerAt - System.currentTimeMillis()).coerceAtLeast(1L))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(openApp(context, id))
            .addAction(act)
            .build()
        nm(context).notify(id, notification)
        RingingStore.setUpcomingShown(context, alarm.id, triggerAt)
    }

    fun cancelUpcoming(context: Context, alarmId: Long) {
        nm(context).cancel(upcomingId(alarmId))
        RingingStore.clearUpcomingShown(context, alarmId)
    }

    fun postMissed(context: Context, alarm: AlarmEntity, dueAt: Long) {
        val notification = Notification.Builder(context, BrutusApplication.CHANNEL_MISSED)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notification_missed_title, time(context, dueAt)))
            .setContentText(context.getString(R.string.notification_missed_text))
            .setWhen(dueAt)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(openApp(context, missedId(alarm.id)))
            .build()
        nm(context).notify(missedId(alarm.id), notification)
    }
}
