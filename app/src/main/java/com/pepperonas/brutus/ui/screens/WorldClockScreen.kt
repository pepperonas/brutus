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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pepperonas.brutus.ui.theme.BrutusRed
import com.pepperonas.brutus.ui.theme.BrutusTextSecondary
import com.pepperonas.brutus.util.WorldClockStore
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen() {
    val context = LocalContext.current
    var zones by remember { mutableStateOf(WorldClockStore.load(context)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }

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
                text = "Weltuhr",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showSheet = true }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Zeitzone hinzufügen",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (zones.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Tippe +, um eine Zeitzone hinzuzufügen",
                    color = BrutusTextSecondary
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(zones, key = { it }) { zone ->
                    ZoneCard(
                        zoneId = zone,
                        now = now,
                        onRemove = {
                            val updated = zones - zone
                            zones = updated
                            WorldClockStore.save(context, updated)
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
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
private fun ZoneCard(zoneId: String, now: Long, onRemove: () -> Unit) {
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
    val date = remember(zoned) {
        zoned?.format(DateTimeFormatter.ofPattern("EEE, d. MMM")) ?: ""
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$region  ·  UTC$offsetLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrutusTextSecondary
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrutusTextSecondary
                )
            }
            Text(
                text = time,
                fontSize = 36.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Entfernen",
                    tint = BrutusTextSecondary
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
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                "Zeitzone hinzufügen",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = BrutusTextSecondary)
                Spacer(modifier = Modifier.height(0.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(BrutusRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp)
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
                                color = if (disabled) BrutusTextSecondary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            if (disabled) {
                                Text(
                                    text = "bereits hinzugefügt",
                                    fontSize = 11.sp,
                                    color = BrutusTextSecondary
                                )
                            }
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "Keine Treffer",
                            color = BrutusTextSecondary,
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
