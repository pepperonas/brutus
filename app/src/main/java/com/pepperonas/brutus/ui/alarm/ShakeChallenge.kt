package com.pepperonas.brutus.ui.alarm

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pepperonas.brutus.ui.theme.rememberReducedMotion
import com.pepperonas.brutus.util.ChallengeDifficulty
import kotlin.math.sqrt
import androidx.compose.ui.res.stringResource
import com.pepperonas.brutus.R
import androidx.compose.ui.res.pluralStringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShakeChallenge(
    requiredShakes: Int = 30,
    sensitivity: Int = ChallengeDifficulty.SHAKE_NORMAL,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotion()
    var shakeCount by remember { mutableIntStateOf(0) }
    var lastAccel by remember { mutableFloatStateOf(SensorManager.GRAVITY_EARTH) }
    val threshold = remember(sensitivity) { ChallengeDifficulty.shakeThreshold(sensitivity) }

    val progress by animateFloatAsState(
        targetValue = shakeCount.toFloat() / requiredShakes,
        animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "shakeProgress"
    )
    // Each counted shake makes the number jump a little and spring back — feedback that the
    // shake registered, without looking at the ring.
    val pop = remember { Animatable(1f) }
    val popSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    LaunchedEffect(shakeCount) {
        if (shakeCount == 0 || reducedMotion) return@LaunchedEffect
        pop.snapTo(1.18f)
        pop.animateTo(1f, popSpec)
    }
    // The listener keeps delivering after the target: report completion exactly once, or a late
    // shake during the hand-over animation would advance the chain a second time.
    var completed by remember { mutableStateOf(false) }

    DisposableEffect(sensitivity) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            private var lastShakeTime = 0L

            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val accel = sqrt(x * x + y * y + z * z)
                val delta = accel - lastAccel
                lastAccel = accel

                if (delta > threshold && System.currentTimeMillis() - lastShakeTime > 250) {
                    lastShakeTime = System.currentTimeMillis()
                    if (completed) return
                    shakeCount++
                    if (shakeCount >= requiredShakes) {
                        completed = true
                        onComplete()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.shake_title),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.shake_instruction),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Wavy ring: the physical exertion made visible — the wave keeps
        // rolling while progress fills.
        androidx.compose.foundation.layout.Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            CircularWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(200.dp),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = Color.White.copy(alpha = 0.1f),
                amplitude = if (reducedMotion) { _ -> 0f }
                else WavyProgressIndicatorDefaults.indicatorAmplitude,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$shakeCount",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 64.sp),
                    color = Color.White,
                    modifier = Modifier.graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }
                )
                Text(
                    text = "/ $requiredShakes",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (shakeCount > 0 && shakeCount < requiredShakes) {
            Text(
                text = pluralStringResource(
                    R.plurals.shake_remaining,
                    requiredShakes - shakeCount,
                    requiredShakes - shakeCount
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
