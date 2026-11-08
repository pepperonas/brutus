package com.pepperonas.brutus.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
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
import com.pepperonas.brutus.util.AppSettings
import com.pepperonas.brutus.util.HardcoreAudioGuard
import com.pepperonas.brutus.util.RingPolicy
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

    /** Whether the alarm screen is in front — then the quiet notification carries the service. */
    private var screenShown = false

    // Session dynamics (see RingPolicy): crescendo, quieting for calls, giving up when unanswered.
    private val handler = Handler(Looper.getMainLooper())
    private var focusRequest: AudioFocusRequest? = null
    private var ringStartedAt = 0L          // elapsedRealtime when the sound started
    private var ringStartedWallClock = 0L   // for the "unanswered" notice
    private var crescendoSeconds = 0
    private var ringLimitMinutes = 0
    private var currentHardcore = false
    private var lastGain = -1f
    private val ticker = object : Runnable {
        override fun run() {
            if (currentAlarmId == -1L) return
            onTick()
            if (currentAlarmId != -1L) handler.postDelayed(this, RingPolicy.TICK_MS)
        }
    }

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
            ACTION_STOP -> {
                if (currentAlarmId != -1L) RingingStore.clearAutoSnoozes(applicationContext, currentAlarmId)
                stopAlarm(dismissed = true)
            }
            ACTION_SCREEN_SHOWN, ACTION_SCREEN_HIDDEN -> {
                if (currentAlarmId == -1L) {
                    // Only the screen reporting in, no session: don't linger as a started service.
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                onAlarmScreenShown(intent.action == ACTION_SCREEN_SHOWN)
            }
            ACTION_SNOOZE -> {
                val alarmId = intent.getLongExtra("alarm_id", -1)
                if (alarmId != -1L) {
                    // Someone reacted: the count of unanswered rounds starts over.
                    RingingStore.clearAutoSnoozes(applicationContext, alarmId)
                    snoozeAlarm(alarmId)
                }
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

        val activityIntent = alarmActivityIntent(alarmId, isFollowup, followupSeq)
        startForeground(NOTIFICATION_ID, ringingNotification(fullScreen = true))
        screenShown = false

        setMaxVolume()
        startVibration()

        // Belt and suspenders: the full-screen intent is the canonical route to the alarm
        // screen. This direct start helps where it is still allowed — while Brutus itself is
        // in front, and on older Android versions. Since Android 14 a foreground service does
        // NOT grant background activity starts (logcat: "Background activity launch blocked",
        // BAL_BLOCK): with the phone unlocked and another app in use, Android shows the
        // full-screen notification as a pinned heads-up instead, and a tap opens the screen.
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
                // A follow-up belongs to an Ultra Hardcore alarm — it never softens or gives up.
                currentHardcore = alarm?.hardcoreEffective == true || isFollowup
                if (alarm?.hardcoreEffective == true) {
                    hardcoreGuard = HardcoreAudioGuard(applicationContext).also { it.attach() }
                }
                crescendoSeconds = AppSettings.crescendoSeconds(applicationContext)
                ringLimitMinutes = AppSettings.ringLimitMinutes(applicationContext)
                ringStartedAt = SystemClock.elapsedRealtime()
                ringStartedWallClock = System.currentTimeMillis()
                lastGain = -1f
                requestAudioFocus()
                playAlarmSound(sound)
                applyGain(currentGain())
                handler.removeCallbacks(ticker)
                handler.postDelayed(ticker, RingPolicy.TICK_MS)
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

    private fun alarmActivityIntent(alarmId: Long, isFollowup: Boolean, followupSeq: Int) =
        Intent(this, AlarmActivity::class.java).apply {
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

    /**
     * The ringing session's notification. [fullScreen] = the loud one on the alarm channel with a
     * full-screen intent: it wakes a locked phone into the alarm screen. On an unlocked phone Android
     * shows it as a *pinned* heads-up instead — right over the alarm screen's clock. So while that
     * screen is in front, the service switches to a quiet one on the low service channel (no heads-up),
     * and back as soon as the screen leaves, so the alarm is never out of sight.
     */
    private fun ringingNotification(fullScreen: Boolean): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, alarmActivityIntent(currentAlarmId, currentIsFollowup, currentFollowupSeq),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = Notification.Builder(
            this, if (fullScreen) BrutusApplication.CHANNEL_ALARM else BrutusApplication.CHANNEL_SERVICE
        )
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(
                getString(
                    if (currentIsFollowup) R.string.notification_realarm_title
                    else R.string.notification_alarm_title
                )
            )
            .setContentText(
                if (currentIsFollowup) getString(R.string.notification_realarm_text, currentFollowupSeq)
                else getString(R.string.notification_alarm_text)
            )
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
        if (fullScreen) builder.setFullScreenIntent(pendingIntent, true)
        return builder.build()
    }

    /** The alarm screen came to the front ([shown]) or left it; swap the notifications accordingly. */
    private fun onAlarmScreenShown(shown: Boolean) {
        if (currentAlarmId == -1L || shown == screenShown) return
        screenShown = shown
        val nm = getSystemService(NotificationManager::class.java)
        if (shown) {
            startForeground(NOTIFICATION_ID_QUIET, ringingNotification(fullScreen = false))
            nm.cancel(NOTIFICATION_ID)
        } else {
            startForeground(NOTIFICATION_ID, ringingNotification(fullScreen = true))
            nm.cancel(NOTIFICATION_ID_QUIET)
        }
    }

    private fun elapsed(): Long = SystemClock.elapsedRealtime() - ringStartedAt

    private fun currentGain(): Float = RingPolicy.gain(
        elapsedMs = elapsed(),
        rampSeconds = crescendoSeconds,
        hardcore = currentHardcore,
        inCall = RingPolicy.isInCall(getSystemService(AudioManager::class.java).mode),
    )

    private fun onTick() {
        applyGain(currentGain())
        if (RingPolicy.ringLimitReached(elapsed(), ringLimitMinutes, currentHardcore)) onRingLimit()
    }

    /** Sets the player gain; the stream itself stays at maximum (and Hardcore keeps it there). */
    private fun applyGain(gain: Float) {
        if (gain == lastGain) return
        lastGain = gain
        try {
            audioTrack?.setVolume(gain)
            mediaPlayer?.setVolume(gain, gain)
        } catch (_: IllegalStateException) { }
    }

    /** Nobody reacted within the limit: snooze on our own, or after enough rounds give up. */
    private fun onRingLimit() {
        val alarmId = currentAlarmId
        val wallClock = ringStartedWallClock
        when (RingPolicy.afterLimit(RingingStore.autoSnoozes(applicationContext, alarmId))) {
            RingPolicy.LimitOutcome.SNOOZE -> {
                RingingStore.recordAutoSnooze(applicationContext, alarmId)
                snoozeAlarm(alarmId, minMinutes = RingPolicy.AUTO_SNOOZE_MIN_MINUTES)
            }
            RingPolicy.LimitOutcome.GIVE_UP -> {
                RingingStore.clearAutoSnoozes(applicationContext, alarmId)
                stopAlarm(dismissed = false) {
                    val app = applicationContext as BrutusApplication
                    val alarm = AlarmRepository(app.database.alarmDao()).getById(alarmId)
                    if (alarm != null) AlarmNotifier.postMissed(applicationContext, alarm, wallClock, unanswered = true)
                }
            }
        }
    }

    /**
     * Asks other audio (music, podcasts, navigation) to step aside while the alarm rings. A refusal —
     * e.g. during a call — changes nothing: the alarm plays anyway, quieter while the call lasts.
     */
    private fun requestAudioFocus() {
        abandonAudioFocus()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setOnAudioFocusChangeListener({ /* call state is read from the audio mode each tick */ }, handler)
            .build()
        focusRequest = request
        try {
            getSystemService(AudioManager::class.java).requestAudioFocus(request)
        } catch (_: Exception) { }
    }

    private fun abandonAudioFocus() {
        val request = focusRequest ?: return
        focusRequest = null
        try {
            getSystemService(AudioManager::class.java).abandonAudioFocusRequest(request)
        } catch (_: Exception) { }
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
            currentGain().let { player.setVolume(it, it) } // a crescendo starts quiet, not after a blast
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
        track.setVolume(currentGain()) // a crescendo starts quiet, not after a blast
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
    private fun snoozeAlarm(alarmId: Long, minMinutes: Int = 0) {
        val isFollowup = currentAlarmId == alarmId && currentIsFollowup
        val seq = if (isFollowup) currentFollowupSeq else 0
        stopAlarm(dismissed = false) {
            val app = applicationContext as BrutusApplication
            val alarm = AlarmRepository(app.database.alarmDao()).getById(alarmId)
            if (alarm != null) {
                // An alarm with snooze switched off (0 min) would ring again at once when it pauses
                // itself — the automatic pause uses at least minMinutes.
                val pause = alarm.copy(snoozeDuration = maxOf(alarm.snoozeDuration, minMinutes))
                AlarmScheduler.scheduleSnooze(this@AlarmService, pause, isFollowup, seq)
            }
            NextAlarmWidget.refresh(applicationContext)
        }
    }

    /** Stops audio/vibration and releases guard, volume override and wake lock. */
    private fun cleanupPlayback() {
        handler.removeCallbacks(ticker)
        abandonAudioFocus()
        lastGain = -1f
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
        // Whichever of the two notifications was not attached to the service must go too.
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID_QUIET)
        screenShown = false

        currentAlarmId = -1
        currentIsFollowup = false
        currentFollowupSeq = 0
        currentUltraHardcore = false
        currentHardcore = false

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
            // Covers the longest ring limit; an alarm without a limit is kept awake by its playback.
            acquire(31 * 60 * 1000L)
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
        /** The quiet variant while the alarm screen is in front (see [ringingNotification]). */
        const val NOTIFICATION_ID_QUIET = 1002
        const val ACTION_SCREEN_SHOWN = "com.pepperonas.brutus.ALARM_SCREEN_SHOWN"
        const val ACTION_SCREEN_HIDDEN = "com.pepperonas.brutus.ALARM_SCREEN_HIDDEN"

        fun notificationIdForUltraHardcore(alarmId: Long): Int =
            UltraHardcoreNotifier.notificationId(alarmId)
    }
}
