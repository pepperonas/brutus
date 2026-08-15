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
