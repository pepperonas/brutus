@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.pepperonas.brutus.ui.screens

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pepperonas.brutus.R
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.scheduler.AlarmActions
import com.pepperonas.brutus.update.UpdateCheckStore
import com.pepperonas.brutus.update.UpdateChecker
import com.pepperonas.brutus.util.BatteryOptimizationPermission
import com.pepperonas.brutus.util.ExactAlarmPermission
import com.pepperonas.brutus.util.FullScreenIntentPermission
import com.pepperonas.brutus.util.NextAlarmCalculator
import com.pepperonas.brutus.util.SoundPreviewPlayer
import com.pepperonas.brutus.util.UltraHardcoreStore
import com.pepperonas.brutus.util.VacationStore
import com.pepperonas.brutus.util.rememberBrutusHaptics
import com.pepperonas.brutus.viewmodel.AlarmViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AlarmListScreen(
    viewModel: AlarmViewModel,
    onOpenSettings: () -> Unit = {},
    openNewAlarm: Boolean = false,
    onNewAlarmOpened: () -> Unit = {},
) {
    val loadedAlarms by viewModel.alarms.collectAsState()
    val loaded = loadedAlarms != null
    val alarms = loadedAlarms.orEmpty()
    var showDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }
    // Launcher shortcut "New alarm".
    LaunchedEffect(openNewAlarm) {
        if (openNewAlarm) {
            editingAlarm = null
            showDialog = true
            onNewAlarmOpened()
        }
    }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalContext.current.resources
    val undoLabel = stringResource(R.string.action_undo)

    // Deletes with an undo affordance: the removed alarms are held here until
    // the snackbar is dismissed or its "Rückgängig" action restores them.
    val deleteWithUndo: (List<AlarmEntity>) -> Unit = { toDelete ->
        if (toDelete.isNotEmpty()) {
            toDelete.forEach { viewModel.deleteAlarm(it) }
            scope.launch {
                // A newer delete replaces a still-visible undo snackbar.
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = resources.getQuantityString(
                        R.plurals.alarm_deleted, toDelete.size, toDelete.size
                    ),
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.restoreAlarms(toDelete)
                }
            }
        }
    }

    val context = LocalContext.current
    val previewPlayer = remember { SoundPreviewPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { previewPlayer.stop() }
    }
    val haptics = rememberBrutusHaptics()

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(30_000L)
        }
    }
    // Vacation mode is set in Settings or from the quick-settings tile; the list follows it live.
    val vacationTick by VacationStore.changes(context).collectAsState(initial = -1)
    val pausedUntil = remember(vacationTick, nowMillis) { VacationStore.pausedUntil(context, nowMillis) }
    val vacationScope = rememberCoroutineScope()
    // A skipped occurrence ("Dismiss early") or a vacation is not the next alarm.
    val nextAlarm = remember(alarms, nowMillis, vacationTick) {
        NextAlarmCalculator.findNext(context, alarms, nowMillis)
    }

    // Re-check exact-alarm + battery + full-screen-intent permissions on resume
    var exactGranted by remember { mutableStateOf(ExactAlarmPermission.isGranted(context)) }
    var batteryIgnoring by remember { mutableStateOf(BatteryOptimizationPermission.isIgnoring(context)) }
    var fsiGranted by remember { mutableStateOf(FullScreenIntentPermission.isGranted(context)) }
    var notificationsOn by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    // The UHC reminder notification can be swiped away on Android 14+; the task must stay reachable.
    var uhcPendingId by remember { mutableStateOf(UltraHardcoreStore.pendingAlarmIds(context).firstOrNull()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // Opt-in update check: the switch and the banner both follow the store live
    // (the worker writes the finding in the background).
    val updateTick by UpdateCheckStore.changes(context).collectAsState(initial = -1)
    val installedVersion = remember { UpdateChecker.installedVersion(context) }
    val updateVersion = remember(updateTick) { UpdateChecker.bannerVersion(context, installedVersion) }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactGranted = ExactAlarmPermission.isGranted(context)
                batteryIgnoring = BatteryOptimizationPermission.isIgnoring(context)
                fsiGranted = FullScreenIntentPermission.isGranted(context)
                notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
                uhcPendingId = UltraHardcoreStore.pendingAlarmIds(context).firstOrNull()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NextAlarmHeader(
                    next = nextAlarm,
                    now = nowMillis,
                    loaded = loaded,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.action_more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.alarm_list_delete_all)) },
                            enabled = alarms.isNotEmpty(),
                            onClick = {
                                menuOpen = false
                                confirmDeleteAll = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.alarm_list_settings)) },
                            leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onOpenSettings()
                            }
                        )
                    }
                }
            }

            AlarmStatusBanners(
                updateVersion = updateVersion,
                pausedUntil = pausedUntil,
                onResumeVacation = {
                    vacationScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        AlarmActions.resumeFromVacation(context.applicationContext)
                    }
                },
                exactGranted = exactGranted,
                batteryIgnoring = batteryIgnoring,
                fsiGranted = fsiGranted,
                notificationsOn = notificationsOn,
                uhcPendingId = uhcPendingId,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Deleting the last alarm (or creating the first) crossfades instead of swapping hard.
            val listEffects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            val listFastEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            val listSpatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
            AnimatedContent(
                // Before the first database answer: nothing, rather than an empty state that isn't true.
                targetState = when {
                    !loaded -> ListState.LOADING
                    alarms.isEmpty() -> ListState.EMPTY
                    else -> ListState.LIST
                },
                transitionSpec = {
                    (fadeIn(listEffects) + scaleIn(listSpatial, initialScale = 0.96f)) togetherWith
                        fadeOut(listFastEffects)
                },
                label = "listOrEmpty",
            ) { state ->
            if (state == ListState.LOADING) {
                Box(Modifier.fillMaxSize())
            } else if (state == ListState.EMPTY) {
                EmptyState(onCreate = {
                    editingAlarm = null
                    showDialog = true
                })
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        DismissableAlarmCard(
                            alarm = alarm,
                            isNext = nextAlarm?.id == alarm.id,
                            modifier = Modifier.animateItem(),
                            onToggle = {
                                haptics.tap()
                                viewModel.toggleAlarm(alarm)
                            },
                            onDelete = {
                                haptics.warn()
                                deleteWithUndo(listOf(alarm))
                            },
                            onCopy = {
                                haptics.tap()
                                // id = 0 marks the dialog payload as a copy template:
                                // everything prefilled, saving creates a NEW alarm.
                                editingAlarm = alarm.copy(id = 0)
                                showDialog = true
                            },
                            onClick = {
                                editingAlarm = alarm
                                showDialog = true
                            }
                        )
                    }
                    // Keep the FAB from covering the last card's actions.
                    item { Spacer(modifier = Modifier.height(96.dp)) }
                }
            }
            }
        }

        AddAlarmFab(
            onClick = {
                haptics.tap()
                editingAlarm = null
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text(stringResource(R.string.alarm_list_delete_all_title)) },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.alarm_delete_all_body, alarms.size, alarms.size
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    haptics.warn()
                    deleteWithUndo(alarms.toList())
                }) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showDialog) {
        AlarmEditDialog(
            existingAlarm = editingAlarm,
            onDismiss = { showDialog = false },
            onPreviewSound = { snd -> previewPlayer.play(snd) },
            onStopPreview = { previewPlayer.stop() },
            onSave = { result ->
                // id == 0 means "copy template" — persist as a new alarm.
                if (editingAlarm != null && editingAlarm!!.id != 0L) {
                    viewModel.updateAlarm(
                        editingAlarm!!.copy(
                            hour = result.hour,
                            minute = result.minute,
                            label = result.label,
                            repeatDays = result.repeatDays,
                            challengeFlags = result.challengeFlags,
                            snoozeDuration = result.snoozeDuration,
                            soundId = result.soundId,
                            mathProblemCount = result.mathProblemCount,
                            shakeCount = result.shakeCount,
                            hardcoreMode = result.hardcoreMode,
                            ultraHardcoreMode = result.ultraHardcoreMode,
                            mathDifficulty = result.mathDifficulty,
                            shakeSensitivity = result.shakeSensitivity,
                            sunriseEnabled = result.sunriseEnabled,
                        )
                    )
                } else {
                    viewModel.addAlarm(
                        hour = result.hour,
                        minute = result.minute,
                        label = result.label,
                        repeatDays = result.repeatDays,
                        challengeFlags = result.challengeFlags,
                        snoozeDuration = result.snoozeDuration,
                        soundId = result.soundId,
                        mathProblemCount = result.mathProblemCount,
                        shakeCount = result.shakeCount,
                        hardcoreMode = result.hardcoreMode,
                        ultraHardcoreMode = result.ultraHardcoreMode,
                        mathDifficulty = result.mathDifficulty,
                        shakeSensitivity = result.shakeSensitivity,
                        sunriseEnabled = result.sunriseEnabled,
                    )
                }
                showDialog = false
            }
        )
    }
}

private enum class ListState { LOADING, EMPTY, LIST }
