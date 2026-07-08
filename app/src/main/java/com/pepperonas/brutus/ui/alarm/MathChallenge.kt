package com.pepperonas.brutus.ui.alarm

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.util.ChallengeDifficulty
import kotlin.random.Random

data class MathProblem(val a: Int, val b: Int, val operator: Char) {
    val answer: Int = when (operator) {
        '+' -> a + b
        '-' -> a - b
        '*' -> a * b
        else -> a + b
    }
    val display: String = "$a $operator $b = ?"
}

fun generateProblem(difficulty: Int): MathProblem = when (difficulty) {
    ChallengeDifficulty.MATH_EASY -> {
        // Addition and subtraction with single/low-double digits — must remain ≥ 0.
        val ops = listOf('+', '-')
        val op = ops.random()
        if (op == '-') {
            val a = Random.nextInt(8, 21)
            val b = Random.nextInt(0, a + 1)
            MathProblem(a, b, op)
        } else {
            MathProblem(Random.nextInt(1, 11), Random.nextInt(1, 11), op)
        }
    }
    ChallengeDifficulty.MATH_BRUTAL -> {
        // Two-digit multiplication or very large additions/subtractions.
        val ops = listOf('+', '-', '*', '*') // bias towards multiplication
        when (val op = ops.random()) {
            '*' -> MathProblem(Random.nextInt(11, 30), Random.nextInt(11, 30), op)
            '-' -> {
                val a = Random.nextInt(500, 2000)
                val b = Random.nextInt(100, a)
                MathProblem(a, b, op)
            }
            else -> MathProblem(Random.nextInt(500, 2000), Random.nextInt(500, 2000), op)
        }
    }
    else -> {
        // Legacy "hard" generator (the previous default).
        val ops = listOf('+', '-', '*')
        when (val op = ops.random()) {
            '*' -> MathProblem(Random.nextInt(10, 50), Random.nextInt(2, 20), op)
            '-' -> {
                val a = Random.nextInt(50, 200)
                val b = Random.nextInt(10, a)
                MathProblem(a, b, op)
            }
            else -> MathProblem(Random.nextInt(50, 500), Random.nextInt(50, 500), op)
        }
    }
}

/**
 * Math gate with its own on-screen keypad — no system keyboard popping over
 * the alarm (and no autocorrect nonsense at 6 a.m.). Every key morphs slightly
 * squarer under the finger; the confirm key sits on primary.
 */
@Composable
fun MathChallenge(
    totalRequired: Int = 3,
    difficulty: Int = ChallengeDifficulty.MATH_HARD,
    onComplete: () -> Unit,
) {
    var solvedCount by remember { mutableIntStateOf(0) }
    var problem by remember { mutableStateOf(generateProblem(difficulty)) }
    var userInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val submit: () -> Unit = {
        if (userInput.toIntOrNull() == problem.answer) {
            solvedCount++
            userInput = ""
            showError = false
            if (solvedCount >= totalRequired) onComplete()
            else problem = generateProblem(difficulty)
        } else {
            showError = true
            userInput = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Mathe-Challenge",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Lösung ${solvedCount + 1} von $totalRequired · ${ChallengeDifficulty.mathLabel(difficulty)}",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = problem.display,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Answer readout
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .background(
                    Color.White.copy(alpha = 0.08f),
                    MaterialTheme.shapes.medium
                )
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userInput.ifEmpty { " " },
                style = MaterialTheme.typography.displaySmall,
                color = if (showError) MaterialTheme.colorScheme.error else Color.White,
            )
        }

        Text(
            text = if (showError) "Falsch! Versuch es nochmal." else " ",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
        )

        NumberPad(
            onDigit = { d ->
                if (userInput.length < 7) {
                    userInput += d
                    showError = false
                }
            },
            onBackspace = { userInput = userInput.dropLast(1) },
            onSubmit = submit,
            submitEnabled = userInput.isNotEmpty(),
        )
    }
}

@Composable
private fun NumberPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    submitEnabled: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("123", "456", "789").forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowKeys.forEach { key ->
                    KeypadButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onDigit(key) },
                    ) {
                        Text(key.toString(), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KeypadButton(modifier = Modifier.weight(1f), onClick = onBackspace) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Löschen",
                )
            }
            KeypadButton(modifier = Modifier.weight(1f), onClick = { onDigit('0') }) {
                Text("0", style = MaterialTheme.typography.headlineSmall)
            }
            KeypadButton(
                modifier = Modifier.weight(1f),
                onClick = onSubmit,
                enabled = submitEnabled,
                primary = true,
            ) {
                Icon(Icons.Default.Check, contentDescription = "Prüfen")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun KeypadButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) 10.dp else 22.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "keyCorner"
    )
    val colors = if (primary) {
        ButtonDefaults.buttonColors()
    } else {
        // Tonal keys on the black alarm gradient — the theme's tonal
        // containers would vanish here, so keys use translucent white.
        ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.1f),
            contentColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.04f),
            disabledContentColor = Color.White.copy(alpha = 0.3f),
        )
    }
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        shape = RoundedCornerShape(corner),
        interactionSource = interaction,
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        modifier = modifier.height(56.dp),
    ) {
        content()
    }
}

// Alarm surfaces are pinned dark by design (black/red brand gradient),
// so there is no light variant to preview.
@Preview(name = "Math keypad", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MathChallengePreview() {
    BrutusTheme(darkTheme = true) {
        MathChallenge(totalRequired = 3, onComplete = {})
    }
}
