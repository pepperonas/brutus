@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.pepperonas.brutus.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pepperonas.brutus.R
import com.pepperonas.brutus.update.UpdateChecker
import com.pepperonas.brutus.util.BatteryOptimizationPermission
import com.pepperonas.brutus.util.ExactAlarmPermission
import com.pepperonas.brutus.util.FullScreenIntentPermission
import com.pepperonas.brutus.util.UltraHardcoreNotifier

// The banners above the alarm list: missing permissions, updates, vacation, Ultra Hardcore task.

@Composable
internal fun ExactAlarmBanner(onFix: () -> Unit) {
    PermissionBanner(
        title = stringResource(R.string.banner_exact_alarm_title),
        body = stringResource(R.string.banner_exact_alarm_body),
        actionLabel = stringResource(R.string.banner_exact_alarm_action),
        onFix = onFix,
    )
}

@Composable
internal fun BatteryOptimizationBanner(onFix: () -> Unit) {
    PermissionBanner(
        title = stringResource(R.string.banner_battery_title),
        body = stringResource(R.string.banner_battery_body),
        actionLabel = stringResource(R.string.banner_battery_action),
        warning = true,
        onFix = onFix,
    )
}

@Composable
internal fun FullScreenIntentBanner(onFix: () -> Unit) {
    PermissionBanner(
        title = stringResource(R.string.banner_fullscreen_title),
        body = stringResource(R.string.banner_fullscreen_body),
        actionLabel = stringResource(R.string.banner_fullscreen_action),
        onFix = onFix,
    )
}

/** A banner that slides in and out on the theme's springs instead of making the list jump. */
@Composable
internal fun ColumnScope.BannerSlot(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) +
            fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
        exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
            fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
    ) {
        Column {
            content()
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Error container for hard blockers, tertiary (warm orange) for soft warnings,
 * secondary for plain information (an available update).
 */
@Composable
internal fun PermissionBanner(
    title: String,
    body: String,
    actionLabel: String,
    warning: Boolean = false,
    info: Boolean = false,
    icon: ImageVector = Icons.Default.Warning,
    onFix: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val container = when {
        info -> cs.secondaryContainer
        warning -> cs.tertiaryContainer
        else -> cs.errorContainer
    }
    val content = when {
        info -> cs.onSecondaryContainer
        warning -> cs.onTertiaryContainer
        else -> cs.onErrorContainer
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, color = content)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = content.copy(alpha = 0.85f),
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            TextButton(onClick = onFix) {
                Text(actionLabel, color = content, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

/**
 * Everything the list has to tell before the alarms: an update, vacation mode, missing permissions
 * and a pending Ultra Hardcore task. Each slot animates in and out on its own.
 */
@Composable
internal fun ColumnScope.AlarmStatusBanners(
    updateVersion: String?,
    pausedUntil: Long?,
    onResumeVacation: () -> Unit,
    exactGranted: Boolean,
    batteryIgnoring: Boolean,
    fsiGranted: Boolean,
    notificationsOn: Boolean,
    uhcPendingId: Long?,
) {
    val context = LocalContext.current
    BannerSlot(updateVersion != null) {
        PermissionBanner(
            title = stringResource(R.string.banner_update_title, updateVersion ?: ""),
            body = stringResource(R.string.banner_update_body),
            actionLabel = stringResource(R.string.banner_update_action),
            info = true,
            icon = Icons.Default.SystemUpdate,
            onFix = { UpdateChecker.openDownload(context) },
        )
    }

    BannerSlot(pausedUntil != null) {
        val lastDay = (pausedUntil ?: 0L) - 1
        PermissionBanner(
            title = stringResource(
                R.string.banner_vacation_title,
                com.pepperonas.brutus.util.VacationDates.format(lastDay),
            ),
            body = stringResource(R.string.banner_vacation_body),
            actionLabel = stringResource(R.string.vacation_resume),
            info = true,
            icon = Icons.Default.BeachAccess,
            onFix = onResumeVacation,
        )
    }

    BannerSlot(!exactGranted) {
        ExactAlarmBanner(onFix = {
            ExactAlarmPermission.settingsIntent(context)?.let { context.startActivity(it) }
        })
    }

    BannerSlot(!batteryIgnoring) {
        BatteryOptimizationBanner(onFix = {
            try {
                context.startActivity(BatteryOptimizationPermission.settingsIntent(context))
            } catch (_: Exception) {
                context.startActivity(BatteryOptimizationPermission.fallbackSettingsIntent())
            }
        })
    }

    BannerSlot(!fsiGranted) {
        FullScreenIntentBanner(onFix = {
            FullScreenIntentPermission.settingsIntent(context)?.let { context.startActivity(it) }
        })
    }

    BannerSlot(!notificationsOn) {
        PermissionBanner(
            title = stringResource(R.string.banner_notifications_title),
            body = stringResource(R.string.banner_notifications_body),
            actionLabel = stringResource(R.string.banner_notifications_action),
            onFix = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            },
        )
    }

    BannerSlot(uhcPendingId != null) {
        val pendingId = uhcPendingId ?: -1L
        PermissionBanner(
            title = stringResource(R.string.notification_uhc_title),
            body = stringResource(R.string.banner_uhc_body),
            actionLabel = stringResource(R.string.notification_uhc_action),
            warning = true,
            onFix = { context.startActivity(UltraHardcoreNotifier.taskIntent(context, pendingId)) },
        )
    }
}
