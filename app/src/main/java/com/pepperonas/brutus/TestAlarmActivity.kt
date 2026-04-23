package com.pepperonas.brutus

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pepperonas.brutus.ui.alarm.AlarmScreen
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.ChallengeFlags
import com.pepperonas.brutus.util.HardcoreAudioGuard
import com.pepperonas.brutus.util.SoundPreviewPlayer

class TestAlarmActivity : ComponentActivity() {

    private var soundPlayer: SoundPreviewPlayer? = null
    private var hardcoreMode: Boolean = false
    private var audioGuard: HardcoreAudioGuard? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val flags = intent.getIntExtra(EXTRA_CHALLENGE_FLAGS, ChallengeFlags.MATH)
        val qrData = intent.getStringExtra(EXTRA_QR_DATA).orEmpty()
        val soundId = intent.getIntExtra(EXTRA_SOUND_ID, AlarmSound.KLAXON.id)
        val mathCount = intent.getIntExtra(EXTRA_MATH_COUNT, 3)
        val shakeCount = intent.getIntExtra(EXTRA_SHAKE_COUNT, 30)
        val snoozeEnabled = intent.getBooleanExtra(EXTRA_SNOOZE_ENABLED, true)
        hardcoreMode = intent.getBooleanExtra(EXTRA_HARDCORE, false)

        soundPlayer = SoundPreviewPlayer(this).also {
            it.play(AlarmSound.fromId(soundId))
        }

        if (hardcoreMode) {
            audioGuard = HardcoreAudioGuard(applicationContext).also { it.attach() }
        }

        setContent {
            BrutusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmScreen(
                        challengeFlags = flags,
                        qrCodeData = qrData,
                        mathProblemCount = mathCount,
                        shakeCount = shakeCount,
                        snoozeEnabled = snoozeEnabled,
                        hardcoreMode = hardcoreMode,
                        onDismiss = { finish() },
                        onSnooze = { finish() }
                    )
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (hardcoreMode && (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
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
    }
}
