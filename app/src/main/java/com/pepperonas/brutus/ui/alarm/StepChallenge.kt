package com.pepperonas.brutus.ui.alarm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pepperonas.brutus.ui.theme.rememberReducedMotion

/**
 * The Ultra Hardcore "anti-snooze" task: walk a configurable number of steps within the
 * 10-minute window so that both follow-up alarms get cancelled. If the device exposes
 * neither STEP_COUNTER nor STEP_DETECTOR (rare, very old hardware) we fall back to the
 * accelerometer-based shake heuristic so the user can never get fully locked out.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StepChallenge(
    requiredSteps: Int,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val reducedMotion = rememberReducedMotion()
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val stepCounter = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    val stepDetector = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) }
    val accel = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    var permissionGranted by remember { mutableStateOf(hasActivityRecognition(context)) }
    var steps by remember { mutableIntStateOf(0) }
    var baseline by remember { mutableFloatStateOf(-1f) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    DisposableEffect(permissionGranted) {
        if (!permissionGranted) return@DisposableEffect onDispose { }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_STEP_COUNTER -> {
                        val total = event.values.firstOrNull() ?: return
                        if (baseline < 0f) baseline = total
                        steps = (total - baseline).toInt().coerceAtLeast(0)
                    }
                    Sensor.TYPE_STEP_DETECTOR -> steps += 1
                    Sensor.TYPE_ACCELEROMETER -> {
                        // Fallback only — count strong vertical impulses as steps. Far less
                        // reliable than a real pedometer, but better than locking the user out.
                        val z = event.values.getOrNull(2) ?: return
                        if (z > 13f) steps += 1
                    }
                }
                if (steps >= requiredSteps) onComplete()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        val sensor = stepCounter ?: stepDetector ?: accel
        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val progress by animateFloatAsState(
        targetValue = (steps.toFloat() / requiredSteps).coerceIn(0f, 1f),
        label = "stepProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Anti-Schlummer-Aufgabe",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Steh auf und lauf $requiredSteps Schritte. Erst dann werden die Re-Alarme abgebrochen.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            CircularWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(220.dp),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = Color.White.copy(alpha = 0.1f),
                amplitude = if (reducedMotion) { _ -> 0f }
                else WavyProgressIndicatorDefaults.indicatorAmplitude,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$steps",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                    color = Color.White
                )
                Text(
                    text = "/ $requiredSteps",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!permissionGranted) {
            Text(
                text = "Berechtigung 'Körperliche Aktivität' wird benötigt, damit der Schrittzähler arbeitet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    } else {
                        permissionGranted = true
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Berechtigung anfragen") }
        } else if (steps in 1 until requiredSteps) {
            Text(
                text = "Noch ${requiredSteps - steps} Schritte!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun hasActivityRecognition(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACTIVITY_RECOGNITION
    ) == PackageManager.PERMISSION_GRANTED
}
