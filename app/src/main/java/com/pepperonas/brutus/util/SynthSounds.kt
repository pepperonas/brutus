package com.pepperonas.brutus.util

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * The sounds added in v2.5.0 — chosen by ear from twenty candidates. Kept apart from
 * [AlarmSoundGenerator] because they share a small toolkit the older sounds don't use.
 *
 * Same contract as every sound: 44.1 kHz, 16-bit mono, one loopable buffer of at most 6 s,
 * deterministic (randomness only from a fixed seed), centred; harsh ones at 95 %, gentle ones at
 * ≤ 60 % of full scale.
 *
 * Gentle sounds are exactly periodic in their loop — Sunrise plays one for ten minutes, and a dip or
 * click every six seconds would spoil it. Decaying notes are rendered *circularly* (a tail wraps round
 * to the start of the loop, [addCircular]); sustained tones use frequencies with a whole number of
 * cycles per loop ([periodic]).
 */
internal object SynthSounds {

    private const val SR = AlarmSoundGenerator.SAMPLE_RATE
    private const val TWO_PI = 2 * PI

    private const val HARSH_PEAK = 0.95
    private const val GENTLE_PEAK = 0.60

    // ---- building blocks ----------------------------------------------------------------------

    private fun buffer(seconds: Double) = DoubleArray((seconds * SR).roundToInt())

    /** Nearest frequency with a whole number of cycles in [loopSec] — sustained tones stay seamless. */
    private fun periodic(freq: Double, loopSec: Double) = (freq * loopSec).roundToInt() / loopSec

