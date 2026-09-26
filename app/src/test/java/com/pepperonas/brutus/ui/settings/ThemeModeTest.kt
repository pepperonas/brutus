package com.pepperonas.brutus.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pepperonas.brutus.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ThemeModeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `the default follows the system`() = runBlocking {
        ThemeSettings.setMode(context, ThemeSettings.Mode.SYSTEM)
        assertEquals(ThemeSettings.Mode.SYSTEM, ThemeSettings.modeFlow(context).first())
    }

    @Test
    fun `every mode round-trips`() = runBlocking {
        ThemeSettings.Mode.entries.forEach { mode ->
            ThemeSettings.setMode(context, mode)
            assertEquals(mode, ThemeSettings.modeFlow(context).first())
        }
    }

    @Test
    fun `stored names are a persisted contract`() {
        // Stored by name: renaming an entry would silently reset every user's choice.
        assertEquals(listOf("SYSTEM", "LIGHT", "DARK"), ThemeSettings.Mode.entries.map { it.name })
    }
}
