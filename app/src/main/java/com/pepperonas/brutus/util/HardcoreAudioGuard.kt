package com.pepperonas.brutus.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Keeps STREAM_ALARM clamped at maximum while a hardcore-mode alarm is ringing.
 *
 * Android has no API to "lock" a stream volume, but every user-initiated
 * volume change broadcasts android.media.VOLUME_CHANGED_ACTION. We listen
 * for that and immediately re-apply the max, making the volume keys feel
 * inert. Volume-down long-presses still register as single events, but
 * they get snapped back within a few milliseconds.
 *
 * Call [attach] in the hosting Activity's onStart/onCreate and [detach]
 * in onStop/onDestroy. Safe to attach/detach multiple times.
 */
class HardcoreAudioGuard(private val context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var receiver: BroadcastReceiver? = null
    private var attached = false

    fun attach() {
        if (attached) return
        clampToMax()

        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == VOLUME_CHANGED_ACTION) {
                    val stream = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1)
                    if (stream == AudioManager.STREAM_ALARM) clampToMax()
                }
            }
        }
        val filter = IntentFilter(VOLUME_CHANGED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(
                context, r, filter, ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(r, filter)
        }
        receiver = r
        attached = true
    }

    fun detach() {
        if (!attached) return
        receiver?.let {
            try { context.unregisterReceiver(it) } catch (_: IllegalArgumentException) { }
        }
        receiver = null
        attached = false
    }

    fun clampToMax() {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
    }

    companion object {
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
    }
}
