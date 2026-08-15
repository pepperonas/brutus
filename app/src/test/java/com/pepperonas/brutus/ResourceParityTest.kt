package com.pepperonas.brutus

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Structural guards for the two shipped languages.
 *
 * Translations rot silently: a key gets added to `values/` and forgotten in
 * `values-de/`, a `%1$s` disappears in one language (→ `IllegalFormatException`
 * at runtime, in that language only, on a user's phone), a plural loses its
 * `other` item. None of that is a compile error, and lint's `MissingTranslation`
 * only covers the first case. So the resource files are diffed here directly.
 */
@RunWith(RobolectricTestRunner::class)
class ResourceParityTest {

    private val resDir: File = listOf(File("src/main/res"), File("app/src/main/res"))
        .firstOrNull { it.isDirectory }
        ?: error("res/ not found (cwd=${File("").absolutePath})")

    private data class Bundle(
        val strings: Map<String, String>,
        val plurals: Map<String, Map<String, String>>,
        val arrays: Map<String, List<String>>,
    )

    private fun parse(path: String): Bundle {
        val file = File(resDir, path)
        assertTrue(file.isFile, "missing resource file $path")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = doc.documentElement

        val strings = mutableMapOf<String, String>()
        val plurals = mutableMapOf<String, Map<String, String>>()
        val arrays = mutableMapOf<String, List<String>>()

        val children = root.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i) as? Element ?: continue
            val name = node.getAttribute("name")
            when (node.tagName) {
                "string" -> strings[name] = node.textContent
                "plurals" -> {
                    val items = mutableMapOf<String, String>()
                    val itemNodes = node.getElementsByTagName("item")
                    for (j in 0 until itemNodes.length) {
                        val item = itemNodes.item(j) as Element
                        items[item.getAttribute("quantity")] = item.textContent
                    }
                    plurals[name] = items
                }
                "string-array" -> {
                    val items = mutableListOf<String>()
                    val itemNodes = node.getElementsByTagName("item")
                    for (j in 0 until itemNodes.length) items += itemNodes.item(j).textContent
                    arrays[name] = items
                }
            }
        }
        return Bundle(strings, plurals, arrays)
    }

    private val en by lazy { parse("values/strings.xml") }
    private val de by lazy { parse("values-de/strings.xml") }

    /** Android format specifiers, e.g. %1$s / %2$d / %d. */
    private val specifierPattern = Regex("""%(\d+\$)?[a-zA-Z]""")

    private fun specifiers(text: String): List<String> =
        specifierPattern.findAll(text).map { it.value }.sorted().toList()

    // ---- key coverage ----------------------------------------------------

    @Test
    fun `every English string has a German translation`() {
        val missing = en.strings.keys.filterNot { it == "app_name" } - de.strings.keys
        assertTrue(missing.isEmpty(), "not translated to German: ${missing.sorted()}")
    }

    @Test
    fun `the German file has no keys the default set does not know`() {
        val orphaned = de.strings.keys - en.strings.keys
        assertTrue(orphaned.isEmpty(), "German-only keys (typo? renamed?): ${orphaned.sorted()}")
    }

    @Test
    fun `plurals and string arrays exist in both languages`() {
        assertEquals(en.plurals.keys, de.plurals.keys, "plural sets differ")
        assertEquals(en.arrays.keys, de.arrays.keys, "string-array sets differ")
    }

    // ---- content sanity --------------------------------------------------

    @Test
    fun `no resource is blank in either language`() {
        (en.strings + de.strings).forEach { (key, value) ->
            assertTrue(value.isNotBlank(), "$key is blank")
        }
        (en.plurals + de.plurals).forEach { (key, items) ->
            items.forEach { (quantity, value) ->
                assertTrue(value.isNotBlank(), "$key[$quantity] is blank")
            }
        }
    }

    @Test
    fun `format specifiers match between the languages`() {
        // A translation that drops a %1$s crashes with IllegalFormatException —
        // and only for users in that language.
        en.strings.forEach { (key, value) ->
            val translated = de.strings[key] ?: return@forEach
            assertEquals(
                specifiers(value), specifiers(translated),
                "format specifiers differ for '$key'"
            )
        }
    }

    @Test
    fun `plural items carry the same placeholders in both languages`() {
        en.plurals.forEach { (key, items) ->
            val translated = de.plurals.getValue(key)
            items.forEach { (quantity, value) ->
                val other = translated[quantity]
                    ?: error("German plural '$key' has no '$quantity' item")
                assertEquals(
                    specifiers(value), specifiers(other),
                    "placeholders differ for plural '$key' [$quantity]"
                )
            }
        }
    }

    @Test
    fun `every plural declares at least the one and other quantities`() {
        (en.plurals + de.plurals).forEach { (key, items) ->
            assertTrue("one" in items, "$key is missing the 'one' form")
            assertTrue("other" in items, "$key is missing the 'other' form")
        }
    }

    @Test
    fun `weekday arrays are seven entries long in both languages`() {
        assertEquals(7, en.arrays.getValue("weekday_short").size)
        assertEquals(7, de.arrays.getValue("weekday_short").size)
    }

    @Test
    fun `the default resource set contains no German leftovers`() {
        // Umlauts in values/ are the fingerprint of a string that was copied
        // over but never translated.
        val germanLooking = en.strings.filterValues { it.any { c -> c in "äöüßÄÖÜ" } }
        assertTrue(
            germanLooking.isEmpty(),
            "untranslated German in the default (English) file: ${germanLooking.keys}"
        )
    }

    @Test
    fun `the German file actually differs from the default`() {
        // Catches a values-de/ that was copied from values/ and never edited.
        val identical = en.strings.count { (key, value) -> de.strings[key] == value }
        assertTrue(
            identical < en.strings.size / 2,
            "$identical of ${en.strings.size} German strings are identical to English — " +
                "is values-de/ actually translated?"
        )
    }

    // ---- shipped locale declaration --------------------------------------

    @Test
    fun `locales_config lists exactly the languages that have resources`() {
        val config = File(resDir, "xml/locales_config.xml")
        assertTrue(config.isFile, "locales_config.xml is missing — Android 13+ loses the per-app language picker")

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(config)
        val declared = doc.getElementsByTagName("locale").let { nodes ->
            (0 until nodes.length).map {
                (nodes.item(it) as Element).getAttribute("android:name")
            }.toSet()
        }

        val shipped = (resDir.listFiles() ?: emptyArray())
            .filter { it.isDirectory && File(it, "strings.xml").isFile }
            .map { if (it.name == "values") "en" else it.name.removePrefix("values-") }
            .toSet()

        assertEquals(
            shipped, declared,
            "locales_config.xml and the values-* folders disagree"
        )
    }
}
