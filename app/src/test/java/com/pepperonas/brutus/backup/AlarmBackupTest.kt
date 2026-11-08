package com.pepperonas.brutus.backup

import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.ChallengeFlags
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Robolectric only for org.json, which is a stub on the plain JVM. */
@RunWith(RobolectricTestRunner::class)
class AlarmBackupTest {

    private val full = AlarmEntity(
        id = 42, hour = 6, minute = 45, enabled = false, label = "Gym", repeatDays = 0b0010101,
        challengeFlags = ChallengeFlags.MATH or ChallengeFlags.QR, snoozeDuration = 10,
        soundId = AlarmSound.KLAXON.id, mathProblemCount = 7, shakeCount = 55, hardcoreMode = true,
        ultraHardcoreMode = true, mathDifficulty = 2, shakeSensitivity = 0, sunriseEnabled = true,
    )
    private val qr = "brutus:123e4567-e89b-12d3-a456-426614174000"

    @Test
    fun `every alarm field survives a round trip - only the id is not carried`() {
        val text = AlarmBackup.encode(listOf(full), qr, AlarmBackup.Settings(30, 15, 60, AlarmSound.SUNRISE.id), 0)
        val back = AlarmBackup.decode(text)
        assertEquals(listOf(full.copy(id = 0)), back.alarms)
        assertEquals(qr, back.qrData)
        assertEquals(AlarmBackup.Settings(30, 15, 60, AlarmSound.SUNRISE.id), back.settings)
        assertEquals(0, back.skipped)
    }

    @Test
    fun `the file names every field of AlarmEntity except the id`() {
        // Adding a field to AlarmEntity without exporting it would silently lose it on import.
        val names = AlarmEntity::class.java.declaredFields.map { it.name }
            .filter { !it.contains('$') && it != "id" && it != "Companion" }.toSet()
        val alarm = JSONObject(AlarmBackup.encode(listOf(full), null, AlarmBackup.Settings(), 0))
            .getJSONArray("alarms").getJSONObject(0)
        assertEquals(names, alarm.keys().asSequence().toSet())
    }

    @Test
    fun `missing fields take today's defaults`() {
        val back = AlarmBackup.decode("""{"format":"brutus-backup","version":1,"alarms":[{"hour":7,"minute":30}]}""")
        assertEquals(listOf(AlarmEntity(hour = 7, minute = 30)), back.alarms)
        assertNull(back.qrData)
        assertEquals(AlarmBackup.Settings(), back.settings)
    }

    @Test
    fun `values outside the offer are clamped, retired sounds get their successor`() {
        val retired = AlarmSound.RETIRED.entries.first()
        val back = AlarmBackup.decode(
            """{"format":"brutus-backup","version":1,"alarms":[{"hour":7,"minute":0,"repeatDays":255,
               "challengeFlags":0,"snoozeDuration":999,"soundId":${retired.key},"mathProblemCount":50,
               "shakeCount":1,"mathDifficulty":9,"shakeSensitivity":-3,"label":"${"x".repeat(300)}"}]}"""
        ).alarms.single()
        assertEquals(0x7F, back.repeatDays)
        assertEquals(ChallengeFlags.MATH, back.challengeFlags)
        assertEquals(60, back.snoozeDuration)
        assertEquals(retired.value.id, back.soundId)
        assertEquals(10, back.mathProblemCount)
        assertEquals(10, back.shakeCount)
        assertEquals(2, back.mathDifficulty)
        assertEquals(0, back.shakeSensitivity)
        assertEquals(100, back.label.length)
    }

    @Test
    fun `alarms with an impossible time are dropped and counted, the rest still comes in`() {
        val back = AlarmBackup.decode(
            """{"format":"brutus-backup","version":2,"futureField":true,
               "alarms":[{"hour":24,"minute":0},{"hour":7,"minute":60},{"minute":5},"nope",{"hour":8,"minute":0}]}"""
        )
        assertEquals(listOf(8), back.alarms.map { it.hour })
        assertEquals(4, back.skipped)
    }

    @Test
    fun `anything that is not a Brutus backup is refused`() {
        listOf("", "hello", "[]", """{"alarms":[]}""", """{"format":"other","version":1}""",
            """{"format":"brutus-backup","version":0}""")
            .forEach { assertFailsWith<AlarmBackup.FormatException>(it) { AlarmBackup.decode(it) } }
    }

    @Test
    fun `importing the same alarms twice does not double them`() {
        val existing = listOf(full.copy(id = 1))
        val incoming = listOf(full, full.copy(label = " Gym "), full.copy(hour = 9), full.copy(hour = 9))
        val fresh = AlarmBackup.newAlarms(existing, incoming)
        assertEquals(listOf(9), fresh.map { it.hour })
        assertEquals(0L, fresh.single().id)
    }

    @Test
    fun `the suggested file name carries the date`() {
        val name = AlarmBackup.suggestedFileName(0)
        kotlin.test.assertTrue(Regex("""brutus-backup-\d{4}-\d{2}-\d{2}\.json""").matches(name), name)
    }
}
