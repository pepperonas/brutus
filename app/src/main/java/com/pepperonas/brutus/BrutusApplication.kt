package com.pepperonas.brutus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.pepperonas.brutus.data.AlarmDatabase
import com.pepperonas.brutus.update.UpdateScheduler
import com.pepperonas.brutus.util.Storage

class BrutusApplication : Application() {

    val database: AlarmDatabase by lazy { AlarmDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        // Before anything touches the database or a preference file.
        Storage.migrateIfNeeded(this)
        createNotificationChannels()
        // WorkManager lives in credential storage and is unavailable before the first unlock;
        // BOOT_COMPLETED restarts the process once the user has unlocked.
        if (Storage.isUserUnlocked(this)) UpdateScheduler.ensureScheduled(this)
    }

    private fun createNotificationChannels() {
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            getString(R.string.channel_alarm_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_alarm_description)
            setBypassDnd(true)
            setSound(null, null)
        }

        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            getString(R.string.channel_service_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_service_description)
        }

        val ultraHardcoreChannel = NotificationChannel(
            CHANNEL_ULTRA_HARDCORE,
            getString(R.string.channel_ultra_hardcore_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_ultra_hardcore_description)
            setBypassDnd(true)
            setSound(null, null)
            enableVibration(false)
        }

        // An ordinary notice: default importance, never through Do Not Disturb.
        val updatesChannel = NotificationChannel(
            CHANNEL_UPDATES,
            getString(R.string.channel_updates_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.channel_updates_description)
        }

        // Quiet: the heads-up before an alarm and the snooze countdown must not wake anyone.
        val upcomingChannel = NotificationChannel(
            CHANNEL_UPCOMING, getString(R.string.channel_upcoming_name), NotificationManager.IMPORTANCE_LOW
        ).apply { description = getString(R.string.channel_upcoming_description) }
        val missedChannel = NotificationChannel(
            CHANNEL_MISSED, getString(R.string.channel_missed_name), NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = getString(R.string.channel_missed_description) }
        val timerChannel = NotificationChannel(
            CHANNEL_TIMER, getString(R.string.channel_timer_name), NotificationManager.IMPORTANCE_LOW
        ).apply { description = getString(R.string.channel_timer_description) }

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(alarmChannel)
        nm.createNotificationChannel(serviceChannel)
        nm.createNotificationChannel(ultraHardcoreChannel)
        nm.createNotificationChannel(updatesChannel)
        nm.createNotificationChannel(upcomingChannel)
        nm.createNotificationChannel(missedChannel)
        nm.createNotificationChannel(timerChannel)
    }

    companion object {
        const val CHANNEL_ALARM = "brutus_alarm"
        const val CHANNEL_SERVICE = "brutus_service"
        const val CHANNEL_ULTRA_HARDCORE = "brutus_ultra_hardcore"
        const val CHANNEL_UPDATES = "brutus_updates"
        const val CHANNEL_UPCOMING = "brutus_upcoming"
        const val CHANNEL_MISSED = "brutus_missed"
        const val CHANNEL_TIMER = "brutus_timer"
    }
}
