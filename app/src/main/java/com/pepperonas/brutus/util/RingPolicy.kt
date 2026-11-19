package com.pepperonas.brutus.util

import android.media.AudioManager
import kotlin.math.min
import kotlin.math.pow

/**
 * The rules of a ringing session, free of Android objects so they can be tested on their own:
 * how loud the sound plays right now (crescendo, a phone call), and what happens when nobody
 * reacts for too long.
 *
 * Hardcore alarms keep their old behaviour: full volume from the first second, and they never
 * stop on their own. A phone call quiets every alarm, Hardcore included — the challenge still has
 * to be solved, and the vibration keeps going.
 */
object RingPolicy {

    /** Where a crescendo starts, in dB relative to target volume. -30 dB is quiet but clearly audible. */
    const val CRESCENDO_START_DB = -30f

    /** Target dB at the minimum 5 % volume setting (~1 % amplitude, -40 dB). */
    const val MIN_VOLUME_DB = -40f

    /** Gain while a call rings or runs — the user must still hear the other person. */
    const val IN_CALL_GAIN = 0.15f

    /** How often an unanswered alarm snoozes itself before it gives up and reports itself missed. */
    const val MAX_AUTO_SNOOZES = 3

    /** Shortest pause of an automatic snooze — also for alarms whose own snooze is switched off. */
    const val AUTO_SNOOZE_MIN_MINUTES = 5

    /** How often the service re-evaluates gain, call state and ring limit. */
    const val TICK_MS = 250L

    /**
     * Target ceiling in dB for [volumePercent] (5..100). Scaled evenly in dB from [MIN_VOLUME_DB]
     * at 5 % to 0 dB at 100 %, so each slider step feels equally loud to the human ear.
     */
    fun volumeDb(volumePercent: Int): Float {
        val clamped = volumePercent.coerceIn(AppSettings.MIN_ALARM_VOLUME, AppSettings.MAX_ALARM_VOLUME)
        if (clamped >= AppSettings.MAX_ALARM_VOLUME) return 0f
        val fraction = (clamped - AppSettings.MIN_ALARM_VOLUME).toFloat() /
            (AppSettings.MAX_ALARM_VOLUME - AppSettings.MIN_ALARM_VOLUME).toFloat()
        return MIN_VOLUME_DB * (1f - fraction)
    }

    /** Player gain for the chosen [volumePercent] (5..100) without a crescendo. */
    fun volumeGain(volumePercent: Int): Float =
        if (volumePercent >= AppSettings.MAX_ALARM_VOLUME) 1f else 10f.pow(volumeDb(volumePercent) / 20f)

    /**
     * Gain of a crescendo [elapsedMs] into the ring. Rises evenly in dB (what the ear perceives as
     * evenly louder) from [CRESCENDO_START_DB] below [volumeDb] up to [volumeDb] over [rampSeconds]; 0 = no crescendo.
     */
    fun crescendoGain(elapsedMs: Long, rampSeconds: Int, volumePercent: Int = AppSettings.DEFAULT_ALARM_VOLUME): Float {
        val targetDb = volumeDb(volumePercent)
        if (rampSeconds <= 0) return if (targetDb == 0f) 1f else 10f.pow(targetDb / 20f)
        val t = (elapsedMs.coerceAtLeast(0L).toFloat() / (rampSeconds * 1000f)).coerceAtMost(1f)
        val db = targetDb + CRESCENDO_START_DB * (1f - t)
        return 10f.pow(db / 20f)
    }

    /** The gain to play at right now. Hardcore ignores both crescendo and reduced volume. */
    fun gain(
        elapsedMs: Long,
        rampSeconds: Int,
        hardcore: Boolean,
        inCall: Boolean,
        volumePercent: Int = AppSettings.DEFAULT_ALARM_VOLUME,
    ): Float {
        val base = if (hardcore) 1f else crescendoGain(elapsedMs, rampSeconds, volumePercent)
        return if (inCall) min(base, IN_CALL_GAIN) else base
    }

    /** Whether the phone is ringing, in a call, or screening one ([AudioManager.getMode]). */
    fun isInCall(audioMode: Int): Boolean = audioMode in CALL_MODES

    /** Whether an alarm that has rung [elapsedMs] has reached its limit; Hardcore has none. */
    fun ringLimitReached(elapsedMs: Long, limitMinutes: Int, hardcore: Boolean): Boolean =
        !hardcore && limitMinutes > 0 && elapsedMs >= limitMinutes * 60_000L

    enum class LimitOutcome { SNOOZE, GIVE_UP }

    /** What to do at the limit, given how often this alarm has already snoozed itself. */
    fun afterLimit(autoSnoozesSoFar: Int): LimitOutcome =
        if (autoSnoozesSoFar < MAX_AUTO_SNOOZES) LimitOutcome.SNOOZE else LimitOutcome.GIVE_UP

    // MODE_CALL_SCREENING (4), MODE_CALL_REDIRECT (5) and MODE_COMMUNICATION_REDIRECT (6) are
    // newer than minSdk; their values are fixed, so the literals are safe on every version.
    private val CALL_MODES = setOf(
        AudioManager.MODE_RINGTONE,
        AudioManager.MODE_IN_CALL,
        AudioManager.MODE_IN_COMMUNICATION,
        4, 5, 6,
    )
}
