package com.pepperonas.brutus.util

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChallengeFlagsTest {

    @Test
    fun `has returns true only when flag bit is set`() {
        val combined = ChallengeFlags.MATH or ChallengeFlags.SHAKE
        assertTrue(ChallengeFlags.has(combined, ChallengeFlags.MATH))
        assertTrue(ChallengeFlags.has(combined, ChallengeFlags.SHAKE))
        assertFalse(ChallengeFlags.has(combined, ChallengeFlags.QR))
    }

    @Test
    fun `describe handles zero flags`() {
        assertEquals("Keine", ChallengeFlags.describe(0))
    }

    @Test
    fun `describe joins multiple flags`() {
        val flags = ChallengeFlags.MATH or ChallengeFlags.SHAKE or ChallengeFlags.QR
        assertEquals("Mathe + Schütteln + QR-Code", ChallengeFlags.describe(flags))
    }

    @Test
    fun `describe single flag`() {
        assertEquals("Schütteln", ChallengeFlags.describe(ChallengeFlags.SHAKE))
    }

    @Test
    fun `activeList preserves canonical order regardless of bit order`() {
        // QR=4, MATH=1, SHAKE=2 — order should always be MATH, SHAKE, QR
        val flags = ChallengeFlags.QR or ChallengeFlags.MATH
        assertEquals(listOf(ChallengeFlags.MATH, ChallengeFlags.QR), ChallengeFlags.activeList(flags))
    }

    @Test
    fun `activeList empty for zero`() {
        assertEquals(emptyList(), ChallengeFlags.activeList(0))
    }
}
