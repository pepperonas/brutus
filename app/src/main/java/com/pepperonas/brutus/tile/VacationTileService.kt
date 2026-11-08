package com.pepperonas.brutus.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.pepperonas.brutus.R
import com.pepperonas.brutus.scheduler.AlarmActions
import com.pepperonas.brutus.ui.screens.HomeRequest
import com.pepperonas.brutus.util.VacationDates
import com.pepperonas.brutus.util.VacationStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Quick-settings tile for vacation mode. Active = alarms are paused; a tap ends the pause at once.
 * Inactive: a tap opens the app's date picker — starting a pause needs a last day, and a tile has
 * no room to ask for one.
 */
class VacationTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        if (VacationStore.pausedUntil(this) != null) {
            CoroutineScope(Dispatchers.IO).launch {
                AlarmActions.resumeFromVacation(applicationContext)
                withContext(Dispatchers.Main) { render() }
            }
        } else {
            openPicker()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openPicker() {
        val intent = HomeRequest.VACATION.intent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun render() {
        val tile = qsTile ?: return
        val until = VacationStore.pausedUntil(this)
        tile.state = if (until != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_vacation_label)
        val subtitle = if (until != null) {
            getString(R.string.tile_vacation_until, VacationDates.format(until - 1))
        } else {
            getString(R.string.tile_vacation_off)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = subtitle
        tile.contentDescription = "${tile.label}, $subtitle"
        tile.updateTile()
    }

    companion object {
        /** Lets the tile redraw after vacation mode changed somewhere else. */
        fun requestUpdate(context: Context) {
            try {
                requestListeningState(context, ComponentName(context, VacationTileService::class.java))
            } catch (_: Exception) {
                // Not added to quick settings, or the system refuses — it renders on next open.
            }
        }
    }
}
