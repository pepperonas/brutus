package com.pepperonas.brutus

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pepperonas.brutus.ui.alarm.AlarmScreen
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
        hardcoreActive = hardcoreMode || ultraHardcoreMode

        soundPlayer = SoundPreviewPlayer(this).also {
            it.play(AlarmSound.fromId(soundId))
        }

        if (hardcoreActive) {
            audioGuard = HardcoreAudioGuard(applicationContext).also { it.attach() }
        }

        setContent {
            BrutusTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmScreen(
                        challengeFlags = flags,
                        qrCodeData = qrData,
                        mathProblemCount = mathCount,
                        shakeCount = shakeCount,
                        snoozeEnabled = snoozeEnabled,
                        hardcoreMode = hardcoreMode,
                        ultraHardcoreMode = ultraHardcoreMode,
                        mathDifficulty = mathDifficulty,
                        shakeSensitivity = shakeSensitivity,
                        onDismiss = { finish() },
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
    }
}
