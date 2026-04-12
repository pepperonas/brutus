package com.pepperonas.brutus.util

import kotlin.math.PI
import kotlin.math.sin

object AlarmSoundGenerator {

    const val SAMPLE_RATE = 44100

    /**
     * Generates a seamlessly loopable 16-bit PCM mono buffer for the given sound.
     */
    fun generatePcm(sound: AlarmSound): ShortArray {
        return when (sound) {
            AlarmSound.SYSTEM -> ShortArray(0) // handled via RingtoneManager
            AlarmSound.KLAXON -> klaxon()
            AlarmSound.SIREN -> siren()
            AlarmSound.NUCLEAR -> nuclear()
            AlarmSound.FOGHORN -> foghorn()
            AlarmSound.PIERCING -> piercing()
        }
    }

    // 600ms: 300ms @ 600Hz square, 300ms @ 900Hz square
    private fun klaxon(): ShortArray {
        val durMs = 600
        val samples = SAMPLE_RATE * durMs / 1000
        val out = ShortArray(samples)
        val half = samples / 2
        for (i in 0 until samples) {
            val freq = if (i < half) 600.0 else 900.0
            val period = SAMPLE_RATE / freq
            val phase = (i % period.toInt()).toDouble() / period
            out[i] = if (phase < 0.5) Short.MAX_VALUE else Short.MIN_VALUE
        }
        return applyEdgeFade(out, 50)
    }

    // 2000ms sweep 400Hz -> 1200Hz -> 400Hz, sine wave
    private fun siren(): ShortArray {
        val durMs = 2000
        val samples = SAMPLE_RATE * durMs / 1000
        val out = ShortArray(samples)
        var phase = 0.0
        for (i in 0 until samples) {
            val t = i.toDouble() / samples
            val sweep = if (t < 0.5) t * 2 else (1.0 - t) * 2
            val freq = 400.0 + 800.0 * sweep
            phase += 2 * PI * freq / SAMPLE_RATE
            out[i] = (sin(phase) * Short.MAX_VALUE * 0.95).toInt().toShort()
        }
        return out
    }

    // 200ms: 100ms @ 1000Hz, 100ms silence
    private fun nuclear(): ShortArray {
        val durMs = 200
        val samples = SAMPLE_RATE * durMs / 1000
        val out = ShortArray(samples)
        val half = samples / 2
        for (i in 0 until half) {
            val freq = 1000.0
            val period = SAMPLE_RATE / freq
            val phase = (i % period.toInt()).toDouble() / period
            out[i] = if (phase < 0.5) Short.MAX_VALUE else Short.MIN_VALUE
        }
        return applyEdgeFade(out, 20)
    }

    // 1500ms: 1000ms @ 120Hz sine + modulation, 500ms silence
    private fun foghorn(): ShortArray {
        val durMs = 1500
        val samples = SAMPLE_RATE * durMs / 1000
        val out = ShortArray(samples)
        val tone = samples * 2 / 3
        for (i in 0 until tone) {
            val t = i.toDouble() / SAMPLE_RATE
            val carrier = sin(2 * PI * 120.0 * t)
            val mod = 0.7 + 0.3 * sin(2 * PI * 4.0 * t)
            out[i] = (carrier * mod * Short.MAX_VALUE * 0.9).toInt().toShort()
        }
        return applyEdgeFade(out, 100)
    }

    // 200ms continuous 3500Hz square with amplitude pulse
    private fun piercing(): ShortArray {
        val durMs = 400
        val samples = SAMPLE_RATE * durMs / 1000
        val out = ShortArray(samples)
        val freq = 3500.0
        val period = SAMPLE_RATE / freq
        for (i in 0 until samples) {
            val phase = (i % period.toInt()).toDouble() / period
            val base = if (phase < 0.5) Short.MAX_VALUE.toInt() else Short.MIN_VALUE.toInt()
            val pulse = 0.6 + 0.4 * sin(2 * PI * 8.0 * i / SAMPLE_RATE)
            out[i] = (base * pulse).toInt().toShort()
        }
        return out
    }

    /** Fade edges to avoid clicks at loop boundary. */
    private fun applyEdgeFade(buf: ShortArray, fadeSamples: Int): ShortArray {
        val n = minOf(fadeSamples, buf.size / 4)
        for (i in 0 until n) {
            val gain = i.toFloat() / n
            buf[i] = (buf[i] * gain).toInt().toShort()
            buf[buf.size - 1 - i] = (buf[buf.size - 1 - i] * gain).toInt().toShort()
        }
        return buf
    }
}
