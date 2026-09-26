package com.pepperonas.brutus

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.scheduler.AlarmActions
import com.pepperonas.brutus.ui.alarm.AlarmScreen
import com.pepperonas.brutus.util.GlobalQrStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.ChallengeDifficulty
import com.pepperonas.brutus.util.ChallengeFlags
import com.pepperonas.brutus.util.HardcoreAudioGuard
import com.pepperonas.brutus.util.SoundPreviewPlayer

class TestAlarmActivity : ComponentActivity() {

    private var soundPlayer: SoundPreviewPlayer? = null
    private var hardcoreActive: Boolean = false
    private var audioGuard: HardcoreAudioGuard? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val flags = intent.getIntExtra(EXTRA_CHALLENGE_FLAGS, ChallengeFlags.MATH)
        val qrData = intent.getStringExtra(EXTRA_QR_DATA).orEmpty()
        val soundId = intent.getIntExtra(EXTRA_SOUND_ID, AlarmSound.KLAXON.id)
        val mathCount = intent.getIntExtra(EXTRA_MATH_COUNT, 3)
        val shakeCount = intent.getIntExtra(EXTRA_SHAKE_COUNT, 30)
        val snoozeEnabled = intent.getBooleanExtra(EXTRA_SNOOZE_ENABLED, true)
        val hardcoreMode = intent.getBooleanExtra(EXTRA_HARDCORE, false)
        val ultraHardcoreMode = intent.getBooleanExtra(EXTRA_ULTRA_HARDCORE, false)
        val mathDifficulty = intent.getIntExtra(EXTRA_MATH_DIFFICULTY, ChallengeDifficulty.MATH_HARD)
        val shakeSensitivity = intent.getIntExtra(EXTRA_SHAKE_SENSITIVITY, ChallengeDifficulty.SHAKE_NORMAL)
        // "Dismiss early" from the heads-up of a Hardcore alarm: the same challenges, but silent and
        // without snooze — solving them skips this one occurrence.
        val earlyDismissId = intent.getLongExtra(EXTRA_EARLY_DISMISS_ALARM_ID, -1L)
        val earlyDismiss = earlyDismissId != -1L
        hardcoreActive = !earlyDismiss && (hardcoreMode || ultraHardcoreMode)

        if (!earlyDismiss) {
            soundPlayer = SoundPreviewPlayer(this).also {
                it.play(AlarmSound.fromId(soundId))
            }
        }

        if (hardcoreActive) {
            audioGuard = HardcoreAudioGuard(applicationContext, restoreOnDetach = true).also { it.attach() }
        }

        setContent {
            BrutusTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmScreen(
                        challengeFlags = flags,
                        qrCodeData = qrData,
                        mathProblemCount = mathCount,
                        shakeCount = shakeCount,
                        snoozeEnabled = snoozeEnabled && !earlyDismiss,
                        hardcoreMode = hardcoreMode,
                        ultraHardcoreMode = ultraHardcoreMode,
                        mathDifficulty = mathDifficulty,
                        shakeSensitivity = shakeSensitivity,
                        onDismiss = {
                            if (earlyDismiss) {
                                val app = applicationContext
                                CoroutineScope(Dispatchers.IO).launch { AlarmActions.skipNext(app, earlyDismissId, solvedChallenge = true) }
                            }
                            finish()
                        },
                        onSnooze = { finish() }
                    )
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (hardcoreActive && (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                event.keyCode == KeyEvent.KEYCODE_VOLUME_MUTE)) {
            audioGuard?.clampToMax()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        soundPlayer?.stop()
        soundPlayer = null
        audioGuard?.detach()
        audioGuard = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CHALLENGE_FLAGS = "challenge_flags"
        const val EXTRA_QR_DATA = "qr_data"
        const val EXTRA_SOUND_ID = "sound_id"
        const val EXTRA_MATH_COUNT = "math_count"
        const val EXTRA_SHAKE_COUNT = "shake_count"
        const val EXTRA_SNOOZE_ENABLED = "snooze_enabled"
        const val EXTRA_HARDCORE = "hardcore"
        const val EXTRA_ULTRA_HARDCORE = "ultra_hardcore"
        const val EXTRA_MATH_DIFFICULTY = "math_difficulty"
        const val EXTRA_SHAKE_SENSITIVITY = "shake_sensitivity"
        const val EXTRA_EARLY_DISMISS_ALARM_ID = "early_dismiss_alarm_id"

        /** The challenges exactly as [alarm] would ring with them (QR code included). */
        fun intentFor(context: Context, alarm: AlarmEntity): Intent =
            Intent(context, TestAlarmActivity::class.java)
                .putExtra(EXTRA_CHALLENGE_FLAGS, alarm.challengeFlags)
                .putExtra(EXTRA_QR_DATA, GlobalQrStore.get(context))
                .putExtra(EXTRA_SOUND_ID, alarm.soundId)
                .putExtra(EXTRA_MATH_COUNT, alarm.mathProblemCount)
                .putExtra(EXTRA_SHAKE_COUNT, alarm.shakeCount)
                .putExtra(EXTRA_SNOOZE_ENABLED, alarm.snoozeDuration > 0)
                .putExtra(EXTRA_HARDCORE, alarm.hardcoreMode)
                .putExtra(EXTRA_ULTRA_HARDCORE, alarm.ultraHardcoreMode)
                .putExtra(EXTRA_MATH_DIFFICULTY, alarm.mathDifficulty)
                .putExtra(EXTRA_SHAKE_SENSITIVITY, alarm.shakeSensitivity)
    }
}
