package com.pepperonas.brutus

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pepperonas.brutus.data.AlarmDatabase
import com.pepperonas.brutus.service.AlarmService
import com.pepperonas.brutus.ui.alarm.AlarmScreen
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.util.ChallengeFlags
import com.pepperonas.brutus.util.GlobalQrStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {

    private var alarmId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockScreen()

        alarmId = intent.getLongExtra("alarm_id", -1)

        val db = AlarmDatabase.getInstance(applicationContext)
        val dao = db.alarmDao()

        CoroutineScope(Dispatchers.IO).launch {
            val alarm = dao.getById(alarmId)
            val flags = alarm?.challengeFlags ?: ChallengeFlags.MATH
            val qrData = GlobalQrStore.get(applicationContext)
            val mathCount = alarm?.mathProblemCount ?: 3
            val shakeCount = alarm?.shakeCount ?: 30
            val snoozeEnabled = (alarm?.snoozeDuration ?: 5) > 0

            runOnUiThread {
                setContent {
                    BrutusTheme {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            AlarmScreen(
                                challengeFlags = flags,
                                qrCodeData = qrData,
                                mathProblemCount = mathCount,
                                shakeCount = shakeCount,
                                snoozeEnabled = snoozeEnabled,
                                onDismiss = { stopAlarm() },
                                onSnooze = { snoozeAlarm() }
                            )
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setupLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KeyguardManager::class.java)
            km.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun stopAlarm() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        }
        startService(intent)
        finishAndRemoveTask()
    }

    private fun snoozeAlarm() {
        val intent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_SNOOZE
            putExtra("alarm_id", alarmId)
        }
        startService(intent)
        finishAndRemoveTask()
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        // Back-Button blockiert - Challenge muss bestanden werden
    }
}
