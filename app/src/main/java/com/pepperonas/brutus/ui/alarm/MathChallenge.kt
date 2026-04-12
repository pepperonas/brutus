package com.pepperonas.brutus.ui.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pepperonas.brutus.ui.theme.BrutusRed
import com.pepperonas.brutus.ui.theme.BrutusRedBright
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

private fun generateProblem(): MathProblem {
    val ops = listOf('+', '-', '*')
    val op = ops.random()
    return when (op) {
        '*' -> MathProblem(Random.nextInt(10, 50), Random.nextInt(2, 20), op)
        '-' -> {
            val a = Random.nextInt(50, 200)
            val b = Random.nextInt(10, a)
            MathProblem(a, b, op)
        }
        else -> MathProblem(Random.nextInt(50, 500), Random.nextInt(50, 500), op)
    }
}

@Composable
fun MathChallenge(
    totalRequired: Int = 3,
    onComplete: () -> Unit,
) {
    var solvedCount by remember { mutableIntStateOf(0) }
    var problem by remember { mutableStateOf(generateProblem()) }
    var userInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Mathe-Challenge",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Lösung ${solvedCount + 1} von $totalRequired",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = problem.display,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = userInput,
            onValueChange = {
                userInput = it.filter { c -> c.isDigit() || c == '-' }
                showError = false
            },
            label = { Text("Antwort") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = { checkAnswer(userInput, problem, onCorrect = {
                solvedCount++
                userInput = ""
                if (solvedCount >= totalRequired) onComplete()
                else problem = generateProblem()
            }, onWrong = { showError = true; userInput = "" }) }),
            isError = showError,
            modifier = Modifier.fillMaxWidth(0.6f)
        )

        if (showError) {
            Text(
                text = "Falsch! Versuch es nochmal.",
                color = BrutusRedBright,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                checkAnswer(userInput, problem, onCorrect = {
                    solvedCount++
                    userInput = ""
                    if (solvedCount >= totalRequired) onComplete()
                    else problem = generateProblem()
                }, onWrong = { showError = true; userInput = "" })
            },
            colors = ButtonDefaults.buttonColors(containerColor = BrutusRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Prüfen", style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun checkAnswer(
    input: String,
    problem: MathProblem,
    onCorrect: () -> Unit,
    onWrong: () -> Unit
) {
    val answer = input.toIntOrNull()
    if (answer == problem.answer) onCorrect() else onWrong()
}
