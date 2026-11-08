package com.pepperonas.brutus.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.content.Context
import android.os.ParcelFileDescriptor
import com.pepperonas.brutus.scheduler.Rescheduler
import kotlinx.coroutines.runBlocking

/**
 * Exists for one reason: a restore puts the alarms back into the database, but AlarmManager knows
 * nothing about them — without this, restored alarms would stay silent until the app is opened.
 * Auto Backup still does the copying (`fullBackupOnly`); the key/value methods are never used.
 */
class BrutusBackupAgent : BackupAgent() {
    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) = Unit
    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) = Unit

    override fun onRestoreFinished() {
        afterRestore(this)
    }

    companion object {
        /** Re-arms everything that came back. Never throws — a restore must not fail over it. */
        fun afterRestore(context: Context) {
            try {
                runBlocking { Rescheduler.rescheduleAll(context) }
            } catch (_: Exception) {
                // MainActivity.onResume runs the same function on the first launch.
            }
        }
    }
}
