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
import com.pepperonas.brutus.R
import com.pepperonas.brutus.data.AlarmRepository
import com.pepperonas.brutus.scheduler.AlarmScheduler
import com.pepperonas.brutus.util.AlarmNotifier
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.AlarmSoundGenerator
import com.pepperonas.brutus.util.HardcoreAudioGuard
import com.pepperonas.brutus.util.RingingStore
import com.pepperonas.brutus.util.UltraHardcoreNotifier
import com.pepperonas.brutus.util.UltraHardcoreStore
import com.pepperonas.brutus.widget.NextAlarmWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var hardcoreGuard: HardcoreAudioGuard? = null

    // Current firing context — used to decide what to do on dismiss.
    private var currentAlarmId: Long = -1
    private var currentIsFollowup: Boolean = false
    private var currentFollowupSeq: Int = 0
    private var currentUltraHardcore: Boolean = false

    /** Newest start id — stopSelf(id) then cannot end a session that started in the meantime. */
    private var lastStartId: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        if (intent == null) {
            // Sticky restart after the process was killed while ringing: the session is gone,
            // but the alarm volume may still be forced to maximum. Put it back and go.
            restoreVolume()
            stopSelf(startId)
            return START_NOT_STICKY
        }
        when (intent.action) {
            ACTION_START -> {
                val alarmId = intent.getLongExtra("alarm_id", -1)
                val isFollowup = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_FOLLOWUP, false)
                val followupSeq = intent.getIntExtra(AlarmScheduler.EXTRA_FOLLOWUP_SEQ, 0)
                if (intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false) && alarmId != -1L) {
                    RingingStore.clearSnooze(applicationContext, alarmId)
                }
                if (alarmId != -1L) startAlarm(alarmId, isFollowup, followupSeq)
            }
            ACTION_STOP -> stopAlarm(dismissed = true)
            ACTION_SNOOZE -> {
                val alarmId = intent.getLongExtra("alarm_id", -1)
                if (alarmId != -1L) snoozeAlarm(alarmId)
            }
        }
        return START_STICKY
    }

    private fun startAlarm(alarmId: Long, isFollowup: Boolean, followupSeq: Int) {
        // A second alarm can fire while one is still ringing. Finish the old
        // session first: stop its audio/vibration (otherwise the old MediaPlayer/
        // AudioTrack is orphaned and blares forever) and — if it was an Ultra
        // Hardcore main alarm — arm its follow-ups, since its dismiss path will
        // never run anymore.
        if (currentAlarmId != -1L) {
            finishSessionForTakeover()
        }

        currentAlarmId = alarmId
        currentIsFollowup = isFollowup
        currentFollowupSeq = followupSeq
        currentUltraHardcore = false

        // It rings now: the heads-up and a snooze countdown have done their job.
        AlarmNotifier.cancelUpcoming(applicationContext, alarmId)
        AlarmNotifier.cancelSnooze(applicationContext, alarmId)

        acquireWakeLock()

        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra(AlarmScheduler.EXTRA_IS_FOLLOWUP, isFollowup)
            putExtra(AlarmScheduler.EXTRA_FOLLOWUP_SEQ, followupSeq)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            )
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (isFollowup)
            getString(R.string.notification_realarm_text, followupSeq)
        else getString(R.string.notification_alarm_text)

        val notification = Notification.Builder(this, BrutusApplication.CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(
                getString(
                    if (isFollowup) R.string.notification_realarm_title
                    else R.string.notification_alarm_title
                )
            )
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        setMaxVolume()
        startVibration()

        // Belt and suspenders: the full-screen intent on the notification is the
        // canonical route for foregrounding on Android 10+, but we also call
        // startActivity directly because the foreground service grants us the
        // background-activity-start privilege. If the full-screen intent
        // permission is revoked, this is the only path that still works
        // reliably; if it's granted, the second call is a no-op because the
        // activity is singleInstance.
        try {
            startActivity(activityIntent)
        } catch (e: Exception) {
            // Some OEMs throw on background activity starts even from a foreground
            // service. The full-screen intent above handles those devices.
        }

        // Load alarm + play chosen sound
        CoroutineScope(Dispatchers.IO).launch {
            val app = applicationContext as BrutusApplication
            val repo = AlarmRepository(app.database.alarmDao())
            val alarm = repo.getById(alarmId)
            val sound = AlarmSound.fromId(alarm?.soundId ?: AlarmSound.KLAXON.id)

            launch(Dispatchers.Main) {
                // A takeover may have replaced the session while we were reading
                // the DB — never write into (or play on top of) the new session.
                if (currentAlarmId != alarmId) return@launch
                currentUltraHardcore = alarm?.ultraHardcoreMode == true
                if (alarm?.hardcoreEffective == true) {
                    hardcoreGuard = HardcoreAudioGuard(applicationContext).also { it.attach() }
                }
                playAlarmSound(sound)
            }

            // Follow-ups don't reschedule the main alarm and don't disable repeating
            // alarms — the original main-alarm registration already handled that path.
            if (!isFollowup && alarm != null) {
                if (alarm.repeatDays != 0) {
                    AlarmScheduler.schedule(this@AlarmService, alarm)
                } else {
                    repo.setEnabled(alarm.id, false)
                    RingingStore.clearExpected(applicationContext, alarm.id) // it rang — not missed
                }
                NextAlarmWidget.refresh(applicationContext)
            }
        }
    }

    private fun setMaxVolume() {
        val audioManager = getSystemService(AudioManager::class.java)
        // Persisted: if the process dies while ringing, the next start restores it.
        RingingStore.rememberVolume(applicationContext, audioManager.getStreamVolume(AudioManager.STREAM_ALARM))
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
    }

    private fun restoreVolume() {
        val previous = RingingStore.previousVolume(applicationContext) ?: return
        getSystemService(AudioManager::class.java).setStreamVolume(AudioManager.STREAM_ALARM, previous, 0)
        RingingStore.forgetVolume(applicationContext)
    }

    /**
     * Never lets a playback failure escape: an exception here would end the ringing session.
     * Synthesized sound → system tone → (vibration, which is already running).
     */
    private fun playAlarmSound(sound: AlarmSound) {
        when (sound) {
            AlarmSound.SILENT -> { /* intentionally nothing */ }
            AlarmSound.SYSTEM -> playSystemAlarm()
            else -> try {
                playSynthesized(sound)
            } catch (e: Exception) {
                audioTrack?.release()
                audioTrack = null
                playSystemAlarm(fallbackToSynth = false)
            }
        }
    }

    /**
     * The system tone can be missing (no default set, file on removed storage, no permission) and
     * MediaPlayer throws then — an alarm that throws instead of ringing is the worst outcome, so it
     * falls back to a synthesized sound, which needs nothing from the device.
     */
    private fun playSystemAlarm(fallbackToSynth: Boolean = true) {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val player = MediaPlayer()
        try {
            checkNotNull(alarmUri) { "no system tone" }
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(this, alarmUri)
            player.isLooping = true
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (e: Exception) {
            player.release()
            if (fallbackToSynth) {
                try {
                    playSynthesized(AlarmSound.KLAXON)
                } catch (_: Exception) {
                    audioTrack?.release()
                    audioTrack = null // vibration still runs
                }
            }
        }
    }

    private fun playSynthesized(sound: AlarmSound) {
        val pcm = AlarmSoundGenerator.generatePcm(sound)
        if (pcm.isEmpty()) {
            if (sound != AlarmSound.KLAXON) playSynthesized(AlarmSound.KLAXON)
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

    /**
     * Snoozing is not dismissing: it must not arm the Ultra Hardcore follow-ups, and snoozing a
     * follow-up keeps it a follow-up.
     */
    private fun snoozeAlarm(alarmId: Long) {
        val isFollowup = currentAlarmId == alarmId && currentIsFollowup
        val seq = if (isFollowup) currentFollowupSeq else 0
        stopAlarm(dismissed = false) {
            val app = applicationContext as BrutusApplication
            val alarm = AlarmRepository(app.database.alarmDao()).getById(alarmId)
            if (alarm != null) AlarmScheduler.scheduleSnooze(this@AlarmService, alarm, isFollowup, seq)
            NextAlarmWidget.refresh(applicationContext)
        }
    }

    /** Stops audio/vibration and releases guard, volume override and wake lock. */
    private fun cleanupPlayback() {
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
        hardcoreGuard?.detach()
        hardcoreGuard = null
        restoreVolume()
        releaseWakeLock()
    }

    /**
     * Called when a new alarm fires while another is still ringing. The outgoing
     * alarm's dismiss path will never run, so its Ultra Hardcore follow-ups must
     * be armed here — otherwise a UHC alarm could be defeated simply by letting a
     * second alarm land on top of it.
     */
    private fun finishSessionForTakeover() {
        val alarmIdSnap = currentAlarmId
        val ultraSnap = currentUltraHardcore
        val isFollowupSnap = currentIsFollowup

        cleanupPlayback()

        if (alarmIdSnap != -1L && !isFollowupSnap) {
            CoroutineScope(Dispatchers.IO).launch {
                if (ultraSnap || isUltraHardcore(alarmIdSnap)) armUltraHardcoreFollowups(alarmIdSnap)
            }
        }
    }

    /**
     * Ends the ringing session. [dismissed] is true only when the user completed the challenges —
     * that alone arms the Ultra Hardcore follow-ups of a main alarm. [then] runs (on IO) before
     * the service stops, so the process cannot be reaped with a snooze or a follow-up half-written.
     */
    private fun stopAlarm(dismissed: Boolean, then: suspend () -> Unit = {}) {
        val alarmIdSnap = currentAlarmId
        val ultraSnap = currentUltraHardcore
        val isFollowupSnap = currentIsFollowup
        val followupSeqSnap = currentFollowupSeq
        val startIdSnap = lastStartId

        cleanupPlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)

        currentAlarmId = -1
        currentIsFollowup = false
        currentFollowupSeq = 0
        currentUltraHardcore = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // A dismiss before the sound has loaded must still arm the follow-ups.
                val ultra = ultraSnap || (alarmIdSnap != -1L && isUltraHardcore(alarmIdSnap))
                if (dismissed && ultra && alarmIdSnap != -1L) {
                    if (!isFollowupSnap) {
                        armUltraHardcoreFollowups(alarmIdSnap)
                    } else {
                        // A dismissed follow-up is done; the last one clears the reminder.
                        UltraHardcoreStore.clearFollowup(applicationContext, alarmIdSnap, followupSeqSnap)
                        if (UltraHardcoreStore.listPending(applicationContext).none { it.alarmId == alarmIdSnap }) {
                            UltraHardcoreNotifier.cancel(applicationContext, alarmIdSnap)
                            UltraHardcoreStore.clearAllFor(applicationContext, alarmIdSnap)
                        } else {
                            UltraHardcoreNotifier.post(applicationContext, alarmIdSnap) // countdown → next one
                        }
                    }
                }
                then()
            } finally {
                withContext(Dispatchers.Main) { stopSelf(startIdSnap) }
            }
        }
    }

    /** The in-memory flag is only set once the sound has loaded; this is the answer before that. */
    private suspend fun isUltraHardcore(alarmId: Long): Boolean =
        AlarmRepository((applicationContext as BrutusApplication).database.alarmDao())
            .getById(alarmId)?.ultraHardcoreMode == true

    private suspend fun armUltraHardcoreFollowups(alarmId: Long) {
        val app = applicationContext as BrutusApplication
        val alarm = AlarmRepository(app.database.alarmDao()).getById(alarmId) ?: return
        val now = System.currentTimeMillis()

        AlarmScheduler.ULTRA_HARDCORE_FOLLOWUP_OFFSETS_MIN.forEachIndexed { idx, offsetMin ->
            val seq = idx + 1
            val triggerAt = now + offsetMin * 60_000L
            AlarmScheduler.scheduleFollowup(this, alarm, seq, triggerAt)
            UltraHardcoreStore.recordFollowup(applicationContext, alarmId, seq, triggerAt)
        }
        UltraHardcoreStore.setStepTarget(applicationContext, alarmId, UltraHardcoreStore.DEFAULT_STEP_TARGET)
        UltraHardcoreNotifier.post(applicationContext, alarmId)
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
        // Normally the session already ended (currentAlarmId == -1) and this is a no-op. If the
        // system tears the service down while an Ultra Hardcore main alarm rings, arm its
        // follow-ups — being killed must not be a way out.
        if (currentAlarmId != -1L) {
            val id = currentAlarmId
            val arm = currentUltraHardcore && !currentIsFollowup
            cleanupPlayback()
            currentAlarmId = -1
            if (arm) CoroutineScope(Dispatchers.IO).launch { armUltraHardcoreFollowups(id) }
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.pepperonas.brutus.START_ALARM"
        const val ACTION_STOP = "com.pepperonas.brutus.STOP_ALARM"
        const val ACTION_SNOOZE = "com.pepperonas.brutus.SNOOZE_ALARM"
        const val NOTIFICATION_ID = 1001

        fun notificationIdForUltraHardcore(alarmId: Long): Int =
            UltraHardcoreNotifier.notificationId(alarmId)
    }
}
