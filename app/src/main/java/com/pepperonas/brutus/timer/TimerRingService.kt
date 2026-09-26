package com.pepperonas.brutus.timer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.R
import com.pepperonas.brutus.util.SoundPreviewPlayer
import com.pepperonas.brutus.util.TimerSoundStore

/**
 * Rings the finished timer until "Stop" — in the app or on the notification — like a kitchen
 * timer, not like a Brutus alarm: the chosen gentle tone, no volume lock, no challenge.
 */
class TimerRingService : Service() {

    private var player: SoundPreviewPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val stop = PendingIntent.getBroadcast(
            this, 0x71000001,
            Intent(this, TimerReceiver::class.java).setAction(TimerReceiver.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, BrutusApplication.CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.notification_timer_done))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(TimerController.openApp(this))
            .addAction(Notification.Action.Builder(null, getString(R.string.timer_stop), stop).build())
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(TimerController.NOTIFICATION_ID + 1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(TimerController.NOTIFICATION_ID + 1, notification)
        }
        if (player == null) {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "brutus:timer")
                .apply { acquire(10 * 60 * 1000L) }
            player = SoundPreviewPlayer(this).also { it.play(TimerSoundStore.get(this)) }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        player?.stop()
        player = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }
}
