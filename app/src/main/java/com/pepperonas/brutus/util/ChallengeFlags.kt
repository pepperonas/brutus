package com.pepperonas.brutus.util

import android.content.Context
import androidx.annotation.StringRes
import com.pepperonas.brutus.R

object ChallengeFlags {
    const val MATH = 1 shl 0
    const val SHAKE = 1 shl 1
    const val QR = 1 shl 2

    fun has(flags: Int, flag: Int): Boolean = (flags and flag) != 0

    /**
     * An alarm without any challenge can't exist — the alarm screen falls back
     * to a math challenge at runtime anyway, so persist that same fallback to
     * keep the list UI honest.
     */
    fun sanitize(flags: Int): Int = if (flags == 0) MATH else flags

    /** Name of a single challenge flag, as a string resource. */
    @StringRes
    fun labelOf(flag: Int): Int = when (flag) {
        MATH -> R.string.challenge_math
        SHAKE -> R.string.challenge_shake
        else -> R.string.challenge_qr
    }

    /** Human-readable list of the active challenges, e.g. "Math + Shake". */
    fun describe(context: Context, flags: Int): String {
        if (flags == 0) return context.getString(R.string.challenge_none)
        val parts = activeList(flags).map { context.getString(labelOf(it)) }
        if (parts.isEmpty()) return context.getString(R.string.challenge_none)
        return parts.joinToString(context.getString(R.string.challenge_separator))
    }

    /** Ordered list of active challenge flags (for sequential execution). */
    fun activeList(flags: Int): List<Int> = buildList {
        if (has(flags, MATH)) add(MATH)
        if (has(flags, SHAKE)) add(SHAKE)
        if (has(flags, QR)) add(QR)
    }
}
