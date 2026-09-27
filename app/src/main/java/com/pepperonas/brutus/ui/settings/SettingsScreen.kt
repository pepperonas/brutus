package com.pepperonas.brutus.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import com.pepperonas.brutus.R
import com.pepperonas.brutus.scheduler.Rescheduler
import com.pepperonas.brutus.util.AlarmSound
import com.pepperonas.brutus.util.AppSettings
import com.pepperonas.brutus.util.SoundPreviewPlayer
import kotlinx.coroutines.Dispatchers
import com.pepperonas.brutus.ui.theme.ThemeSettings
import com.pepperonas.brutus.ui.theme.springPressed
import com.pepperonas.brutus.ui.util.openUrl
import com.pepperonas.brutus.update.UpdateCheckStore
import com.pepperonas.brutus.update.UpdateScheduler
import com.pepperonas.brutus.util.rememberBrutusHaptics
import kotlinx.coroutines.launch

/**
 * Settings & info, reached from the ⋮ menu of the alarm list. Same build as Flipper the Ripper's
 * settings: section title in the primary colour over a rounded card; About is the last section.
 * Slot [notificationExtras] lets later sections add rows without this screen knowing about them.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    notificationExtras: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberBrutusHaptics()
    val snackbar = remember { SnackbarHostState() }
    val openFailed = stringResource(R.string.about_open_failed)
    fun reportOpenFailed() {
        scope.launch { snackbar.showSnackbar(openFailed) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            SettingsSection(stringResource(R.string.settings_appearance)) {
                ThemeModeRow()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val dynamicOn by ThemeSettings.dynamicColorFlow(context).collectAsState(initial = false)
                    Spacer(Modifier.height(16.dp))
                    SwitchRow(
                        title = stringResource(R.string.alarm_list_material_you),
                        hint = stringResource(R.string.settings_dynamic_color_hint),
                        checked = dynamicOn,
                        onToggle = {
                            haptics.tap()
                            scope.launch { ThemeSettings.setDynamicColor(context, !dynamicOn) }
                        },
                    )
                }
            }

            SettingsSection(stringResource(R.string.settings_sunrise)) {
                SunriseSoundRow()
            }

            SettingsSection(stringResource(R.string.settings_notifications)) {
                UpcomingLeadRow()
                Spacer(Modifier.height(16.dp))
                UpdateCheckRow()
                notificationExtras()
            }

            SettingsSection(stringResource(R.string.settings_about)) {
                AboutSection(onOpenFailed = ::reportOpenFailed)
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
    )
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun ThemeModeRow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode by ThemeSettings.modeFlow(context).collectAsState(initial = ThemeSettings.Mode.SYSTEM)
    val options = listOf(
        ThemeSettings.Mode.SYSTEM to R.string.settings_theme_system,
        ThemeSettings.Mode.LIGHT to R.string.settings_theme_light,
        ThemeSettings.Mode.DARK to R.string.settings_theme_dark,
    )
    Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(8.dp))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = mode == value,
                onClick = { scope.launch { ThemeSettings.setMode(context, value) } },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(stringResource(label)) },
            )
        }
    }
}

/** What the Sunrise pre-alarm plays. Tapping a chip picks it and previews it; leaving stops the preview. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SunriseSoundRow() {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(AppSettings.sunriseSound(context)) }
    val preview = remember { SoundPreviewPlayer(context) }
    DisposableEffect(Unit) { onDispose { preview.stop() } }
    Text(stringResource(R.string.settings_sunrise_sound), style = MaterialTheme.typography.bodyLarge)
    Text(
        stringResource(R.string.settings_sunrise_sound_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AlarmSound.sunriseSounds().forEach { snd ->
            FilterChip(
                selected = selected == snd,
                onClick = {
                    selected = snd
                    AppSettings.setSunriseSound(context, snd)
                    preview.play(snd)
                },
                label = { Text(stringResource(snd.labelRes)) },
            )
        }
    }
    Text(
        stringResource(selected.descriptionRes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TextButton(onClick = { preview.stop() }) { Text(stringResource(R.string.settings_sunrise_stop_preview)) }
}

/** How long before an alarm the quiet heads-up appears; changing it re-arms every alarm. */
@Composable
private fun UpcomingLeadRow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lead by remember { mutableStateOf(AppSettings.upcomingLeadMinutes(context)) }
    Text(stringResource(R.string.settings_upcoming), style = MaterialTheme.typography.bodyLarge)
    Text(
        stringResource(R.string.settings_upcoming_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    val options = AppSettings.UPCOMING_LEAD_OPTIONS
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, minutes ->
            SegmentedButton(
                selected = lead == minutes,
                onClick = {
                    lead = minutes
                    AppSettings.setUpcomingLeadMinutes(context, minutes)
                    scope.launch(Dispatchers.IO) { Rescheduler.rescheduleAll(context.applicationContext) }
                },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = {
                    Text(
                        if (minutes == 0) stringResource(R.string.settings_upcoming_off)
                        else stringResource(R.string.settings_upcoming_minutes, minutes),
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
private fun UpdateCheckRow() {
    val context = LocalContext.current
    val haptics = rememberBrutusHaptics()
    val tick by UpdateCheckStore.changes(context).collectAsState(initial = -1)
    val on = remember(tick) { UpdateCheckStore.isEnabled(context) }
    // Asked only when the user switches the check on — the in-app banner works without it.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    SwitchRow(
        title = stringResource(R.string.alarm_list_update_check),
        hint = stringResource(R.string.settings_update_check_hint),
        checked = on,
        onToggle = {
            haptics.tap()
            val enable = !on
            UpdateScheduler.setEnabled(context, enable)
            if (enable && needsNotificationPermission(context)) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
    )
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

/** A whole-row toggle: the row is the touch target, the switch only shows the state. */
@Composable
internal fun SwitchRow(title: String, hint: String?, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.size(16.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * About: who made the app, where it lives, under which licences — and a way to say thanks. Every
 * fact comes from [AboutLinks] or the installed package; nothing here is typed by hand.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutSection(onOpenFailed: () -> Unit) {
    val context = LocalContext.current
    fun open(url: String) {
        if (!context.openUrl(url)) onOpenFailed()
    }
    val (versionName, versionCode) = remember { installedVersion(context) }
    var showFontLicense by rememberSaveable { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(56.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
            // The launcher monogram, drawn on its own background — same mark as the website.
            Icon(
                painterResource(R.drawable.ic_launcher_background),
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier.size(56.dp),
            )
            Icon(
                painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier.size(56.dp),
            )
        }
        Column {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.about_version_format, versionName, versionCode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.about_made_by, AboutLinks.AUTHOR), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(R.string.about_tagline),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(12.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinkChip(stringResource(R.string.about_website), Icons.Outlined.Public) { open(AboutLinks.PRODUCT_URL) }
        LinkChip(AboutLinks.WEBSITE_LABEL, Icons.Outlined.Language) { open(AboutLinks.WEBSITE_URL) }
        LinkChip(stringResource(R.string.about_source), Icons.Outlined.Code) { open(AboutLinks.REPO_URL) }
        LinkChip(AboutLinks.LICENSE_NAME, Icons.Outlined.Description) { open(AboutLinks.LICENSE_URL) }
        LinkChip(stringResource(R.string.about_third_party), Icons.Outlined.TextFields) { showFontLicense = true }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(R.string.about_license_hint, AboutLinks.LICENSE_NAME),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(16.dp))
    val donateInteraction = remember { MutableInteractionSource() }
    Button(
        onClick = { open(AboutLinks.donateUrl()) },
        interactionSource = donateInteraction,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth().height(52.dp).springPressed(donateInteraction),
    ) {
        Icon(Icons.Outlined.Favorite, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.about_donate))
    }
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(R.string.about_donate_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (showFontLicense) {
        val text = remember { context.resources.openRawResource(R.raw.space_grotesk_ofl).bufferedReader().use { it.readText() } }
        AlertDialog(
            onDismissRequest = { showFontLicense = false },
            confirmButton = { TextButton(onClick = { showFontLicense = false }) { Text(stringResource(R.string.action_close)) } },
            title = { Text(stringResource(R.string.about_font_license_title)) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text(text, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                }
            },
        )
    }
}

@Composable
private fun LinkChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) },
        modifier = Modifier.height(40.dp),
    )
}

internal fun installedVersion(context: Context): Pair<String, Long> = try {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    (info.versionName ?: "") to PackageInfoCompat.getLongVersionCode(info)
} catch (_: Exception) {
    "" to 0L
}
