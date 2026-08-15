package com.pepperonas.brutus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.ChallengeFlags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The persistence layer against a real (in-memory) Room database, i.e. against
 * the generated implementation and the actual SQL — not a mock. That makes this
 * also a schema test: Room verifies the entity against the database it opens, so
 * a column added without a migration fails right here.
 */
@RunWith(RobolectricTestRunner::class)
class AlarmDaoTest {

    private lateinit var db: AlarmDatabase
    private lateinit var dao: AlarmDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AlarmDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.alarmDao()
    }

    @After
    fun tearDown() = db.close()

    private fun alarm(
        hour: Int,
        minute: Int = 0,
        enabled: Boolean = true,
        label: String = "",
    ) = AlarmEntity(hour = hour, minute = minute, enabled = enabled, label = label)

    @Test
    fun `an empty database yields an empty list, never null`() = runTest {
        assertEquals(emptyList(), dao.getAllAlarms().first())
        assertEquals(emptyList(), dao.getEnabledAlarms())
    }

    @Test
    fun `insert returns a generated id that reads back`() = runTest {
        val id = dao.insert(alarm(7, 30, label = "Arbeit"))

        assertTrue(id > 0, "Room must auto-generate a primary key")
        val loaded = dao.getById(id)
        assertEquals("Arbeit", loaded?.label)
        assertEquals(7, loaded?.hour)
        assertEquals(30, loaded?.minute)
    }

    @Test
    fun `generated ids are distinct so alarms never overwrite each other`() = runTest {
        val ids = listOf(alarm(6), alarm(7), alarm(8)).map { dao.insert(it) }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `getById returns null for an id that was never stored`() = runTest {
        dao.insert(alarm(7))
        assertNull(dao.getById(9999L))
    }

    @Test
    fun `the list is ordered by hour and then minute — the UI does not sort again`() = runTest {
        dao.insert(alarm(9, 5))
        dao.insert(alarm(6, 45))
        dao.insert(alarm(6, 5))
        dao.insert(alarm(23, 59))

        val order = dao.getAllAlarms().first().map { it.hour to it.minute }
        assertEquals(listOf(6 to 5, 6 to 45, 9 to 5, 23 to 59), order)
    }

    @Test
    fun `only enabled alarms are handed to the boot receiver`() = runTest {
        dao.insert(alarm(6, enabled = true, label = "on"))
        dao.insert(alarm(7, enabled = false, label = "off"))
        dao.insert(alarm(8, enabled = true, label = "on too"))

        val labels = dao.getEnabledAlarms().map { it.label }.toSet()
        assertEquals(setOf("on", "on too"), labels)
    }

    @Test
    fun `every configurable field survives a write-read round-trip`() = runTest {
        val full = AlarmEntity(
            hour = 5,
            minute = 55,
            enabled = false,
            label = "Frühschicht",
            repeatDays = 0x7F,
            challengeFlags = ChallengeFlags.MATH or ChallengeFlags.SHAKE or ChallengeFlags.QR,
            snoozeDuration = 15,
            soundId = AlarmSound.BANSHEE.id,
            mathProblemCount = 10,
            shakeCount = 100,
            hardcoreMode = true,
            ultraHardcoreMode = true,
            mathDifficulty = 2,
            shakeSensitivity = 2,
            sunriseEnabled = true,
        )

        val id = dao.insert(full)

        assertEquals(full.copy(id = id), dao.getById(id))
    }

    @Test
    fun `update rewrites the stored row in place`() = runTest {
        val id = dao.insert(alarm(7, 0, label = "alt"))
        val stored = dao.getById(id)!!

        dao.update(stored.copy(hour = 9, minute = 15, label = "neu", hardcoreMode = true))

        val updated = dao.getById(id)!!
        assertEquals(9 to 15, updated.hour to updated.minute)
        assertEquals("neu", updated.label)
        assertTrue(updated.hardcoreMode)
        assertEquals(1, dao.getAllAlarms().first().size, "update must not insert a copy")
    }

    @Test
    fun `setEnabled toggles exactly one row`() = runTest {
        val a = dao.insert(alarm(6, label = "a"))
        val b = dao.insert(alarm(7, label = "b"))

        dao.setEnabled(a, false)

        assertEquals(false, dao.getById(a)!!.enabled)
        assertEquals(true, dao.getById(b)!!.enabled)
    }

    @Test
    fun `setEnabled on a missing id changes nothing`() = runTest {
        val id = dao.insert(alarm(6))
        dao.setEnabled(9999L, false)
        assertEquals(true, dao.getById(id)!!.enabled)
    }

    @Test
    fun `delete removes only the targeted alarm`() = runTest {
        val keep = dao.insert(alarm(6, label = "keep"))
        val drop = dao.insert(alarm(7, label = "drop"))

        dao.delete(dao.getById(drop)!!)

        assertNull(dao.getById(drop))
        assertEquals(listOf(keep), dao.getAllAlarms().first().map { it.id })
    }

    @Test
    fun `re-inserting a known id replaces the row instead of duplicating it`() = runTest {
        // OnConflictStrategy.REPLACE — this is what the undo path relies on.
        val id = dao.insert(alarm(7, label = "erste"))
        dao.insert(alarm(7, label = "zweite").copy(id = id))

        val all = dao.getAllAlarms().first()
        assertEquals(1, all.size)
        assertEquals("zweite", all.single().label)
    }

    @Test
    fun `restoring a deleted alarm with id 0 yields a fresh row`() = runTest {
        // The undo snackbar re-inserts with id = 0 so Room hands out a new key.
        val original = dao.getById(dao.insert(alarm(7, label = "undo mich")))!!
        dao.delete(original)

        val restoredId = dao.insert(original.copy(id = 0))

        assertTrue(restoredId > 0)
        assertEquals("undo mich", dao.getById(restoredId)!!.label)
        assertEquals(1, dao.getAllAlarms().first().size)
    }

    @Test
    fun `the alarm flow reflects writes without being re-subscribed`() = runTest {
        assertEquals(0, dao.getAllAlarms().first().size)

        dao.insert(alarm(7))
        assertEquals(1, dao.getAllAlarms().first().size)

        dao.insert(alarm(8))
        assertEquals(2, dao.getAllAlarms().first().size)
    }

    @Test
    fun `the repository passes through to the dao`() = runTest {
        val repo = AlarmRepository(dao)

        val id = repo.insert(alarm(6, 30, label = "repo"))
        assertEquals("repo", repo.getById(id)?.label)

        repo.setEnabled(id, false)
        assertTrue(repo.getEnabledAlarms().isEmpty())
        assertEquals(1, repo.allAlarms.first().size)

        repo.update(repo.getById(id)!!.copy(label = "geändert"))
        assertEquals("geändert", repo.getById(id)?.label)

        repo.delete(repo.getById(id)!!)
        assertTrue(repo.allAlarms.first().isEmpty())
    }
}
