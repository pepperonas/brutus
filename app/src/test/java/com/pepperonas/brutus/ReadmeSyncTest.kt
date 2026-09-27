package com.pepperonas.brutus

import com.pepperonas.brutus.ui.settings.AboutLinks
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The README's big numbers — version, unit tests, lines of code — are typed in. This keeps them true:
 * a badge that says 350 tests while there are 360 is worse than no badge. Runs on the JVM from the
 * module directory, like every Gradle unit test.
 */
class ReadmeSyncTest {

    private val root = File("..").canonicalFile
    private val readmes = listOf("README.md", "README.de.md").map { File(root, it).readText() }

    private val versionName = Regex("""versionName = "([^"]+)"""")
        .find(File(root, "app/build.gradle.kts").readText())!!.groupValues[1]

    private fun kotlinLines(dir: String) =
        File(root, dir).walkTopDown().filter { it.extension == "kt" }.sumOf { it.readLines().size }

    /** "11.3k" → 11300 */
    private fun thousands(s: String) = (s.removeSuffix("k").toDouble() * 1000).toInt()

    private fun badge(readme: String, label: String): String =
        Regex("""img\.shields\.io/badge/${Regex.escape(label)}-([^-?)]+)""").find(readme)?.groupValues?.get(1)
            ?: error("no '$label' badge")

    @Test
    fun `the version badge shows the app's versionName`() {
        readmes.forEach { assertEquals(versionName, badge(it, "version")) }
    }

    @Test
    fun `both changelogs start with the current version`() {
        listOf("CHANGELOG.md", "CHANGELOG.de.md").forEach { name ->
            val top = Regex("""^## \[([^\]]+)]""", RegexOption.MULTILINE).find(File(root, name).readText())!!.groupValues[1]
            assertEquals(versionName, top, name)
        }
    }

    @Test
    fun `the unit test badges count the tests that exist`() {
        val tests = File(root, "app/src/test").walkTopDown().filter { it.extension == "kt" }
            .sumOf { f -> f.readLines().count { it.trim() == "@Test" } }
        readmes.forEach { readme ->
            val shown = Regex("""unit%20tests-(\d+)""").findAll(readme).map { it.groupValues[1].toInt() }.toList()
            assertTrue(shown.isNotEmpty())
            shown.forEach { assertEquals(tests, it, "a unit test badge") }
        }
    }

    @Test
    fun `the lines-of-code badges are within five percent of the source`() {
        val main = kotlinLines("app/src/main")
        val test = kotlinLines("app/src/test")
        readmes.forEach { readme ->
            val shownMain = thousands(badge(readme, "lines%20of%20code"))
            val shownTest = thousands(badge(readme, "test%20code"))
            assertTrue(abs(shownMain - main) <= main * 0.05, "lines of code: badge $shownMain, source $main")
            assertTrue(abs(shownTest - test) <= test * 0.05, "test code: badge $shownTest, source $test")
        }
    }

    @Test
    fun `the PayPal button goes where the app's donate button goes`() {
        readmes.forEach { readme ->
            assertTrue(AboutLinks.donateUrl("Brutus") in readme, "README PayPal link differs from AboutLinks.donateUrl()")
        }
    }

    @Test
    fun `every image the READMEs show exists`() {
        readmes.forEach { readme ->
            Regex("""src="((?:docs|app)/[^"]+)"""").findAll(readme).map { it.groupValues[1] }.forEach { path ->
                assertTrue(File(root, path).isFile, "missing $path")
            }
        }
    }

    @Test
    fun `both languages show the same pictures`() {
        val images = readmes.map { r -> Regex("""src="((?:docs|app)/[^"]+)"""").findAll(r).map { it.groupValues[1] }.toSet() }
        assertEquals(images[0], images[1])
    }
}
