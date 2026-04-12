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

        soundPlayer = SoundPreviewPlayer(this).also {
            it.play(AlarmSound.fromId(soundId))
        }

        setContent {
            BrutusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmScreen(
                        challengeFlags = flags,
                        qrCodeData = qrData,
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
    }
}
