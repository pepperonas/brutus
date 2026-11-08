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

    /** Where a crescendo starts, in dB below full volume. -30 dB is quiet but clearly audible. */
    const val CRESCENDO_START_DB = -30f

    /** Gain while a call rings or runs — the user must still hear the other person. */
    const val IN_CALL_GAIN = 0.15f

    /** How often an unanswered alarm snoozes itself before it gives up and reports itself missed. */
    const val MAX_AUTO_SNOOZES = 3

    /** Shortest pause of an automatic snooze — also for alarms whose own snooze is switched off. */
    const val AUTO_SNOOZE_MIN_MINUTES = 5

    /** How often the service re-evaluates gain, call state and ring limit. */
    const val TICK_MS = 250L

    /**
     * Gain of a crescendo [elapsedMs] into the ring. Rises evenly in dB (what the ear perceives as
     * evenly louder) from [CRESCENDO_START_DB] to 0 dB over [rampSeconds]; 0 = no crescendo.
     */
    fun crescendoGain(elapsedMs: Long, rampSeconds: Int): Float {
        if (rampSeconds <= 0) return 1f
        val t = (elapsedMs.coerceAtLeast(0L).toFloat() / (rampSeconds * 1000f)).coerceAtMost(1f)
        val db = CRESCENDO_START_DB * (1f - t)
        return 10f.pow(db / 20f)
    }

    /** The gain to play at right now. */
    fun gain(elapsedMs: Long, rampSeconds: Int, hardcore: Boolean, inCall: Boolean): Float {
        val base = if (hardcore) 1f else crescendoGain(elapsedMs, rampSeconds)
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
