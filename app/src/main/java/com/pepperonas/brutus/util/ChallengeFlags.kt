package com.pepperonas.brutus.util

object ChallengeFlags {
    const val MATH = 1 shl 0
    const val SHAKE = 1 shl 1
    const val QR = 1 shl 2

    fun has(flags: Int, flag: Int): Boolean = (flags and flag) != 0

    fun describe(flags: Int): String {
        if (flags == 0) return "Keine"
        val parts = mutableListOf<String>()
        if (has(flags, MATH)) parts += "Mathe"
        if (has(flags, SHAKE)) parts += "Schuetteln"
        if (has(flags, QR)) parts += "QR-Code"
        return parts.joinToString(" + ")
    }

    /** Ordered list of active challenge flags (for sequential execution). */
    fun activeList(flags: Int): List<Int> = buildList {
        if (has(flags, MATH)) add(MATH)
        if (has(flags, SHAKE)) add(SHAKE)
        if (has(flags, QR)) add(QR)
    }
}
