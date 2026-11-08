@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.pepperonas.brutus.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AlarmAdd
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pepperonas.brutus.data.AlarmEntity
import com.pepperonas.brutus.ui.theme.BrutusTheme
import com.pepperonas.brutus.util.NextAlarmCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.pepperonas.brutus.R

// Header, empty state and add button of the alarm list.

/** Brand FAB with a press shape-morph: pill relaxes toward a squircle under the finger. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AddAlarmFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) 12.dp else 20.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "fabCorner"
    )
    FloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(corner),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        interactionSource = interaction,
        modifier = modifier,
    ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.alarm_list_add))
    }
}

@Composable
internal fun NextAlarmHeader(next: AlarmEntity?, now: Long, loaded: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val triggerMillis = remember(next, now) {
        next?.let { NextAlarmCalculator.upcoming(context, it, now) }
    }

    Column(
        modifier = modifier.padding(top = 28.dp, bottom = 20.dp),
    ) {
        if (triggerMillis != null) {
            Text(
                text = stringResource(R.string.alarm_list_next_alarm),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = NextAlarmCalculator.formatCountdown(
                    LocalContext.current, now, triggerMillis
                ),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatTriggerDate(
                    triggerMillis,
                    stringResource(R.string.format_next_alarm_date)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (loaded) {
            Text(
                text = stringResource(R.string.alarm_list_no_alarm),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal fun formatTriggerDate(millis: Long, pattern: String): String {
    // Pattern comes from resources — German writes "Mo, 17. Aug, 06:30",
    // English "Mon, Aug 17, 06:30".
    val fmt = SimpleDateFormat(pattern, Locale.getDefault())
    return fmt.format(Date(millis))
}

@Composable
internal fun EmptyState(onCreate: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.AlarmAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.alarm_list_empty_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.alarm_list_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            FilledTonalButton(onClick = onCreate) {
                Text(stringResource(R.string.alarm_list_create))
            }
        }
    }
}

@Preview(name = "Empty dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
internal fun EmptyStatePreviewDark() {
    BrutusTheme(darkTheme = true) { EmptyState(onCreate = {}) }
}
