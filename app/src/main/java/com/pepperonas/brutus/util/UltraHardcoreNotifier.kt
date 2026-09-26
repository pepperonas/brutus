package com.pepperonas.brutus.util

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.R
import com.pepperonas.brutus.UltraHardcoreTaskActivity

/**
 * The persistent "Ultra Hardcore active" reminder. The anti-snooze task is reachable from it,
 * so it has to come back after a reboot (BootReceiver) and has to go when the alarm is switched
 * off or deleted (AlarmViewModel) — not only from inside the service.
 */
object UltraHardcoreNotifier {

    fun notificationId(alarmId: Long): Int = 2000 + (alarmId.toInt() and 0xFFFF)

    fun taskIntent(context: Context, alarmId: Long): Intent =
        Intent(context, UltraHardcoreTaskActivity::class.java).apply {
            putExtra(UltraHardcoreTaskActivity.EXTRA_ALARM_ID, alarmId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun post(context: Context, alarmId: Long) {
        val taskPi = PendingIntent.getActivity(
            context,
            (0x55_00_00_00 or (alarmId.toInt() and 0xFFFFFF)),
            taskIntent(context, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, BrutusApplication.CHANNEL_ULTRA_HARDCORE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notification_uhc_title))
            .setContentText(context.getString(R.string.notification_uhc_text))
            .setStyle(Notification.BigTextStyle().bigText(context.getString(R.string.notification_uhc_big_text)))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(taskPi)
            .addAction(
                Notification.Action.Builder(null, context.getString(R.string.notification_uhc_action), taskPi).build()
            )
            .build()
        context.getSystemService(NotificationManager::class.java).notify(notificationId(alarmId), notification)
    }

    fun cancel(context: Context, alarmId: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(notificationId(alarmId))
    }
}
