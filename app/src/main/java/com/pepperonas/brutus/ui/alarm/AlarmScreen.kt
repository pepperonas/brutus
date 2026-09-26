package com.pepperonas.brutus.ui.alarm

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.res.stringResource
import com.pepperonas.brutus.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    // Saveable: rotating or folding the phone must not restart the math problems.
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
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
    // Kept as State and read only inside drawBehind: the breathing then repaints the background
    // alone. Reading it in composition recomposed the whole alarm screen every frame while it rang.
    val breathe: State<Float> = if (reducedMotion) {
        remember { mutableFloatStateOf(0.3f) }
    } else {
        rememberInfiniteTransition(label = "alarmBreathe").animateFloat(
            initialValue = 0.22f,
            targetValue = 0.40f,
            animationSpec = infiniteRepeatable(
                tween(2500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "breatheAlpha"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black, BrutusDarkRed.copy(alpha = breathe.value), Color.Black)
                    )
                )
            }
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
                    text = stringResource(R.string.alarm_banner),
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
                            text = stringResource(
                                if (ultraHardcoreMode) R.string.alarm_badge_ultra_hardcore
                                else R.string.alarm_badge_hardcore
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            letterSpacing = 2.sp
                        )
                    }
                }

                if (isFollowup && followupSeq > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.alarm_followup, followupSeq),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        letterSpacing = 1.sp
                    )
                }

                if (active.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ChallengeProgressDots(total = active.size, current = currentIndex)
                    Text(
                        text = stringResource(
                            R.string.alarm_challenge_progress,
                            minOf(currentIndex + 1, active.size),
                            active.size
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            // Challenge area (sequential). The "done" moment is part of the same transition, so
            // the last challenge hands over to it instead of being cut off.
            val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            val fastEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    (fadeIn(effects) + scaleIn(spatial, initialScale = 0.92f)) togetherWith fadeOut(fastEffects)
                },
                label = "challengeTransition"
            ) { idx ->
                if (idx < active.size) {
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
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.alarm_done),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.alarm_good_morning),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The payoff springs in instead of popping into the layout.
                AnimatedVisibility(
                    visible = allDone,
                    enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec(), initialScale = 0.6f) +
                        fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                ) {
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
            text = stringResource(R.string.alarm_stop),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** The current challenge's dot swells on a spring; colours change on the effects spec. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChallengeProgressDots(total: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            val target = when {
                i < current -> MaterialTheme.colorScheme.tertiary
                i == current -> MaterialTheme.colorScheme.primary
                else -> Color.White.copy(alpha = 0.2f)
            }
            val color by animateColorAsState(target, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "dot")
            val size by animateDpAsState(
                if (i == current) 16.dp else 12.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "dotSize"
            )
            Box(
                modifier = Modifier
                    .size(size)
                    .background(color, CircleShape)
            )
        }
    }
}

private fun getCurrentTime(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
