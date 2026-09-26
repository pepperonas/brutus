package com.pepperonas.brutus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.pepperonas.brutus.util.RingingStore
import com.pepperonas.brutus.util.Storage
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
                pendingResult?.finish()
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
        val skips = RingingStore.skips(context)
        val next = NextAlarmCalculator.findNext(alarms, now, skips)
        val triggerAt = next?.let { NextAlarmCalculator.nextTrigger(it, now, skips[it.id]) }

        val views = RemoteViews(context.packageName, R.layout.widget_next_alarm)
        if (next == null || triggerAt == null) {
            views.setTextViewText(R.id.widget_time, "—")
            views.setTextViewText(R.id.widget_countdown, context.getString(R.string.widget_no_alarm))
            views.setTextViewText(R.id.widget_days, "")
        } else {
            views.setTextViewText(R.id.widget_time, next.timeString())
            views.setTextViewText(R.id.widget_countdown, formatRelative(context, triggerAt, now))
            views.setTextViewText(
                R.id.widget_days,
                formatDays(context, next.repeatDays, triggerAt)
            )
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
            // Before the first unlock after a reboot every AppWidgetManager call throws — and the
            // ringing service calls this right after an alarm fires. Found on the emulator: the
            // exception killed the service a second into the alarm. The widget updates itself on
            // its next tick once the user has unlocked.
            if (!Storage.isUserUnlocked(context)) return
            val ids = try {
                AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, NextAlarmWidget::class.java)
                )
            } catch (_: IllegalStateException) {
                return
            }
            if (ids.isEmpty()) return
            val intent = Intent(context, NextAlarmWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        // internal (not private) so the unit tests can exercise the exact strings
        // the user reads on the home screen.
        internal fun formatRelative(context: Context, target: Long, now: Long): String {
            val diff = (target - now).coerceAtLeast(0L)
            val mins = diff / 60_000L
            val hours = mins / 60
            val days = (hours / 24).toInt()
            return when {
                days >= 1 -> context.resources.getQuantityString(
                    R.plurals.widget_in_days, days, days
                )
                hours >= 1 -> context.getString(
                    R.string.widget_in_hours, hours.toInt(), (mins % 60).toInt()
                )
                mins >= 1 -> context.getString(R.string.widget_in_minutes, mins.toInt())
                else -> context.getString(R.string.widget_now)
            }
        }

        internal fun formatDays(context: Context, bitmask: Int, triggerAt: Long): String {
            if (bitmask == 0) {
                // Pattern and locale both come from resources so the weekday
                // reads natively in whichever language the widget is showing.
                val fmt = SimpleDateFormat(
                    context.getString(R.string.format_widget_weekday),
                    Locale.getDefault()
                )
                return fmt.format(Date(triggerAt))
            }
            if (bitmask == 0x7F) return context.getString(R.string.widget_daily)
            val labels = context.resources.getStringArray(R.array.weekday_short)
            return labels.filterIndexed { i, _ -> (bitmask and (1 shl i)) != 0 }
                .joinToString(" ")
        }
    }
}
