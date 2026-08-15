package com.pepperonas.brutus.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The global QR code is printed once and taped to a wall. If [GlobalQrStore.get]
 * ever returns a fresh value, every printed copy in the flat becomes worthless
 * and the user is locked out of dismissing their alarm — the single most
 * expensive bug this app could ship.
 */
@RunWith(RobolectricTestRunner::class)
class GlobalQrStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun storedValue(): String? = context
        .getSharedPreferences("brutus_global", Context.MODE_PRIVATE)
        .getString("qr_data", null)

    @Test
    fun `generated payload is the documented brutus-prefixed UUID`() {
        val data = QrGenerator.generateData()
        assertTrue(data.startsWith("brutus:"), "unexpected payload: $data")
        // Must parse as a UUID — the scanner does an exact string match, so the
        // payload has to be stable and collision-free.
        UUID.fromString(data.removePrefix("brutus:"))
        assertEquals(43, data.length, "payload length is documented as ~43 chars")
    }

    @Test
    fun `each generated payload is unique`() {
        val codes = List(50) { QrGenerator.generateData() }
        assertEquals(50, codes.toSet().size)
    }

    @Test
    fun `the first get generates and persists a code`() {
        assertEquals(null, storedValue())

        val code = GlobalQrStore.get(context)

        assertEquals(code, storedValue(), "the generated code must be written through")
        assertTrue(code.startsWith("brutus:"))
    }

    @Test
    fun `repeated reads return the very same code — printed copies stay valid`() {
        val first = GlobalQrStore.get(context)
        repeat(10) {
            assertEquals(first, GlobalQrStore.get(context))
        }
    }

    @Test
    fun `an existing code is honored instead of being regenerated`() {
        val seeded = "brutus:${UUID.randomUUID()}"
        context.getSharedPreferences("brutus_global", Context.MODE_PRIVATE)
            .edit().putString("qr_data", seeded).commit()

        assertEquals(seeded, GlobalQrStore.get(context))
    }

    @Test
    fun `a foreign payload does not match the installation code`() {
        val mine = GlobalQrStore.get(context)
        // Somebody else's printout — the scanner compares the exact string.
        assertNotEquals(mine, "brutus:${UUID.randomUUID()}")
    }
}
