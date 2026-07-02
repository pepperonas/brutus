package com.pepperonas.brutus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.pepperonas.brutus.BrutusApplication
import com.pepperonas.brutus.MainActivity
import com.pepperonas.brutus.R
import com.pepperonas.brutus.util.NextAlarmCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home-screen widget showing the next upcoming Brutus alarm.
 *
 * Auto-refreshes every 30 minutes via the appwidget-provider config; manual
 * refreshes are also pushed by [refresh] whenever an alarm is added, toggled,
 * or deleted (called from the ViewModel).
 */
class NextAlarmWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // goAsync keeps the (possibly cold-started) process alive until the DB
        // read + RemoteViews push are done — without it the widget can get stuck
        // on the placeholder when the process is reaped right after onReceive.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onEnabled(context: Context) {
        // First widget instance — pre-populate so the user doesn't see the
        // initialLayout placeholder for the next 30-min update window.
        refresh(context)
    }

    private suspend fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
    ) {
        val app = context.applicationContext as BrutusApplication
        val alarms = app.database.alarmDao().getEnabledAlarms()
        val now = System.currentTimeMillis()
        val next = NextAlarmCalculator.findNext(alarms, now)
        val triggerAt = next?.let { NextAlarmCalculator.nextTrigger(it, now) }

        val views = RemoteViews(context.packageName, R.layout.widget_next_alarm)
        if (next == null || triggerAt == null) {
            views.setTextViewText(R.id.widget_time, "—")
            views.setTextViewText(R.id.widget_countdown, "kein Alarm")
            views.setTextViewText(R.id.widget_days, "")
        } else {
            views.setTextViewText(R.id.widget_time, next.timeString())
            views.setTextViewText(R.id.widget_countdown, formatRelative(triggerAt, now))
            views.setTextViewText(R.id.widget_days, formatDays(next.repeatDays, triggerAt))
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, widgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_time, pi)

        manager.updateAppWidget(widgetId, views)
    }

    companion object {
        /**
         * Forces a refresh of every Brutus widget on the home screen. Called by the
         * view model whenever the alarm set changes so the widget never lags
         * behind by 30 minutes.
         */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, NextAlarmWidget::class.java)
            )
            if (ids.isEmpty()) return
            val intent = Intent(context, NextAlarmWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        private fun formatRelative(target: Long, now: Long): String {
            val diff = (target - now).coerceAtLeast(0L)
            val mins = diff / 60_000L
            val hours = mins / 60
            val days = hours / 24
            return when {
                days >= 1 -> "in ${days} Tagen"
                hours >= 1 -> "in $hours Std ${mins % 60} Min"
                mins >= 1 -> "in $mins Min"
                else -> "gleich"
            }
        }

        private fun formatDays(bitmask: Int, triggerAt: Long): String {
            if (bitmask == 0) {
                val fmt = SimpleDateFormat("EEE", Locale.GERMAN)
                return fmt.format(Date(triggerAt))
            }
            if (bitmask == 0x7F) return "täglich"
            val labels = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
            return labels.filterIndexed { i, _ -> (bitmask and (1 shl i)) != 0 }
                .joinToString(" ")
        }
    }
}
