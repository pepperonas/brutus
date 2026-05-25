package com.pepperonas.brutus.util

import com.pepperonas.brutus.ui.alarm.MathProblem
import com.pepperonas.brutus.ui.alarm.generateProblem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChallengeDifficultyTest {

    @Test
    fun `shake thresholds increase with sensitivity level`() {
        val light = ChallengeDifficulty.shakeThreshold(ChallengeDifficulty.SHAKE_LIGHT)
        val normal = ChallengeDifficulty.shakeThreshold(ChallengeDifficulty.SHAKE_NORMAL)
        val hard = ChallengeDifficulty.shakeThreshold(ChallengeDifficulty.SHAKE_HARD)
        assertTrue(light < normal, "light ($light) should be < normal ($normal)")
        assertTrue(normal < hard, "normal ($normal) should be < hard ($hard)")
    }

    @Test
    fun `unknown shake sensitivity falls back to normal`() {
        assertEquals(
            ChallengeDifficulty.shakeThreshold(ChallengeDifficulty.SHAKE_NORMAL),
            ChallengeDifficulty.shakeThreshold(99)
        )
    }

    @Test
    fun `easy math stays within single-and-low-double digits`() {
        repeat(200) {
            val p = generateProblem(ChallengeDifficulty.MATH_EASY)
            assertTrue(p.operator == '+' || p.operator == '-', "easy should not use multiplication, got ${p.operator}")
            assertTrue(p.a <= 20 && p.b <= 20, "easy operands should be ≤ 20, got $p")
            assertTrue(p.answer >= 0, "easy answers must be ≥ 0, got $p → ${p.answer}")
        }
    }

    @Test
    fun `brutal math produces at least some two-digit multiplication`() {
        var sawTwoDigitMul = false
        repeat(400) {
            val p = generateProblem(ChallengeDifficulty.MATH_BRUTAL)
            if (p.operator == '*' && p.a >= 11 && p.b >= 11) sawTwoDigitMul = true
        }
        assertTrue(sawTwoDigitMul, "brutal difficulty should produce 2-digit × 2-digit multiplications within 400 draws")
    }

    @Test
    fun `math problem answer matches operator`() {
        val p = MathProblem(7, 5, '*')
        assertEquals(35, p.answer)
        val q = MathProblem(20, 3, '-')
        assertEquals(17, q.answer)
        val r = MathProblem(8, 9, '+')
        assertEquals(17, r.answer)
    }

    @Test
    fun `labels exist for all valid difficulty levels`() {
        listOf(
            ChallengeDifficulty.MATH_EASY,
            ChallengeDifficulty.MATH_HARD,
            ChallengeDifficulty.MATH_BRUTAL,
        ).forEach { assertTrue(ChallengeDifficulty.mathLabel(it).isNotBlank()) }
        listOf(
            ChallengeDifficulty.SHAKE_LIGHT,
            ChallengeDifficulty.SHAKE_NORMAL,
            ChallengeDifficulty.SHAKE_HARD,
        ).forEach { assertTrue(ChallengeDifficulty.shakeLabel(it).isNotBlank()) }
    }
}
