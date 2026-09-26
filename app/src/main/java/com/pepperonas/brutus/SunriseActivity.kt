@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.pepperonas.brutus

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pepperonas.brutus.data.AlarmDatabase
import com.pepperonas.brutus.scheduler.AlarmScheduler
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.SoundPreviewPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.pepperonas.brutus.R

/**
 * Pre-alarm "Sunrise" — fires up to [AlarmScheduler.SUNRISE_LEAD_MIN] minutes before
 * the main alarm. Ramps screen brightness from 0 to 1 over the lead window and plays
 * the soft Glockenspiel at gradually rising amplitude. Has no challenges, no
 * Hardcore guard, and no foreground service — the actual alarm is a separate
 * registration that fires on time regardless of whether this activity is open.
 *
 * "Wecker stoppen" cancels the upcoming main alarm too (handy if the user is
 * already up and doesn't want the brutal wake-up). "Schon wach" just closes
 * the sunrise screen, the main alarm remains armed.
 */
class SunriseActivity : ComponentActivity() {

    private var soundPlayer: SoundPreviewPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupLockScreen()

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        val mainTriggerAt = intent.getLongExtra(EXTRA_MAIN_TRIGGER_AT, 0L)
        getSystemService(android.app.NotificationManager::class.java)
            .cancel(notificationIdForSunrise(alarmId))

        soundPlayer = SoundPreviewPlayer(this).also {
            it.play(AlarmSound.CHIME)
        }

        // Hidden until the alarm is known: a Hardcore alarm must not be switched off with one tap
        // ten minutes early — that would defeat the point of the challenge.
        val canStopMain = mutableStateOf(false)
        CoroutineScope(Dispatchers.IO).launch {
            val alarm = AlarmDatabase.getInstance(applicationContext).alarmDao().getById(alarmId)
            canStopMain.value = alarm != null && !alarm.hardcoreEffective
        }

        setContent {
            BrutusTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SunriseScreen(
                        mainTriggerAt = mainTriggerAt,
                        canStopMain = canStopMain.value,
                        onSkip = { finish() },
                        // The real alarm takes over now — the chime must not keep playing under it.
                        onMainTime = { finish() },
                        onCancelMain = {
                            if (alarmId != -1L) {
                                // Disable the alarm entirely — same effect as toggling it off
                                // in the list. Repeating alarms can be re-enabled from there.
                                CoroutineScope(Dispatchers.IO).launch {
                                    val dao = AlarmDatabase.getInstance(applicationContext).alarmDao()
                                    val alarm = dao.getById(alarmId)
                                    if (alarm != null && !alarm.hardcoreEffective) {
                                        AlarmScheduler.cancel(applicationContext, alarm)
                                        dao.setEnabled(alarmId, false)
                                        com.pepperonas.brutus.widget.NextAlarmWidget.refresh(applicationContext)
                                    }
                                }
                            }
                            finish()
                        },
                        onBrightnessChange = { fraction ->
                            window.attributes = window.attributes.apply {
                                screenBrightness = fraction.coerceIn(0.05f, 1f)
                            }
                        },
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setupLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = 0.05f }
    }

    override fun onDestroy() {
        soundPlayer?.stop()
        soundPlayer = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_MAIN_TRIGGER_AT = "main_trigger_at"

        fun notificationIdForSunrise(alarmId: Long): Int = 3000 + (alarmId.toInt() and 0xFFFF)
    }
}

@Composable
private fun SunriseScreen(
    mainTriggerAt: Long,
    canStopMain: Boolean,
    onSkip: () -> Unit,
    onMainTime: () -> Unit,
    onCancelMain: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
) {
    val context = LocalContext.current
    val totalMs = AlarmScheduler.SUNRISE_LEAD_MIN * 60_000L
    val startedAt = remember { System.currentTimeMillis() }
    var progress by remember { mutableFloatStateOf(0f) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            val elapsed = (nowMillis - startedAt).coerceAtLeast(0L)
            progress = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
            onBrightnessChange(progress)
            if (mainTriggerAt > 0L && nowMillis >= mainTriggerAt) {
                onMainTime()
                break
            }
            delay(500L)
        }
    }

    // Background gradient warms up from black to dawn-orange as progress grows.
    val warmth = progress
    val bgTop = Color(
        red = 0.05f + 0.9f * warmth,
        green = 0.03f + 0.45f * warmth,
        blue = 0.05f + 0.10f * warmth,
    )
    val bgBottom = Color(
        red = 0.02f + 0.30f * warmth,
        green = 0.01f + 0.15f * warmth,
        blue = 0.03f + 0.05f * warmth,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.sunrise_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 6.sp,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f + 0.3f * warmth)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = formatNow(nowMillis),
                    // Calm hero: light weight stays (gentle pre-alarm, not the
                    // ring screen), but tabular numerals stop the tick-jitter.
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 88.sp,
                        fontWeight = FontWeight.Light,
                    ),
                    color = Color.White.copy(alpha = 0.6f + 0.4f * warmth)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (mainTriggerAt > 0L)
                        stringResource(
                            R.string.sunrise_countdown,
                            remaining(mainTriggerAt, nowMillis)
                        )
                    else stringResource(R.string.sunrise_running),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
                Spacer(modifier = Modifier.height(24.dp))
                // The lead-in made visible: a gently rolling wave fills up to the real alarm.
                val reduced = com.pepperonas.brutus.ui.theme.rememberReducedMotion()
                androidx.compose.material3.LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(0.7f),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = Color.White.copy(alpha = 0.12f),
                    amplitude = if (reduced) { _ -> 0f }
                    else androidx.compose.material3.WavyProgressIndicatorDefaults.indicatorAmplitude,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (canStopMain) {
                    Button(
                        onClick = onCancelMain,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            stringResource(R.string.sunrise_stop_alarm),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Button(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White.copy(alpha = 0.7f)
                    ),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        stringResource(R.string.sunrise_close),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

private fun formatNow(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

private fun remaining(target: Long, now: Long): String {
    val secs = ((target - now) / 1000L).coerceAtLeast(0L)
    val mm = secs / 60
    val ss = secs % 60
    return "%d:%02d".format(mm, ss)
}
