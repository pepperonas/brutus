package com.pepperonas.brutus.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_settings")

/**
 * Persisted theme preferences. Default is the Brutus brand scheme (red seed);
 * Material You dynamic color is a deliberate opt-in (API 31+).
 */
object ThemeSettings {
    private val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")

    fun dynamicColorFlow(context: Context): Flow<Boolean> =
        context.themeDataStore.data.map { it[DYNAMIC_COLOR] ?: false }

    suspend fun setDynamicColor(context: Context, enabled: Boolean) {
        context.themeDataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }
}
