package com.pepperonas.brutus.viewmodel

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.data.AlarmDatabase
import com.pepperonas.brutus.data.AlarmEntity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The alarm list must tell "not loaded yet" apart from "no alarms": with an empty list as the starting
 * value, a cold start showed *No alarms yet — Create alarm* for a moment although alarms existed.
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AlarmViewModelTest {

    private lateinit var main: ExecutorCoroutineDispatcher
    private lateinit var app: Application

    @Before
    fun setUp() {
        main = newSingleThreadContext("vm-main")
        Dispatchers.setMain(main)
        app = ApplicationProvider.getApplicationContext()
        kotlin.concurrent.thread { (app as BrutusApplication).database.clearAllTables() }.join()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        main.close()
        AlarmDatabase.resetInstanceForTests()
    }

    @Test
    fun `before the database answers the list is unknown, not empty`() {
        assertNull(AlarmViewModel(app).alarms.value)
    }

    @Test
    fun `an empty database becomes an empty list, not null`() {
        val vm = AlarmViewModel(app)
        val loaded = runBlocking { withTimeout(5_000) { vm.alarms.first { it != null } } }
        assertEquals(emptyList(), loaded)
    }

    @Test
    fun `stored alarms arrive as the first loaded value`() {
        runBlocking { (app as BrutusApplication).database.alarmDao().insert(AlarmEntity(hour = 6, minute = 30, label = "Work")) }
        val vm = AlarmViewModel(app)
        val loaded = runBlocking { withTimeout(5_000) { vm.alarms.first { it != null } } }
        assertEquals(listOf("Work"), loaded!!.map { it.label })
    }
}
