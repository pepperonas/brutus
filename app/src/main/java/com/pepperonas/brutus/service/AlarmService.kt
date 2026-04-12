package com.pepperonas.brutus.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.pepperonas.brutus.AlarmActivity
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.data.AlarmRepository
import com.pepperonas.brutus.scheduler.AlarmScheduler
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.AlarmSoundGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var previousVolume: Int = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val alarmId = intent.getLongExtra("alarm_id", -1)
                if (alarmId != -1L) startAlarm(alarmId)
            }
            ACTION_STOP -> stopAlarm()
            ACTION_SNOOZE -> {
                val alarmId = intent.getLongExtra("alarm_id", -1)
                if (alarmId != -1L) snoozeAlarm(alarmId)
            }
        }
        return START_STICKY
    }

    private fun startAlarm(alarmId: Long) {
        acquireWakeLock()

        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("alarm_id", alarmId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, BrutusApplication.CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Brutus Alarm")
            .setContentText("Alarm aktiv! Aufstehen!")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        setMaxVolume()
        startVibration()
        startActivity(activityIntent)

        // Load alarm + play chosen sound
        CoroutineScope(Dispatchers.IO).launch {
            val app = applicationContext as BrutusApplication
            val repo = AlarmRepository(app.database.alarmDao())
            val alarm = repo.getById(alarmId)
            val sound = AlarmSound.fromId(alarm?.soundId ?: AlarmSound.KLAXON.id)

            launch(Dispatchers.Main) { playAlarmSound(sound) }

            // Reschedule if repeating
            if (alarm != null && alarm.repeatDays != 0) {
                AlarmScheduler.schedule(this@AlarmService, alarm)
            } else if (alarm != null) {
                repo.setEnabled(alarm.id, false)
            }
        }
    }

    private fun setMaxVolume() {
        val audioManager = getSystemService(AudioManager::class.java)
        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
    }

    private fun restoreVolume() {
        if (previousVolume >= 0) {
            val audioManager = getSystemService(AudioManager::class.java)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0)
        }
    }

    private fun playAlarmSound(sound: AlarmSound) {
        if (sound == AlarmSound.SYSTEM) {
            playSystemAlarm()
        } else {
            playSynthesized(sound)
        }
    }

    private fun playSystemAlarm() {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(this@AlarmService, alarmUri)
            isLooping = true
            prepare()
            start()
        }
    }

    private fun playSynthesized(sound: AlarmSound) {
        val pcm = AlarmSoundGenerator.generatePcm(sound)
        if (pcm.isEmpty()) {
            playSystemAlarm()
            return
        }
        val bufferBytes = pcm.size * 2

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(AlarmSoundGenerator.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcm, 0, pcm.size)
        track.setLoopPoints(0, pcm.size, -1)
        track.play()
        audioTrack = track
    }

    private fun startVibration() {
        vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 500, 200, 500, 200, 1000)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun snoozeAlarm(alarmId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val app = applicationContext as BrutusApplication
            val repo = AlarmRepository(app.database.alarmDao())
            val alarm = repo.getById(alarmId)
            if (alarm != null) {
                AlarmScheduler.scheduleSnooze(this@AlarmService, alarm)
            }
        }
        stopAlarm()
    }

    private fun stopAlarm() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: IllegalStateException) { }
            it.release()
        }
        mediaPlayer = null

        audioTrack?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) { }
            it.release()
        }
        audioTrack = null

        vibrator?.cancel()
        restoreVolume()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "brutus:alarm_wakelock"
        ).apply {
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.pepperonas.brutus.START_ALARM"
        const val ACTION_STOP = "com.pepperonas.brutus.STOP_ALARM"
        const val ACTION_SNOOZE = "com.pepperonas.brutus.SNOOZE_ALARM"
        const val NOTIFICATION_ID = 1001
    }
}
