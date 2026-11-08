package com.pepperonas.brutus.backup

import com.pepperonas.brutus.util.Storage
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What a backup takes along. State that only describes this phone's current moment must stay
 * behind, both backup formats must agree, and every preference file must be a deliberate choice.
 */
class BackupRulesTest {

    private val res = File("src/main/res/xml")

    /** Excludes of one rules element as "domain:path". */
    private fun excludes(file: String, section: String?): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, file))
        val root = if (section == null) doc.documentElement
        else doc.getElementsByTagName(section).item(0) as org.w3c.dom.Element
        val nodes = root.getElementsByTagName("exclude")
        return (0 until nodes.length).map { nodes.item(it) as org.w3c.dom.Element }
            .map { "${it.getAttribute("domain")}:${it.getAttribute("path")}" }.toSet()
    }

    private val transient = setOf("brutus_ringing", "brutus_ultra_hardcore", "brutus_timer_state", "brutus_updates")
    private val kept = setOf("brutus_global", "brutus_settings", "brutus_timer", "brutus_world_clock")

    @Test
    fun `cloud backup, device transfer and the pre-Android-12 rules exclude the same things`() {
        val legacy = excludes("backup_rules.xml", null)
        assertEquals(legacy, excludes("data_extraction_rules.xml", "cloud-backup"))
        assertEquals(legacy, excludes("data_extraction_rules.xml", "device-transfer"))
    }

    @Test
    fun `snoozes, missed-alarm bookkeeping, follow-ups and the running timer stay on the phone`() {
        val ex = excludes("backup_rules.xml", null)
        transient.forEach { assertTrue("device_sharedpref:$it.xml" in ex, "$it would be restored") }
    }

    @Test
    fun `every preference file is either kept or excluded on purpose`() {
        // A new store must be sorted into one of the two sets above.
        assertEquals(Storage.PREFERENCE_FILES.toSet(), transient + kept)
        val ex = excludes("backup_rules.xml", null)
        kept.forEach { assertTrue("device_sharedpref:$it.xml" !in ex, "$it must be backed up") }
    }

    @Test
    fun `the manifest uses both rule files and the restore agent`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        listOf(
            "android:fullBackupContent=\"@xml/backup_rules\"",
            "android:dataExtractionRules=\"@xml/data_extraction_rules\"",
            "android:backupAgent=\".backup.BrutusBackupAgent\"",
            "android:fullBackupOnly=\"true\"",
        ).forEach { assertTrue(it in manifest, it) }
    }
}
