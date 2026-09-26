package com.pepperonas.brutus.update

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Reading the version out of the two places a release is announced (org.json → Robolectric). */
@RunWith(RobolectricTestRunner::class)
class ReleaseSourceTest {

    @Test
    fun `the product page's latest json yields its version`() {
        val body = """{"version":"v2.3.0","published":"2026-09-26T10:00:00Z","assets":[]}"""
        assertEquals("v2.3.0", ReleaseSource.versionFromSite(body))
    }

    @Test
    fun `the GitHub release yields its tag`() {
        val body = """{"tag_name":"v2.3.0","name":"v2.3.0 — something","draft":false}"""
        assertEquals("v2.3.0", ReleaseSource.versionFromGithub(body))
    }

    @Test
    fun `missing, blank or non-string fields yield nothing`() {
        assertNull(ReleaseSource.versionFromSite("""{"published":"x"}"""))
        assertNull(ReleaseSource.versionFromSite("""{"version":""}"""))
        assertNull(ReleaseSource.versionFromSite("""{"version":230}"""))
        assertNull(ReleaseSource.versionFromGithub("""{"name":"v2.3.0"}"""))
    }

    @Test
    fun `broken json yields nothing instead of throwing`() {
        listOf("", "not json", "[1,2]", "{\"version\":").forEach {
            assertNull(ReleaseSource.versionFromSite(it), "site: '$it'")
            assertNull(ReleaseSource.versionFromGithub(it), "github: '$it'")
        }
    }

    @Test
    fun `the page is asked first, GitHub only when the page fails`() {
        val asked = mutableListOf<String>()
        val source = ReleaseSource { url ->
            asked += url
            if (url == ReleaseSource.SITE_URL) """{"version":"v2.3.0"}""" else error("not reached")
        }
        assertEquals("v2.3.0", source.latestVersion())
        assertEquals(listOf(ReleaseSource.SITE_URL), asked)
    }

    @Test
    fun `an unreachable page falls back to GitHub`() {
        val asked = mutableListOf<String>()
        val source = ReleaseSource { url ->
            asked += url
            if (url == ReleaseSource.SITE_URL) null else """{"tag_name":"v2.4.0"}"""
        }
        assertEquals("v2.4.0", source.latestVersion())
        assertEquals(listOf(ReleaseSource.SITE_URL, ReleaseSource.GITHUB_URL), asked)
    }

    @Test
    fun `a page with garbage also falls back to GitHub`() {
        val source = ReleaseSource { url ->
            if (url == ReleaseSource.SITE_URL) "<html>502</html>" else """{"tag_name":"v2.4.0"}"""
        }
        assertEquals("v2.4.0", source.latestVersion())
    }

    @Test
    fun `both failing yields nothing`() {
        assertNull(ReleaseSource { null }.latestVersion())
    }

    @Test
    fun `the endpoints are the product page and the public GitHub API`() {
        assertEquals("https://brutus.celox.io/latest.json", ReleaseSource.SITE_URL)
        assertEquals("https://api.github.com/repos/pepperonas/brutus/releases/latest", ReleaseSource.GITHUB_URL)
        assertEquals("https://brutus.celox.io/download", ReleaseSource.DOWNLOAD_URL)
    }
}
