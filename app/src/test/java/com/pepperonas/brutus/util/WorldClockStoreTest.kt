package com.pepperonas.brutus.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * SharedPreferences round-trip behavior of the world-clock zone store.
 * Robolectric gives every test a fresh Application, so each test starts
 * from an empty preferences file.
 */
@RunWith(RobolectricTestRunner::class)
class WorldClockStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `first load seeds and returns the default zones`() {
        val zones = WorldClockStore.load(context)
        assertEquals(listOf("Europe/Berlin", "America/New_York", "Asia/Tokyo"), zones)
        // The defaults were persisted, not just returned — a second load
        // reads them back from the preferences file.
        assertEquals(zones, WorldClockStore.load(context))
    }

    @Test
    fun `save and load round-trip preserves order`() {
        val zones = listOf("Asia/Tokyo", "Europe/Berlin", "Pacific/Auckland")
        WorldClockStore.save(context, zones)
        assertEquals(zones, WorldClockStore.load(context))
    }

    @Test
    fun `saving an empty list sticks — no silent fallback to defaults`() {
        WorldClockStore.load(context)                 // seed defaults
        WorldClockStore.save(context, emptyList())    // user removed every zone
        assertTrue(WorldClockStore.load(context).isEmpty())
    }

    @Test
    fun `blank entries are filtered out on load`() {
        WorldClockStore.save(context, listOf("Europe/Berlin", "", "Asia/Tokyo"))
        assertEquals(listOf("Europe/Berlin", "Asia/Tokyo"), WorldClockStore.load(context))
    }

    @Test
    fun `a second save overwrites the previous zone list`() {
        WorldClockStore.save(context, listOf("Europe/Berlin"))
        WorldClockStore.save(context, listOf("America/New_York"))
        assertEquals(listOf("America/New_York"), WorldClockStore.load(context))
    }

    @Test
    fun `multi-segment zone ids with underscores survive the round-trip`() {
        val zones = listOf("America/Argentina/Buenos_Aires", "America/North_Dakota/New_Salem")
        WorldClockStore.save(context, zones)
        assertEquals(zones, WorldClockStore.load(context))
    }
}
