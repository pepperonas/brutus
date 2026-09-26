package com.pepperonas.brutus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pepperonas.brutus.scheduler.AlarmActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Buttons on the snooze and heads-up notifications. Not exported: only our PendingIntents reach it. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", -1L)
        if (alarmId == -1L) return
        val app = context.applicationContext
        when (intent.action) {
            ACTION_CANCEL_SNOOZE -> AlarmActions.cancelSnooze(app, alarmId)
            ACTION_SKIP_NEXT -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try { AlarmActions.skipNext(app, alarmId, solvedChallenge = false) } finally { pending?.finish() }
                }
            }
        }
    }

    companion object {
        const val ACTION_CANCEL_SNOOZE = "com.pepperonas.brutus.CANCEL_SNOOZE"
        const val ACTION_SKIP_NEXT = "com.pepperonas.brutus.SKIP_NEXT"
    }
}
