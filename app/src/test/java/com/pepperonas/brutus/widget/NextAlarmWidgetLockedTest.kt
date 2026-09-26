package com.pepperonas.brutus.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Before the first unlock after a reboot, every AppWidgetManager call throws
 * "User 0 must be unlocked for widgets to be available". The ringing service refreshes the widget
 * right after an alarm fires — on the emulator that exception killed the service a second after
 * the alarm started, and the alarm went silent. Robolectric does not throw, so the test pins the
 * guard itself: while locked, the widget is not touched at all.
 */
@RunWith(RobolectricTestRunner::class)
class NextAlarmWidgetLockedTest {

    private lateinit var context: Context
    private var before = 0

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(AppWidgetManager.getInstance(context)).createWidget(NextAlarmWidget::class.java, R.layout.widget_next_alarm)
        before = allUpdates().size // createWidget announces itself; count only what refresh adds
    }

    private fun allUpdates() = shadowOf(context as android.app.Application).broadcastIntents
        .filter { it.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE }

    private fun widgetBroadcasts() = allUpdates().drop(before)

    @Test
    fun `unlocked, a refresh reaches the widget`() {
        NextAlarmWidget.refresh(context)
        assertEquals(1, widgetBroadcasts().size)
    }

    @Test
    fun `locked, a refresh does nothing instead of throwing`() {
        shadowOf(context.getSystemService(UserManager::class.java)).setUserUnlocked(false)
        NextAlarmWidget.refresh(context)
        assertTrue(widgetBroadcasts().isEmpty())
    }
}
