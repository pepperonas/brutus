package com.pepperonas.brutus.data

import com.pepperonas.brutus.LocaleContexts
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.ChallengeFlags
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The constructor defaults are a persisted contract: they are what every row
 * created by the "+" button gets, and what a Room migration backfills into
 * existing rows (`DEFAULT 0` in the ALTER TABLE statements). Silently changing
 * one of them changes alarms that users already created.
 */
@RunWith(RobolectricTestRunner::class)
class AlarmEntityDefaultsTest {

    private val fresh = AlarmEntity(hour = 6, minute = 30)

    @Test
    fun `a fresh alarm is enabled, one-shot and unlabelled`() {
        assertTrue(fresh.enabled)
        assertEquals(0, fresh.repeatDays)
        assertEquals("", fresh.label)
        assertEquals(0L, fresh.id, "id must stay 0 so Room auto-generates it")
    }

    @Test
    fun `a fresh alarm starts with the math challenge only`() {
        assertEquals(ChallengeFlags.MATH, fresh.challengeFlags)
        assertEquals(listOf(ChallengeFlags.MATH), ChallengeFlags.activeList(fresh.challengeFlags))
    }

    @Test
    fun `challenge and difficulty defaults match the UI copy`() {
        assertEquals(3, fresh.mathProblemCount)
        assertEquals(30, fresh.shakeCount)
        assertEquals(1, fresh.mathDifficulty, "1 = 'Hart', the documented legacy default")
        assertEquals(1, fresh.shakeSensitivity, "1 = 'Normal'")
        assertEquals(5, fresh.snoozeDuration)
    }

    @Test
    fun `the brutal opt-ins are all off by default`() {
        assertFalse(fresh.hardcoreMode)
        assertFalse(fresh.ultraHardcoreMode)
        assertFalse(fresh.sunriseEnabled)
        assertFalse(fresh.hardcoreEffective)
    }

    @Test
    fun `soundName falls back instead of crashing on a corrupt stored id`() {
        val en = LocaleContexts.english()
        assertEquals(en.getString(AlarmSound.SYSTEM.labelRes), fresh.copy(soundId = 4711).soundName(en))
    }

    @Test
    fun `challengeName describes the stored flag combination`() {
        val all = fresh.copy(
            challengeFlags = ChallengeFlags.MATH or ChallengeFlags.SHAKE or ChallengeFlags.QR
        )
        assertEquals("Math + Shake + QR code", all.challengeName(LocaleContexts.english()))
        assertEquals("Mathe + Schütteln + QR-Code", all.challengeName(LocaleContexts.german()))
    }

    @Test
    fun `hardcoreEffective is true when either switch is on`() {
        assertTrue(fresh.copy(hardcoreMode = true).hardcoreEffective)
        assertTrue(fresh.copy(ultraHardcoreMode = true).hardcoreEffective)
        assertTrue(fresh.copy(hardcoreMode = true, ultraHardcoreMode = true).hardcoreEffective)
    }
}
