package com.pepperonas.brutus

import android.app.NotificationManager
import android.os.Bundle
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pepperonas.brutus.scheduler.AlarmScheduler
import com.pepperonas.brutus.service.AlarmService
import com.pepperonas.brutus.ui.alarm.StepChallenge
import com.pepperonas.brutus.ui.theme.BrutusDarkRed
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.util.UltraHardcoreStore
import kotlinx.coroutines.delay

/**
 * Anti-snooze task screen launched from the Ultra Hardcore reminder notification.
 * Completing the step challenge cancels both pending follow-up alarms and clears
 * the persistent notification.
 */
class UltraHardcoreTaskActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        val required = if (alarmId != -1L) UltraHardcoreStore.stepTarget(this, alarmId)
        else UltraHardcoreStore.DEFAULT_STEP_TARGET

        setContent {
            BrutusTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var done by remember { mutableStateOf(false) }

                    LaunchedEffect(done) {
                        if (done && alarmId != -1L) {
                            AlarmScheduler.cancelAllFollowups(applicationContext, alarmId)
                            UltraHardcoreStore.clearAllFor(applicationContext, alarmId)
                            val nm = getSystemService(NotificationManager::class.java)
                            nm.cancel(AlarmService.notificationIdForUltraHardcore(alarmId))
                            delay(1800L)
                            finish()
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black, BrutusDarkRed.copy(alpha = 0.3f), Color.Black)
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .safeDrawingPadding()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Header()

                            if (done) {
                                Success()
                            } else {
                                StepChallenge(
                                    requiredSteps = required,
                                    onComplete = { done = true }
                                )
                            }

                            BottomBar(
                                done = done,
                                onCancel = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Header() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "ULTRA HARDCORE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Re-Alarme stoppen",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    private fun Success() {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Geschafft!",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Re-Alarme deaktiviert. Du bist wach.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    private fun BottomBar(done: Boolean, onCancel: () -> Unit) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!done) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White.copy(alpha = 0.55f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Später — Re-Alarme akzeptieren",
                        fontSize = 14.sp
                    )
                }
            } else {
                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    // Theme primary IS the brand red on the pinned-dark alarm surfaces.
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Schliessen", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
    }
}
