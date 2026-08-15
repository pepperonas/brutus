package com.pepperonas.brutus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.util.WorldClockStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.pepperonas.brutus.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen() {
    val context = LocalContext.current
    var zones by remember { mutableStateOf(WorldClockStore.load(context)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val removedMessage = stringResource(R.string.world_removed)
    val undoLabel = stringResource(R.string.action_undo)
    // Removing a zone is undoable — the snackbar puts it back at its old spot.
    val removeWithUndo: (String) -> Unit = { zone ->
        val index = zones.indexOf(zone)
        if (index >= 0) {
            val updated = zones - zone
            zones = updated
            WorldClockStore.save(context, updated)
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = removedMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    val restored = zones.toMutableList()
                        .apply { add(index.coerceIn(0, size), zone) }
                    zones = restored
                    WorldClockStore.save(context, restored)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.world_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showSheet = true }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.world_add),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (zones.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.world_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(zones, key = { it }) { zone ->
                    ZoneCard(
                        zoneId = zone,
                        now = now,
                        modifier = Modifier.animateItem(),
                        onRemove = { removeWithUndo(zone) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 8.dp)
    )
    }

    if (showSheet) {
        AddZoneSheet(
            alreadyAdded = zones.toSet(),
            onDismiss = { showSheet = false },
            onAdd = { zone ->
                val updated = zones + zone
                zones = updated
                WorldClockStore.save(context, updated)
                showSheet = false
            }
        )
    }
}

@Composable
private fun ZoneCard(
    zoneId: String,
    now: Long,
    modifier: Modifier = Modifier,
    onRemove: () -> Unit,
) {
    val zoned = remember(zoneId, now) {
        try {
            ZonedDateTime.now(ZoneId.of(zoneId))
        } catch (_: Exception) {
            null
        }
    }
    val city = zoneId.substringAfterLast('/').replace('_', ' ')
    val region = zoneId.substringBeforeLast('/')
    val offsetLabel = remember(zoned) {
        zoned?.offset?.toString() ?: ""
    }
    val time = remember(zoned) {
        zoned?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--"
    }
    // Pattern comes from resources: German writes "Mo, 17. Aug", English "Mon, Aug 17".
    val datePattern = stringResource(R.string.format_world_clock_date)
    val date = remember(zoned, datePattern) {
        zoned?.format(DateTimeFormatter.ofPattern(datePattern, Locale.getDefault())) ?: ""
    }
    // Day/night at the remote location, told through color roles: warm
    // tertiary sun vs. muted secondary moon.
    val isDay = (zoned?.hour ?: 12) in 6..17

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDay) Icons.Default.WbSunny else Icons.Default.DarkMode,
                contentDescription = stringResource(
                    if (isDay) R.string.world_day else R.string.world_night
                ),
                tint = if (isDay) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(end = 14.dp)
                    .size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.world_region_offset, region, offsetLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = time,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.world_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddZoneSheet(
    alreadyAdded: Set<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val zones = remember {
        ZoneId.getAvailableZoneIds().sorted()
    }
    val filtered = remember(query) {
        val q = query.trim().lowercase()
        zones.filter { q.isBlank() || it.lowercase().contains(q) }.take(60)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                stringResource(R.string.world_add),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(
                        MaterialTheme.colorScheme.primary
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.world_search_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.height(420.dp)
            ) {
                items(filtered, key = { it }) { zone ->
                    val disabled = zone in alreadyAdded
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !disabled) { onAdd(zone) }
                            .padding(vertical = 12.dp),
                    ) {
                        Column {
                            Text(
                                text = zone,
                                color = if (disabled) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface
                            )
                            if (disabled) {
                                Text(
                                    text = stringResource(R.string.world_already_added),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.world_no_matches),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun ZoneStack() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ZoneCard(zoneId = "Europe/Berlin", now = 0L, onRemove = {})
        ZoneCard(zoneId = "America/New_York", now = 0L, onRemove = {})
        ZoneCard(zoneId = "Asia/Tokyo", now = 0L, onRemove = {})
    }
}

@Preview(
    name = "Zones dark",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
private fun ZonesPreviewDark() {
    BrutusTheme(darkTheme = true) { ZoneStack() }
}

@Preview(
    name = "Zones light",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Composable
private fun ZonesPreviewLight() {
    BrutusTheme(darkTheme = false) { ZoneStack() }
}

@Preview(
    name = "Zones dynamic",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    wallpaper = androidx.compose.ui.tooling.preview.Wallpapers.RED_DOMINATED_EXAMPLE,
)
@Composable
private fun ZonesPreviewDynamic() {
    BrutusTheme(darkTheme = true) { ZoneStack() }
}
