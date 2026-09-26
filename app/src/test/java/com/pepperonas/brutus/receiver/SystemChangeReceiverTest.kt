package com.pepperonas.brutus.receiver

import android.app.AlarmManager
import android.content.Intent
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * The receiver is only as good as its manifest filter: an action it handles but the manifest does
 * not declare never arrives. Before v2.3.1 there was no such receiver at all — a time-zone change
 * left every alarm at the old absolute time.
 */
class SystemChangeReceiverTest {

    private val manifest = File("src/main/AndroidManifest.xml").readText()

    private fun receiverBlock(name: String): String {
        val start = manifest.indexOf("android:name=\"$name\"")
        check(start >= 0) { "$name missing from the manifest" }
        return manifest.substring(start, manifest.indexOf("</receiver>", start))
    }

    private fun openingTag(name: String): String {
        val start = manifest.indexOf("android:name=\"$name\"")
        check(start >= 0) { "$name missing from the manifest" }
        return manifest.substring(start, manifest.indexOf(">", start))
    }

    @Test
    fun `every handled action is declared in the manifest filter`() {
        val block = receiverBlock(".receiver.SystemChangeReceiver")
        SystemChangeReceiver.HANDLED.forEach {
            assertTrue("android:name=\"$it\"" in block, "$it is handled but never delivered")
        }
    }

    @Test
    fun `it covers clock, zone, exact-alarm permission and app update`() {
        listOf(
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        ).forEach { assertTrue(it in SystemChangeReceiver.HANDLED, "$it not handled") }
    }

    @Test
    fun `the whole ringing path can run before the first unlock`() {
        listOf(
            ".receiver.AlarmReceiver", ".receiver.BootReceiver", ".receiver.SystemChangeReceiver",
            ".service.AlarmService", ".AlarmActivity", ".SunriseActivity", ".UltraHardcoreTaskActivity",
        ).forEach {
            assertTrue("android:directBootAware=\"true\"" in openingTag(it), "$it is not direct-boot aware")
        }
    }
}
