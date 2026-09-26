package com.pepperonas.brutus.update

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.R

/** One check: ask the source, remember the finding, notify once per new version. */
object UpdateChecker {

    enum class Outcome { DISABLED, FAILED, UP_TO_DATE, ALREADY_NOTIFIED, NOTIFIED }

    private const val NOTIFICATION_ID = 0x5550D47E

    fun check(context: Context, source: LatestVersionSource, installed: String): Outcome {
        if (!UpdateCheckStore.isEnabled(context)) return Outcome.DISABLED
        val latest = source.latestVersion() ?: return Outcome.FAILED
        if (AppVersion.parse(latest) == null) return Outcome.FAILED
        UpdateCheckStore.recordLatest(context, latest)

        if (!AppVersion.isNewer(latest, installed)) return Outcome.UP_TO_DATE
        if (UpdateCheckStore.notifiedVersion(context) == latest) return Outcome.ALREADY_NOTIFIED

        notify(context, latest)
        UpdateCheckStore.markNotified(context, latest)
        return Outcome.NOTIFIED
    }

    /** The version the banner should offer, or null when there is nothing to show. */
    fun bannerVersion(context: Context, installed: String): String? {
        if (!UpdateCheckStore.isEnabled(context)) return null
        val latest = UpdateCheckStore.latestSeen(context) ?: return null
        return if (AppVersion.isNewer(latest, installed)) AppVersion.display(latest) else null
    }

    fun downloadIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(ReleaseSource.DOWNLOAD_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun installedVersion(context: Context): String =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }

    private fun notify(context: Context, latest: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return // the banner still shows it

        val tap = PendingIntent.getActivity(
            context, 0, downloadIntent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val version = AppVersion.display(latest)
        val n = NotificationCompat.Builder(context, BrutusApplication.CHANNEL_UPDATES)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.update_notification_title, version))
            .setContentText(context.getString(R.string.update_notification_body))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
    }
}
