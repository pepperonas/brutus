package com.pepperonas.brutus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pepperonas.brutus.ui.alarm.AlarmScreen
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.ChallengeFlags
import com.pepperonas.brutus.util.SoundPreviewPlayer

class TestAlarmActivity : ComponentActivity() {

    private var soundPlayer: SoundPreviewPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val flags = intent.getIntExtra(EXTRA_CHALLENGE_FLAGS, ChallengeFlags.MATH)
        val qrData = intent.getStringExtra(EXTRA_QR_DATA).orEmpty()
        val soundId = intent.getIntExtra(EXTRA_SOUND_ID, AlarmSound.KLAXON.id)
        val mathCount = intent.getIntExtra(EXTRA_MATH_COUNT, 3)
        val shakeCount = intent.getIntExtra(EXTRA_SHAKE_COUNT, 30)
        val snoozeEnabled = intent.getBooleanExtra(EXTRA_SNOOZE_ENABLED, true)

        soundPlayer = SoundPreviewPlayer(this).also {
            it.play(AlarmSound.fromId(soundId))
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
                        onDismiss = { finish() },
                        onSnooze = { finish() }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        soundPlayer?.stop()
        soundPlayer = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CHALLENGE_FLAGS = "challenge_flags"
        const val EXTRA_QR_DATA = "qr_data"
        const val EXTRA_SOUND_ID = "sound_id"
        const val EXTRA_MATH_COUNT = "math_count"
        const val EXTRA_SHAKE_COUNT = "shake_count"
        const val EXTRA_SNOOZE_ENABLED = "snooze_enabled"
    }
}
