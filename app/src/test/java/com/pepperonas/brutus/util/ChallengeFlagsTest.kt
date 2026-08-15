package com.pepperonas.brutus.util

import android.content.Context
import com.pepperonas.brutus.LocaleContexts
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ChallengeFlagsTest {

    private lateinit var en: Context
    private lateinit var de: Context

    @Before
    fun setUp() {
        en = LocaleContexts.english()
        de = LocaleContexts.german()
    }

    @Test
    fun `has returns true only when flag bit is set`() {
        val combined = ChallengeFlags.MATH or ChallengeFlags.SHAKE
        assertTrue(ChallengeFlags.has(combined, ChallengeFlags.MATH))
        assertTrue(ChallengeFlags.has(combined, ChallengeFlags.SHAKE))
        assertFalse(ChallengeFlags.has(combined, ChallengeFlags.QR))
    }

    @Test
    fun `describe handles zero flags`() {
        assertEquals("None", ChallengeFlags.describe(en, 0))
        assertEquals("Keine", ChallengeFlags.describe(de, 0))
    }

    @Test
    fun `describe never returns an empty label for an unknown bit`() {
        // A value from a future version (or a corrupt row) must not render an
        // empty chip on the alarm card.
        assertEquals("None", ChallengeFlags.describe(en, 1 shl 5))
        assertEquals("Keine", ChallengeFlags.describe(de, 1 shl 5))
    }

    @Test
    fun `describe joins multiple flags`() {
        val flags = ChallengeFlags.MATH or ChallengeFlags.SHAKE or ChallengeFlags.QR
        assertEquals("Math + Shake + QR code", ChallengeFlags.describe(en, flags))
        assertEquals("Mathe + Schütteln + QR-Code", ChallengeFlags.describe(de, flags))
    }

    @Test
    fun `describe single flag`() {
        assertEquals("Shake", ChallengeFlags.describe(en, ChallengeFlags.SHAKE))
        assertEquals("Schütteln", ChallengeFlags.describe(de, ChallengeFlags.SHAKE))
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

    @Test
    fun `sanitize maps zero to MATH and passes everything else through`() {
        assertEquals(ChallengeFlags.MATH, ChallengeFlags.sanitize(0))
        assertEquals(ChallengeFlags.SHAKE, ChallengeFlags.sanitize(ChallengeFlags.SHAKE))
        val combined = ChallengeFlags.MATH or ChallengeFlags.QR
        assertEquals(combined, ChallengeFlags.sanitize(combined))
    }
}
