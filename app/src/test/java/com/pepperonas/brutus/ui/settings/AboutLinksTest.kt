package com.pepperonas.brutus.ui.settings

import org.junit.Test
import java.io.File
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What the About section shows and opens — pinned, so a typo cannot send users somewhere else. */
class AboutLinksTest {

    @Test
    fun `the links point at Brutus, its repository and its licence`() {
        assertEquals("https://brutus.celox.io", AboutLinks.PRODUCT_URL)
        assertEquals("https://github.com/pepperonas/brutus", AboutLinks.REPO_URL)
        assertEquals("https://github.com/pepperonas/brutus/blob/main/LICENSE", AboutLinks.LICENSE_URL)
        assertEquals("https://celox.io", AboutLinks.WEBSITE_URL)
    }

    @Test
    fun `the licence named in the app is the licence in the repository`() {
        val license = File("../LICENSE").readText()
        assertTrue(license.startsWith("MIT License"), "LICENSE is no longer MIT — update AboutLinks")
        assertEquals("MIT License", AboutLinks.LICENSE_NAME)
    }

    @Test
    fun `the donate link goes to PayPal with the right recipient, currency and note`() {
        val uri = URI(AboutLinks.donateUrl())
        assertEquals("www.paypal.com", uri.host)
        val query = uri.rawQuery.split('&').associate { it.substringBefore('=') to it.substringAfter('=') }
        assertEquals("martin.pfeffer@celox.io", query["business"])
        assertEquals("EUR", query["currency_code"])
        assertEquals("Brutus", query["item_name"])
    }

    @Test
    fun `a note with spaces is encoded as %20, not as +`() {
        assertTrue(AboutLinks.donateUrl("Brutus Alarm").endsWith("item_name=Brutus%20Alarm"))
    }

    @Test
    fun `the bundled font licence is the one in THIRD_PARTY_LICENSES`() {
        // The app shows res/raw/space_grotesk_ofl.txt; the repo keeps the canonical copy.
        // Line endings are Git's business (the canonical copy is CRLF, checkouts may normalize).
        fun text(path: String) = File(path).readText().replace("\r\n", "\n")
        assertEquals(
            text("../THIRD_PARTY_LICENSES/SpaceGrotesk-OFL.txt"),
            text("src/main/res/raw/space_grotesk_ofl.txt"),
        )
    }
}
