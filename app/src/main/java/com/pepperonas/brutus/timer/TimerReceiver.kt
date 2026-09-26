package com.pepperonas.brutus.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** AlarmManager's "time is up" and the buttons on the timer notification. Not exported. */
class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            ACTION_FIRE -> TimerController.fired(app)
            ACTION_PAUSE -> TimerController.pause(app)
            ACTION_RESUME -> TimerController.resume(app)
            ACTION_CANCEL, ACTION_STOP -> TimerController.cancel(app)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.pepperonas.brutus.timer.FIRE"
        const val ACTION_PAUSE = "com.pepperonas.brutus.timer.PAUSE"
        const val ACTION_RESUME = "com.pepperonas.brutus.timer.RESUME"
        const val ACTION_CANCEL = "com.pepperonas.brutus.timer.CANCEL"
        const val ACTION_STOP = "com.pepperonas.brutus.timer.STOP"
    }
}
