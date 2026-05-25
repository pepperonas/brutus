package com.pepperonas.brutus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.pepperonas.brutus.data.AlarmDatabase

class BrutusApplication : Application() {

    val database: AlarmDatabase by lazy { AlarmDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            "Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Active alarm notifications"
            setBypassDnd(true)
            setSound(null, null)
        }

        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "Alarm Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background alarm service"
        }

        val ultraHardcoreChannel = NotificationChannel(
            CHANNEL_ULTRA_HARDCORE,
            "Ultra Hardcore Reminder",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Persistent reminder while two follow-up alarms are armed"
            setBypassDnd(true)
            setSound(null, null)
            enableVibration(false)
        }

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(alarmChannel)
        nm.createNotificationChannel(serviceChannel)
        nm.createNotificationChannel(ultraHardcoreChannel)
    }

    companion object {
        const val CHANNEL_ALARM = "brutus_alarm"
        const val CHANNEL_SERVICE = "brutus_service"
        const val CHANNEL_ULTRA_HARDCORE = "brutus_ultra_hardcore"
    }
}
