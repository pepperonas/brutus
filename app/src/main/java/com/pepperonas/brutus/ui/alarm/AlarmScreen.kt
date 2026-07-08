package com.pepperonas.brutus.ui.alarm

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.pepperonas.brutus.ui.theme.BrutusDarkRed
import com.pepperonas.brutus.ui.theme.BrutusRedBright
import com.pepperonas.brutus.ui.theme.rememberReducedMotion
import com.pepperonas.brutus.util.ChallengeDifficulty
import com.pepperonas.brutus.util.ChallengeFlags
import com.pepperonas.brutus.util.rememberBrutusHaptics
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmScreen(
    challengeFlags: Int,
    qrCodeData: String,
    mathProblemCount: Int,
    shakeCount: Int,
    snoozeEnabled: Boolean,
    hardcoreMode: Boolean = false,
    ultraHardcoreMode: Boolean = false,
    isFollowup: Boolean = false,
    followupSeq: Int = 0,
    mathDifficulty: Int = ChallengeDifficulty.MATH_HARD,
    shakeSensitivity: Int = ChallengeDifficulty.SHAKE_NORMAL,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    val active = remember(challengeFlags) {
        val list = ChallengeFlags.activeList(challengeFlags)
        list.ifEmpty { listOf(ChallengeFlags.MATH) }
    }
    var currentIndex by remember { mutableIntStateOf(0) }
    val allDone = currentIndex >= active.size
    val haptics = rememberBrutusHaptics()

    LaunchedEffect(currentIndex, allDone) {
        if (currentIndex > 0 || allDone) {
            if (allDone) haptics.success() else haptics.tap()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTime()
            delay(1000)
        }
    }

    // Breathing brand gradient: the red core slowly swells and settles (~5 s
    // cycle, small alpha delta — deliberately far from any flicker/strobe).
    // Static when system animations are disabled.
    val reducedMotion = rememberReducedMotion()
    val breathe = if (reducedMotion) {
        0.3f
    } else {
        val transition = rememberInfiniteTransition(label = "alarmBreathe")
        val value by transition.animateFloat(
            initialValue = 0.22f,
            targetValue = 0.40f,
            animationSpec = infiniteRepeatable(
                tween(2500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "breatheAlpha"
        )
        value
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black, BrutusDarkRed.copy(alpha = breathe), Color.Black)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Gradient bleeds edge-to-edge behind the system bars;
                // content stays clear of cutout + gesture areas.
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTime,
                    // displayLarge = Space Grotesk + tabular numerals: the hero
                    // readout ticks without horizontal jitter.
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 76.sp),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "BRUTUS ALARM",
                    style = MaterialTheme.typography.titleLarge,
                    // Deliberate raw brand color: the wordmark stays BrutusRedBright
                    // even under Material You — this screen IS the brand.
                    color = BrutusRedBright,
                    letterSpacing = 8.sp
                )

                if (hardcoreMode || ultraHardcoreMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.extraSmall
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (ultraHardcoreMode) "ULTRA HARDCORE MODE" else "HARDCORE MODE",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            letterSpacing = 2.sp
                        )
                    }
                }

                if (isFollowup && followupSeq > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Re-Alarm $followupSeq/2 — du bist nicht entkommen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        letterSpacing = 1.sp
                    )
                }

                if (active.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ChallengeProgressDots(total = active.size, current = currentIndex)
                    Text(
                        text = "Challenge ${minOf(currentIndex + 1, active.size)} von ${active.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            // Challenge area (sequential)
            if (!allDone) {
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        (fadeIn(tween(300)) togetherWith fadeOut(tween(150)))
                    },
                    label = "challengeTransition"
                ) { idx ->
                    when (active[idx]) {
                        ChallengeFlags.MATH -> MathChallenge(
                            totalRequired = mathProblemCount,
                            difficulty = mathDifficulty,
                            onComplete = { currentIndex++ }
                        )
                        ChallengeFlags.SHAKE -> ShakeChallenge(
                            requiredShakes = shakeCount,
                            sensitivity = shakeSensitivity,
                            onComplete = { currentIndex++ }
                        )
                        ChallengeFlags.QR -> QrChallenge(
                            expectedQrData = qrCodeData,
                            onComplete = { currentIndex++ }
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "Geschafft!",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Guten Morgen",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (allDone) {
                    DismissButton(onDismiss = onDismiss)
                }

                if (snoozeEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SwipeToSnoozeButton(onSnooze = onSnooze)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * The emotional payoff button — a big primary CTA whose shape relaxes under
 * the finger (pill → squircle) with a spatial spring.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DismissButton(onDismiss: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) 12.dp else 32.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "dismissCorner"
    )
    Button(
        onClick = onDismiss,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(corner),
    ) {
        Text(
            text = "ALARM STOPPEN",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChallengeProgressDots(total: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { i ->
            val color = when {
                i < current -> MaterialTheme.colorScheme.tertiary
                i == current -> MaterialTheme.colorScheme.primary
                else -> Color.White.copy(alpha = 0.2f)
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

private fun getCurrentTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
