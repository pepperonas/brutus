package com.pepperonas.brutus.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Visual smoke test for the Expressive theme layer — color roles, surface
 * ladder, type scale, core components. Render-only; not shipped in any screen.
 */
@Composable
private fun ThemeSpecimen() {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("04:30", style = MaterialTheme.typography.displayLarge)
            Text("Brutus Expressive", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Tonale Ebenen, Springs, Space Grotesk.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) { Text("Primary") }
                FilledTonalButton(onClick = {}) { Text("Tonal") }
                OutlinedButton(onClick = {}) { Text("Outlined") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = true, onCheckedChange = null)
                Switch(checked = false, onCheckedChange = null)
            }
            SurfaceLadder()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Swatch(MaterialTheme.colorScheme.primary)
                Swatch(MaterialTheme.colorScheme.primaryContainer)
                Swatch(MaterialTheme.colorScheme.secondaryContainer)
                Swatch(MaterialTheme.colorScheme.tertiary)
                Swatch(MaterialTheme.colorScheme.tertiaryContainer)
                Swatch(MaterialTheme.colorScheme.errorContainer)
            }
        }
    }
}

@Composable
private fun SurfaceLadder() {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(
            cs.surfaceContainerLowest,
            cs.surfaceContainerLow,
            cs.surfaceContainer,
            cs.surfaceContainerHigh,
            cs.surfaceContainerHighest,
        ).forEach { tone ->
            Box(
                Modifier
                    .weight(1f)
                    .height(32.dp)
                    .background(tone, MaterialTheme.shapes.small)
            )
        }
    }
}

@Composable
private fun RowScope.Swatch(color: Color) {
    Box(
        Modifier
            .weight(1f)
            .height(32.dp)
            .background(color, MaterialTheme.shapes.small)
    )
}

@Preview(name = "Dark (Brand)", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ThemePreviewDark() {
    BrutusTheme(darkTheme = true) { ThemeSpecimen() }
}

@Preview(name = "Light (Brand)", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun ThemePreviewLight() {
    BrutusTheme(darkTheme = false) { ThemeSpecimen() }
}

@Preview(name = "Dynamic (Material You)", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, wallpaper = androidx.compose.ui.tooling.preview.Wallpapers.RED_DOMINATED_EXAMPLE)
@Composable
private fun ThemePreviewDynamic() {
    // Dynamic color is DataStore-gated at runtime; previews approximate it via
    // the wallpaper parameter + brand fallback below API 31.
    BrutusTheme(darkTheme = true) { ThemeSpecimen() }
}
