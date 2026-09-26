package com.pepperonas.brutus.update

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Deciding "is the release newer than what is installed" — a wrong answer nags or stays silent. */
class AppVersionTest {

    @Test
    fun `a tag with a leading v parses like the plain version name`() {
        assertEquals(listOf(2, 2, 0), AppVersion.parse("v2.2.0"))
        assertEquals(listOf(2, 2, 0), AppVersion.parse("2.2.0"))
        assertEquals(listOf(2, 2, 0), AppVersion.parse("  V2.2.0 "))
    }

    @Test
    fun `pre-release and build suffixes are ignored`() {
        assertEquals(listOf(2, 3, 0), AppVersion.parse("v2.3.0-beta.1"))
        assertEquals(listOf(2, 3, 0), AppVersion.parse("2.3.0+42"))
    }

    @Test
    fun `garbage does not parse`() {
        listOf("", "v", "latest", "2..0", "2.x.0", "-1.0", "v2.2.0 extra").forEach {
            assertNull(AppVersion.parse(it), "'$it' must not parse")
        }
    }

    @Test
    fun `parts compare as numbers, not as text`() {
        assertTrue(AppVersion.isNewer("v2.10.0", "2.9.1"))
        assertFalse(AppVersion.isNewer("v2.9.1", "2.10.0"))
    }

    @Test
    fun `the same version is not newer`() {
        assertFalse(AppVersion.isNewer("v2.2.0", "2.2.0"))
        assertFalse(AppVersion.isNewer("v2.2", "2.2.0"), "a missing part counts as 0")
    }

    @Test
    fun `patch, minor and major bumps are newer`() {
        assertTrue(AppVersion.isNewer("v2.2.1", "2.2.0"))
        assertTrue(AppVersion.isNewer("v2.3.0", "2.2.9"))
        assertTrue(AppVersion.isNewer("v3.0.0", "2.99.99"))
        assertTrue(AppVersion.isNewer("v2.2.0.1", "2.2.0"))
    }

    @Test
    fun `an unparsable side is never newer`() {
        assertFalse(AppVersion.isNewer("latest", "2.2.0"))
        assertFalse(AppVersion.isNewer("v9.0.0", "unknown"))
        assertFalse(AppVersion.isNewer(null, "2.2.0"))
    }

    @Test
    fun `display drops the leading v`() {
        assertEquals("2.3.0", AppVersion.display("v2.3.0"))
        assertEquals("2.3.0", AppVersion.display("2.3.0"))
    }
}
