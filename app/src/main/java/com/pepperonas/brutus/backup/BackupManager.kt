package com.pepperonas.brutus.backup

import android.content.Context
import com.pepperonas.brutus.data.AlarmDatabase
import com.pepperonas.brutus.scheduler.Rescheduler
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.AppSettings
import com.pepperonas.brutus.util.GlobalQrStore
import com.pepperonas.brutus.widget.NextAlarmWidget

/** Export and import between the app's stores and an [AlarmBackup] file. */
object BackupManager {

    data class ImportResult(
        val added: Int,
        val alreadyThere: Int,
        val skipped: Int,
        val qrReplaced: Boolean,
    )

    suspend fun export(context: Context, now: Long = System.currentTimeMillis()): String {
        val alarms = AlarmDatabase.getInstance(context).alarmDao().getAll()
        return AlarmBackup.encode(
            alarms = alarms,
            qrData = GlobalQrStore.get(context),
            settings = AlarmBackup.Settings(
                crescendoSeconds = AppSettings.crescendoSeconds(context),
                ringLimitMinutes = AppSettings.ringLimitMinutes(context),
                upcomingLeadMinutes = AppSettings.upcomingLeadMinutes(context),
                sunriseSoundId = AppSettings.sunriseSound(context).id,
            ),
            exportedAt = now,
        )
    }

    /**
     * Adds the file's alarms that are not there yet, takes over its QR code and its settings
     * (only values the app offers), then re-arms everything.
     * @throws AlarmBackup.FormatException when the file is not a Brutus backup.
     */
    suspend fun import(context: Context, text: String): ImportResult {
        val contents = AlarmBackup.decode(text)
        val dao = AlarmDatabase.getInstance(context).alarmDao()
        val fresh = AlarmBackup.newAlarms(dao.getAll(), contents.alarms)
        fresh.forEach { dao.insert(it) }

        val qrReplaced = contents.qrData
            ?.takeIf { GlobalQrStore.isValid(it) && it != GlobalQrStore.get(context) }
            ?.let { GlobalQrStore.set(context, it); true } ?: false

        contents.settings.let { s ->
            s.crescendoSeconds?.takeIf { it in AppSettings.CRESCENDO_OPTIONS }
                ?.let { AppSettings.setCrescendoSeconds(context, it) }
            s.ringLimitMinutes?.takeIf { it in AppSettings.RING_LIMIT_OPTIONS }
                ?.let { AppSettings.setRingLimitMinutes(context, it) }
            s.upcomingLeadMinutes?.takeIf { it in AppSettings.UPCOMING_LEAD_OPTIONS }
                ?.let { AppSettings.setUpcomingLeadMinutes(context, it) }
            s.sunriseSoundId?.let { AlarmSound.fromId(it) }?.takeIf { it in AlarmSound.sunriseSounds() }
                ?.let { AppSettings.setSunriseSound(context, it) }
        }

        Rescheduler.rescheduleAll(context)
        NextAlarmWidget.refresh(context)
        return ImportResult(
            added = fresh.size,
            alreadyThere = contents.alarms.size - fresh.size,
            skipped = contents.skipped,
            qrReplaced = qrReplaced,
        )
    }
}
