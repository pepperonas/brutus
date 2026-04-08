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

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(alarmChannel)
        nm.createNotificationChannel(serviceChannel)
    }

    companion object {
        const val CHANNEL_ALARM = "brutus_alarm"
        const val CHANNEL_SERVICE = "brutus_service"
    }
}
