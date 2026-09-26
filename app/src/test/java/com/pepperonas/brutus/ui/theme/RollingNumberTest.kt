package com.pepperonas.brutus.ui.theme

import org.junit.Test
import kotlin.test.assertEquals

/** Steppers roll like a mechanical counter — the direction must follow the change. */
class RollingNumberTest {

    @Test
    fun `counting up rolls the new value in from below`() {
        assertEquals(1, rollDirection(from = 4, to = 5))
    }

    @Test
    fun `counting down rolls it in from above`() {
        assertEquals(-1, rollDirection(from = 5, to = 4))
    }
}