    /** Removes DC, scales to [peak] of full scale, optionally fades the loop edges, converts to 16 bit. */
    private fun finish(buf: DoubleArray, peak: Double, fadeMs: Int = 0): ShortArray {
        val mean = buf.average()
        for (i in buf.indices) buf[i] -= mean
        val max = buf.maxOf { abs(it) }.coerceAtLeast(1e-9)
        val scale = peak * Short.MAX_VALUE / max
        val fade = minOf(SR * fadeMs / 1000, buf.size / 4)
        return ShortArray(buf.size) { i ->
            var g = 1.0
            if (fade > 0) {
                if (i < fade) g = i.toDouble() / fade
                if (buf.size - 1 - i < fade) g = minOf(g, (buf.size - 1 - i).toDouble() / fade)
            }
            (buf[i] * scale * g).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    /** Band-limited-ish sawtooth from the first [n] harmonics at phase [p] (in cycles). */
    private fun saw(p: Double, n: Int): Double {
        var s = 0.0
        for (k in 1..n) s += sin(TWO_PI * k * p) / k
        return s
    }

    /** Odd-harmonic square from the first [n] odd harmonics. */
    private fun square(p: Double, n: Int): Double {
        var s = 0.0
        var k = 1
        repeat(n) { s += sin(TWO_PI * k * p) / k; k += 2 }
        return s
    }

    /**
     * Adds a decaying note circularly: its tail past the end of the loop wraps round to the start,
     * so a loop of such notes repeats without a seam.
     */
    private fun addCircular(buf: DoubleArray, startSec: Double, lengthSec: Double, voice: (Double) -> Double) {
        val n = buf.size
        val start = (startSec * SR).roundToInt()
        val len = (lengthSec * SR).roundToInt()
        for (j in 0 until len) buf[(start + j) % n] += voice(j.toDouble() / SR)
    }

    /** Soft attack (raised cosine) then exponential decay. */
    private fun env(t: Double, attack: Double, tau: Double): Double {
        val a = if (t < attack) 0.5 - 0.5 * kotlin.math.cos(PI * t / attack) else 1.0
        return a * exp(-t / tau)
    }

    private fun note(midi: Double) = 440.0 * 2.0.pow((midi - 69) / 12)

    // ---- harsh --------------------------------------------------------------------------------

    fun airRaid(): ShortArray {
        val b = buffer(6.0)
        var p = 0.0
        for (i in b.indices) {
            val t = i.toDouble() / SR
            val up = ((t / 2.4).coerceAtMost(1.0))
            val down = ((t - 3.6) / 2.4).coerceIn(0.0, 1.0)
            val shape = (0.5 - 0.5 * kotlin.math.cos(PI * up)) * (1 - (0.5 - 0.5 * kotlin.math.cos(PI * down)))
            val f = 160.0 + 560.0 * shape + 6.0 * sin(TWO_PI * 5.5 * t)
            p += f / SR
            b[i] = tanh(2.2 * saw(p, 8))
        }
        return finish(b, HARSH_PEAK, 30)
    }

    fun dive(): ShortArray {
        val b = buffer(1.4)
        var p = 0.0
        for (i in b.indices) {
            val t = i.toDouble() / SR
            val f = when {
                t < 0.12 -> 190.0 + (300.0 - 190.0) * t / 0.12
                t < 0.75 -> 300.0 + (460.0 - 300.0) * ((t - 0.12) / 0.63).pow(0.5)
                else -> 460.0 - 50.0 * ((t - 0.75) / 0.65)
            }
            p += f / SR
            val amp = minOf(1.0, t / 0.02) * if (t > 1.25) (1.4 - t) / 0.15 else 1.0
            b[i] = tanh(3.0 * square(p, 9)) * amp
        }
        return finish(b, HARSH_PEAK, 10)
    }

    fun carAlarm(): ShortArray {
        val b = buffer(6.0)
        var p = 0.0
        for (i in b.indices) {
            val t = i.toDouble() / SR
            val seg = floor(t / 1.5).toInt()
            val u = t - seg * 1.5
            var gate = 1.0
            val f = when (seg) {
                0 -> 900.0 + 600.0 * sin(TWO_PI * 1.3 * u)                        // wail
                1 -> 700.0 + 900.0 * ((u * 9.0) % 1.0)                             // yelp
                2 -> { gate = if ((u * 6.0) % 1.0 < 0.55) 1.0 else 0.0; 420.0 }   // horn
                else -> if ((u * 12.0) % 1.0 < 0.5) 1150.0 else 850.0              // warble
            }
            p += f / SR
            b[i] = square(p, 7) * gate
        }
        return finish(b, HARSH_PEAK, 5)
    }

    fun schoolBell(): ShortArray {
        val b = buffer(3.0)
        val partials = doubleArrayOf(1.0, 2.76, 5.40, 8.93)
        val base = 1480.0
        val tau = doubleArrayOf(0.35, 0.22, 0.14, 0.09)
        val a = DoubleArray(partials.size)
        val strike = SR / 22
        for (i in b.indices) {
            if (i % strike == 0 && i < SR * 25 / 10) for (k in a.indices) a[k] += 1.0
            val t = i.toDouble() / SR
            var s = 0.0
            for (k in a.indices) {
                s += a[k] * sin(TWO_PI * base * partials[k] * t) / (k + 1)
                a[k] *= exp(-1.0 / (tau[k] * SR))
            }
            b[i] = s
        }
        return finish(b, HARSH_PEAK, 20)
    }

    fun reverseBeeper(): ShortArray {
        val b = buffer(4.0)
        var beat = 0.0
        var p = 0.0
        for (i in b.indices) {
            val t = i.toDouble() / SR
            val rate = 2.0 + 10.0 * (t / 4.0).pow(2)
            beat += rate / SR
            p += 1040.0 / SR
            b[i] = if (beat % 1.0 < 0.5) tanh(4.0 * square(p, 5)) else 0.0
        }
        return finish(b, HARSH_PEAK, 5)
    }

    fun shepard(): ShortArray {
        val loop = 6.0
        val b = buffer(loop)
        val voices = 8
        val low = 55.0
        val phase = DoubleArray(voices)
        for (i in b.indices) {
            val t = i.toDouble() / SR
            var s = 0.0
            for (v in 0 until voices) {
                val x = (v + t / loop) % voices
                val f = low * 2.0.pow(x)
                phase[v] += f / SR
                val g = exp(-((x - voices / 2.0).pow(2)) / 3.0)
                s += g * saw(phase[v], 3)
            }
            b[i] = tanh(1.6 * s)
        }
        return finish(b, HARSH_PEAK, 20)
    }

    fun steelHammer(): ShortArray {
        val b = buffer(3.0)
        val rnd = Random(4711)
        val hits = doubleArrayOf(0.0, 0.34, 0.49, 1.08, 1.30, 1.43, 2.12, 2.55)
        val partials = doubleArrayOf(420.0, 1130.0, 2210.0, 3470.0, 5230.0)
        for (h in hits) {
            val gain = 0.7 + rnd.nextDouble() * 0.3
            addCircular(b, h, 1.2) { t ->
                var s = 0.0
                for ((k, f) in partials.withIndex()) s += sin(TWO_PI * f * t) * exp(-t / (0.5 / (k + 1)))
                val noise = if (t < 0.012) (rnd.nextDouble() * 2 - 1) * 2.0 * (1 - t / 0.012) else 0.0
                gain * (s + noise)
            }
        }
        for (i in b.indices) b[i] = tanh(1.8 * b[i])
        return finish(b, HARSH_PEAK)
    }

    fun strobe(): ShortArray {
        val b = buffer(5.0)
        var beat = 0.0
        var p = 0.0
        for (i in b.indices) {
            val t = i.toDouble() / SR
            val u = t / 5.0
            val rate = 4.0 + 26.0 * u * u
            beat += rate / SR
            val f = 1500.0 + 2500.0 * u
            p += f / SR
            val on = beat % 1.0 < 0.45
            b[i] = if (on) square(p, 5) else 0.0
        }
        return finish(b, HARSH_PEAK, 5)
    }

    fun whoop(): ShortArray {
        val b = buffer(2.4)
        var p = 0.0
        for (i in b.indices) {
            val t = i.toDouble() / SR
            val u = (t % 0.8) / 0.8
            val f = 380.0 + 1100.0 * u.pow(0.7)
            p += f / SR
            val amp = if (u > 0.94) (1 - u) / 0.06 else minOf(1.0, u / 0.01)
            b[i] = tanh(2.5 * square(p, 7)) * amp
        }
        return finish(b, HARSH_PEAK, 5)
    }

    // ---- gentle -------------------------------------------------------------------------------

    fun singingBowl(): ShortArray {
        val b = buffer(6.0)
        val f0 = 196.0
        addCircular(b, 0.0, 17.0) { t ->
            val e = env(t, 0.08, 3.2)
            (sin(TWO_PI * f0 * t) + sin(TWO_PI * (f0 + 0.9) * t) +
                0.45 * sin(TWO_PI * f0 * 2.71 * t) * exp(-t / 1.6) +
                0.2 * sin(TWO_PI * f0 * 5.1 * t) * exp(-t / 0.8)) * e
        }
        return finish(b, GENTLE_PEAK)
    }

    fun birds(): ShortArray {
        val b = buffer(6.0)
        val rnd = Random(1234)
        var t0 = 0.05
        while (t0 < 5.6) {
            val calls = 2 + rnd.nextInt(3)
            val base = 2800.0 + rnd.nextDouble() * 1400.0
            val len = 0.06 + rnd.nextDouble() * 0.06
            val gain = 0.5 + rnd.nextDouble() * 0.5
            repeat(calls) { c ->
                addCircular(b, t0 + c * (len + 0.05), len) { t ->
                    val u = t / len
                    val f = base * (1 + 0.25 * u) + 180.0 * sin(TWO_PI * 38.0 * t)
                    gain * sin(PI * u).pow(2) * sin(TWO_PI * f * t)
                }
            }
            t0 += calls * (len + 0.05) + 0.4 + rnd.nextDouble() * 0.9
        }
        return finish(b, 0.45)
    }

    fun windChimes(): ShortArray {
        val b = buffer(6.0)
        val rnd = Random(777)
        val scale = doubleArrayOf(84.0, 86.0, 88.0, 91.0, 93.0, 96.0) // C6 D6 E6 G6 A6 C7
        var t0 = 0.0
        while (t0 < 6.0) {
            val f = note(scale[rnd.nextInt(scale.size)])
            val gain = 0.4 + rnd.nextDouble() * 0.6
            addCircular(b, t0, 5.0) { t ->
                gain * env(t, 0.004, 1.3) * (sin(TWO_PI * f * t) + 0.3 * sin(TWO_PI * f * 2.76 * t) * exp(-t / 0.4))
            }
            t0 += 0.25 + rnd.nextDouble() * 0.6
        }
        return finish(b, GENTLE_PEAK)
    }

    fun kalimba(): ShortArray {
        val b = buffer(6.0)
        val motif = doubleArrayOf(67.0, 71.0, 74.0, 79.0, 76.0, 74.0, 71.0, 74.0) // G B D G E D B D
        val step = 6.0 / motif.size
        motif.forEachIndexed { n, m ->
            val f = note(m)
            addCircular(b, n * step, 3.0) { t ->
                env(t, 0.005, 0.8) * (sin(TWO_PI * f * t) + 0.35 * sin(TWO_PI * f * 5.4 * t) * exp(-t / 0.06))
            }
        }
        return finish(b, GENTLE_PEAK)
    }

    fun harp(): ShortArray {
        val b = buffer(6.0)
        val arps = listOf(doubleArrayOf(60.0, 64.0, 67.0, 71.0, 72.0, 76.0, 79.0, 83.0), doubleArrayOf(53.0, 57.0, 60.0, 64.0, 65.0, 69.0, 72.0, 76.0))
        arps.forEachIndexed { a, notes ->
            notes.forEachIndexed { n, m ->
                val f = note(m)
                addCircular(b, a * 3.0 + n * 0.3, 4.0) { t ->
                    env(t, 0.006, 1.4) * (sin(TWO_PI * f * t) + 0.4 * sin(TWO_PI * 2 * f * t) * exp(-t / 0.3) +
                        0.15 * sin(TWO_PI * 3 * f * t) * exp(-t / 0.15))
                }
            }
        }
        return finish(b, GENTLE_PEAK)
    }

    fun ocean(): ShortArray {
        val loop = 6.0
        val b = buffer(loop)
        val rnd = Random(99)
        // Filter one full extra loop first, so the filter state at the seam matches the start.
        val n = b.size
        var lp1 = 0.0
        var lp2 = 0.0
        val noise = DoubleArray(n) { rnd.nextDouble() * 2 - 1 }
        for (pass in 0..1) {
            for (i in 0 until n) {
                val t = i.toDouble() / SR
                val swell = 0.5 - 0.5 * kotlin.math.cos(TWO_PI * t / loop)
                val cutoff = 300.0 + 1500.0 * swell
                val a = 1 - exp(-TWO_PI * cutoff / SR)
                lp1 += a * (noise[i] - lp1)
                lp2 += a * (lp1 - lp2)
                if (pass == 1) b[i] = lp2 * (0.2 + 0.8 * swell)
            }
        }
        return finish(b, GENTLE_PEAK)
    }

    fun electricPiano(): ShortArray {
        val b = buffer(6.0)
        val chords = listOf(doubleArrayOf(53.0, 57.0, 60.0, 64.0), doubleArrayOf(52.0, 55.0, 59.0, 62.0), doubleArrayOf(50.0, 53.0, 57.0, 60.0), doubleArrayOf(48.0, 52.0, 55.0, 59.0))
        chords.forEachIndexed { c, notes ->
            notes.forEachIndexed { k, m ->
                val f = note(m)
                addCircular(b, c * 1.5 + k * 0.02, 5.0) { t ->
                    val index = 1.8 * exp(-t / 0.35) + 0.2
                    env(t, 0.008, 1.8) * sin(TWO_PI * f * t + index * sin(TWO_PI * f * t))
                }
            }
        }
        return finish(b, GENTLE_PEAK)
    }

    fun sunrise(): ShortArray {
        val loop = 6.0
        val b = buffer(loop)
        val roots = doubleArrayOf(50.0, 57.0, 62.0, 66.0) // D3 A3 D4 F#4
        val rnd = Random(66)
        val ph = List(roots.size) { DoubleArray(6) { rnd.nextDouble() } }
        for (i in b.indices) {
            val t = i.toDouble() / SR
            val bright = 0.5 - 0.5 * kotlin.math.cos(TWO_PI * t / loop) // opens and settles each loop
            var s = 0.0
            roots.forEachIndexed { r, m ->
                val f = periodic(note(m), loop)
                for (h in 1..6) {
                    val w = if (h == 1) 1.0 else bright.pow(h / 2.0) / h
                    s += w * sin(TWO_PI * (f * h * t + ph[r][h - 1]))
                }
            }
            b[i] = s
        }
        return finish(b, GENTLE_PEAK)
    }
}
