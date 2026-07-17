package com.pepperonas.brutus.ui.alarm

import com.pepperonas.brutus.util.ChallengeDifficulty
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the math-gate problem generator. `generateProblem` draws from the
 * global [kotlin.random.Random], so the range/sign invariants are asserted over
 * many samples instead of a seed — every operator branch is hit with
 * overwhelming probability at 500 iterations.
 */
class MathProblemTest {

    private val samples = 500

    // ----- MathProblem arithmetic -----

    @Test
    fun `answer is computed per operator`() {
        assertEquals(12, MathProblem(7, 5, '+').answer)
        assertEquals(2, MathProblem(7, 5, '-').answer)
        assertEquals(35, MathProblem(7, 5, '*').answer)
    }

    @Test
    fun `unknown operator falls back to addition`() {
        assertEquals(12, MathProblem(7, 5, '/').answer)
        assertEquals(12, MathProblem(7, 5, '?').answer)
    }

    @Test
    fun `display renders the question with a placeholder`() {
        assertEquals("7 - 5 = ?", MathProblem(7, 5, '-').display)
    }

    // ----- Generator invariants per difficulty -----

    @Test
    fun `easy problems stay within 0 to 20 and never go negative`() {
        repeat(samples) {
            val p = generateProblem(ChallengeDifficulty.MATH_EASY)
            assertTrue(p.operator in listOf('+', '-'), "unexpected op ${p.operator}")
            assertTrue(p.answer >= 0, "negative answer: ${p.display}")
            // "Addition und Subtraktion bis 20" — the promised difficulty cap.
            assertTrue(p.answer <= 20, "answer above easy cap: ${p.display}")
        }
    }

    @Test
    fun `hard problems keep subtraction positive and multiplication in range`() {
        repeat(samples) {
            val p = generateProblem(ChallengeDifficulty.MATH_HARD)
            assertTrue(p.operator in listOf('+', '-', '*'), "unexpected op ${p.operator}")
            when (p.operator) {
                '-' -> assertTrue(p.answer > 0, "non-positive subtraction: ${p.display}")
                '*' -> {
                    assertTrue(p.a in 10..49, "a out of range: ${p.display}")
                    assertTrue(p.b in 2..19, "b out of range: ${p.display}")
                }
                else -> {
                    assertTrue(p.a in 50..499 && p.b in 50..499, "operand out of range: ${p.display}")
                }
            }
        }
    }

    @Test
    fun `brutal problems use two-digit multiplication and large positive results`() {
        repeat(samples) {
            val p = generateProblem(ChallengeDifficulty.MATH_BRUTAL)
            assertTrue(p.operator in listOf('+', '-', '*'), "unexpected op ${p.operator}")
            when (p.operator) {
                '*' -> {
                    assertTrue(p.a in 11..29, "a out of range: ${p.display}")
                    assertTrue(p.b in 11..29, "b out of range: ${p.display}")
                }
                '-' -> assertTrue(p.answer > 0, "non-positive subtraction: ${p.display}")
                else -> assertTrue(p.a >= 500 && p.b >= 500, "operand too small: ${p.display}")
            }
        }
    }

    @Test
    fun `unknown difficulty falls back to the hard generator instead of throwing`() {
        repeat(samples) {
            val p = generateProblem(99)
            assertTrue(p.operator in listOf('+', '-', '*'))
            if (p.operator == '-') assertTrue(p.answer > 0)
        }
        // negative levels take the same fallback
        generateProblem(-1)
    }

    @Test
    fun `every difficulty eventually produces each of its operators`() {
        fun opsSeen(level: Int): Set<Char> =
            (1..samples).map { generateProblem(level).operator }.toSet()

        assertEquals(setOf('+', '-'), opsSeen(ChallengeDifficulty.MATH_EASY))
        assertEquals(setOf('+', '-', '*'), opsSeen(ChallengeDifficulty.MATH_HARD))
        assertEquals(setOf('+', '-', '*'), opsSeen(ChallengeDifficulty.MATH_BRUTAL))
    }
}
