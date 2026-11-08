package com.pepperonas.brutus.tile

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.data.AlarmDatabase
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.scheduler.AlarmActions
import com.pepperonas.brutus.ui.screens.HomeRequest
import com.pepperonas.brutus.util.RingingStore
import com.pepperonas.brutus.util.Storage
import com.pepperonas.brutus.util.VacationStore
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlarmManager
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class VacationModeTest {

    private lateinit var context: Context
    private lateinit var db: AlarmDatabase
    private val day = 24 * 3_600_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        AlarmDatabase.resetInstanceForTests()
        db = AlarmDatabase.getInstance(context)
        Storage.PREFERENCE_FILES.forEach { Storage.prefs(context, it).edit().clear().commit() }
    }

    private fun clockTrigger(): Long =
        Shadows.shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms
            .filter { it.type == AlarmManager.RTC_WAKEUP && it.showIntent != null }
            .minOf { it.triggerAtTime }

    @Test
    fun `pausing re-arms AlarmManager for the day back, resuming brings the normal time back`() = runBlocking {
        db.alarmDao().insert(AlarmEntity(hour = 7, minute = 0, repeatDays = 0x7F))
        AlarmActions.resumeFromVacation(context) // arms normally
        val normal = clockTrigger()
        assertTrue(normal - System.currentTimeMillis() <= day)

        val lastDay = System.currentTimeMillis() + 5 * day
        AlarmActions.pauseThrough(context, lastDay)
        val paused = clockTrigger()
        assertTrue(paused >= VacationStore.startOfNextDay(lastDay), "rang during the vacation")
        assertEquals(paused, RingingStore.expected(context).values.single(), "missed-alarm bookkeeping follows")

        AlarmActions.resumeFromVacation(context)
        assertNull(VacationStore.pausedUntil(context))
        assertEquals(normal, clockTrigger())
    }

    @Test
    fun `a pause that has run out reads as off`() {
        VacationStore.pauseThrough(context, System.currentTimeMillis() - 3 * day)
        assertNull(VacationStore.pausedUntil(context))
        VacationStore.pauseThrough(context, System.currentTimeMillis())
        assertNotNull(VacationStore.pausedUntil(context), "today counts as a vacation day")
    }

    @Test
    fun `each launcher shortcut carries a request MainActivity understands`() {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/xml/shortcuts.xml"))
        val intents = doc.getElementsByTagName("intent")
        val actions = (0 until intents.length).map { intents.item(it) as org.w3c.dom.Element }.map {
            assertEquals("com.pepperonas.brutus", it.getAttribute("android:targetPackage"))
            assertEquals("com.pepperonas.brutus.MainActivity", it.getAttribute("android:targetClass"))
            it.getAttribute("android:action")
        }
        assertEquals(HomeRequest.entries.map { it.action }.toSet(), actions.toSet())
        actions.forEach { assertNotNull(HomeRequest.from(Intent(it)), it) }
        assertNull(HomeRequest.from(Intent(Intent.ACTION_MAIN)))
        assertNull(HomeRequest.from(null))
    }

    @Test
    fun `the tile is declared with the quick-settings permission and action`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val block = manifest.substringAfter(".tile.VacationTileService").substringBefore("</service>")
        assertTrue("android.permission.BIND_QUICK_SETTINGS_TILE" in block)
        assertTrue("android.service.quicksettings.action.QS_TILE" in block)
        assertTrue("@xml/shortcuts" in manifest)
    }

    @Test
    fun `asking the tile to redraw never throws, even when it was never added`() {
        VacationTileService.requestUpdate(context)
    }
}
