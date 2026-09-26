package com.pepperonas.brutus.update

/**
 * Release tags ("v2.3.0") against the installed versionName ("2.2.0").
 * Parts compare as numbers; a leading `v` and any `-pre`/`+build` suffix are ignored.
 * Anything unparsable is never "newer" — a broken response must not produce a notification.
 */
object AppVersion {

    private val CORE = Regex("""\d+(\.\d+)*""")

    fun parse(raw: String?): List<Int>? {
        val s = raw?.trim()?.removePrefix("v")?.removePrefix("V") ?: return null
        val core = s.substringBefore('-').substringBefore('+')
        if (!CORE.matches(core)) return null
        return core.split('.').map { it.toIntOrNull() ?: return null }
    }

    fun isNewer(candidate: String?, installed: String?): Boolean {
        val a = parse(candidate) ?: return false
        val b = parse(installed) ?: return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** "v2.3.0" → "2.3.0", for text the user reads. */
    fun display(raw: String): String = raw.trim().removePrefix("v").removePrefix("V")
}
