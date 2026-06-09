package com.pepperonas.brutus.util

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object AlarmSoundGenerator {

    const val SAMPLE_RATE = 44100

    /**
     * Generates a seamlessly loopable 16-bit PCM mono buffer for the given sound.
     */
    fun generatePcm(sound: AlarmSound): ShortArray {
        return when (sound) {
            AlarmSound.SILENT -> ShortArray(0) // no audio at all
            AlarmSound.SYSTEM -> ShortArray(0) // handled via RingtoneManager
            AlarmSound.KLAXON -> klaxon()
            AlarmSound.SIREN -> siren()
            AlarmSound.NUCLEAR -> nuclear()
            AlarmSound.PIERCING -> piercing()
            AlarmSound.AIRHORN -> airhorn()
            AlarmSound.JACKHAMMER -> jackhammer()
            AlarmSound.FIRE_ALARM -> fireAlarm()
            AlarmSound.DENTIST -> dentist()
            AlarmSound.BANSHEE -> banshee()
            AlarmSound.CHIME -> chime()
            AlarmSound.MARIMBA -> marimba()
            AlarmSound.MORNING -> morning()
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

    /**
     * 900ms — three detuned sawtooth voices stacked into a brash stadium air-horn.
     * Sawtooths are harmonically dense, the slight detune adds a beating "blat".
     */
    private fun airhorn(): ShortArray {
        val durMs = 900
        val samples = SAMPLE_RATE * durMs / 1000
        val out = ShortArray(samples)
        val voices = doubleArrayOf(233.0, 311.0, 466.5) // Bb3, ~Eb4 (tritone-ish), Bb4
        val amp = Short.MAX_VALUE * 0.32
        for (i in 0 until samples) {
            val t = i.toDouble() / SAMPLE_RATE
            // gentle swell over the first 80ms so the loop point doesn't slam
            val swell = (t * 12.0).coerceAtMost(1.0)
            var sum = 0.0
            for (f in voices) {
                val phase = (t * f) % 1.0
                sum += 2.0 * phase - 1.0 // sawtooth
            }
            sum /= voices.size
            out[i] = (sum * swell * amp)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return applyEdgeFade(out, 40)
    }

    /**
     * 1000ms — a ~73Hz square gated into rapid on/off salvos, like a jackhammer
     * pounding concrete. Each hit gets a sharp attack; gaps make it rattle.
     */
    private fun jackhammer(): ShortArray {
        val durMs = 1000
        val samples = SAMPLE_RATE * durMs / 1000
        val out = ShortArray(samples)
        val freq = 73.0
        val period = SAMPLE_RATE / freq
        val hitMs = 28
        val gapMs = 22
        val cycleSamples = SAMPLE_RATE * (hitMs + gapMs) / 1000
        val hitSamples = SAMPLE_RATE * hitMs / 1000
        for (i in 0 until samples) {
            val inCycle = i % cycleSamples
            if (inCycle >= hitSamples) { out[i] = 0; continue }
            val phase = (i % period.toInt()).toDouble() / period
            // square fundamental + a clattering 5th harmonic for grit
            val sq = if (phase < 0.5) 1.0 else -1.0
            val grit = if ((phase * 5.0) % 1.0 < 0.5) 0.35 else -0.35
            val decay = 1.0 - inCycle.toDouble() / hitSamples * 0.4
            out[i] = ((sq * 0.7 + grit) * decay * Short.MAX_VALUE * 0.85)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }

    /**
     * 4000ms — the standardized T-3 smoke-alarm cadence: three 0.5s beeps, 0.5s
     * gaps, then a 1.5s pause before the loop repeats. Tone is a 3100Hz square.
     */
    private fun fireAlarm(): ShortArray {
        val durMs = 4000
        val samples = SAMPLE_RATE * durMs / 1000
        val out = ShortArray(samples)
        val freq = 3100.0
        val period = SAMPLE_RATE / freq
        val beatMs = 500
        val beatSamples = SAMPLE_RATE * beatMs / 1000
        // beep, gap, beep, gap, beep, then long gap (remaining samples)
        fun isBeep(i: Int): Boolean = when (i / beatSamples) {
            0, 2, 4 -> true
            else -> false
        }
        for (i in 0 until samples) {
            if (!isBeep(i)) { out[i] = 0; continue }
            val phase = (i % period.toInt()).toDouble() / period
            out[i] = if (phase < 0.5) Short.MAX_VALUE else Short.MIN_VALUE
        }
        return out
    }

    /**
     * 1500ms — an FM dental-drill: 1600Hz carrier deeply modulated by a 42Hz tone
     * with a slow wail on top. The high modulation index makes it grind and screech.
     */
    private fun dentist(): ShortArray {
        val durMs = 1500
        val samples = SAMPLE_RATE * durMs / 1000
        val out = ShortArray(samples)
        val carrier = 1600.0
        val modFreq = 42.0
        val modIndex = 9.0
        val amp = Short.MAX_VALUE * 0.6
        for (i in 0 until samples) {
            val t = i.toDouble() / SAMPLE_RATE
            // slow wail drifts the carrier up and down across the loop
            val wail = carrier + 220.0 * sin(2 * PI * 0.7 * t)
            val mod = modIndex * sin(2 * PI * modFreq * t)
            val sample = sin(2 * PI * wail * t + mod)
            out[i] = (sample * amp)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return applyEdgeFade(out, 30)
    }

    /**
     * 2500ms — four closely-detuned voices that beat against each other while the
     * whole cluster sweeps upward, producing a dissonant rising banshee wail.
     */
    private fun banshee(): ShortArray {
        val durMs = 2500
        val samples = SAMPLE_RATE * durMs / 1000
        val out = ShortArray(samples)
        val base = doubleArrayOf(620.0, 631.0, 639.0, 652.0) // tight, beating cluster
        val amp = Short.MAX_VALUE * 0.45
        for (i in 0 until samples) {
            val t = i.toDouble() / SAMPLE_RATE
            val rise = 1.0 + 0.9 * (t / (durMs / 1000.0)) // up to ~+90% pitch by the end
            var sum = 0.0
            for (f in base) sum += sin(2 * PI * f * rise * t)
            sum /= base.size
            out[i] = (sum * amp)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return applyEdgeFade(out, 40)
    }

    /**
     * 2000ms — three descending bell-like notes (E5, C5, G4) with quick attack and
     * slow exponential decay. Adds a 2nd + 3rd harmonic for a bell-character timbre.
     */
    private fun chime(): ShortArray {
        val durMs = 2000
        val samples = SAMPLE_RATE * durMs / 1000
        val out = ShortArray(samples)
        val notes = doubleArrayOf(659.25, 523.25, 392.00) // E5, C5, G4
        val noteSamples = samples / notes.size
        val amp = Short.MAX_VALUE * 0.55
        for (n in notes.indices) {
            val freq = notes[n]
            val offset = n * noteSamples
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val envelope = exp(-t * 3.0)
                val sample = sin(2 * PI * freq * t) * 0.6 +
                    sin(2 * PI * freq * 2.0 * t) * 0.3 +
                    sin(2 * PI * freq * 3.0 * t) * 0.15
                out[offset + i] = (sample * envelope * amp)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
        }
        return applyEdgeFade(out, 100)
    }

    /**
     * 1200ms — three woody plucks at A4. Boosting the 4th harmonic gives the wooden
     * marimba character. Sharp attack, fast decay so each pluck feels percussive.
     */
    private fun marimba(): ShortArray {
        val durMs = 1200
        val samples = SAMPLE_RATE * durMs / 1000
        val out = ShortArray(samples)
        val freq = 440.0
        val pluckCount = 3
        val pluckSamples = samples / pluckCount
        val amp = Short.MAX_VALUE * 0.6
        for (p in 0 until pluckCount) {
            val offset = p * pluckSamples
            for (i in 0 until pluckSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val envelope = exp(-t * 10.0)
                val sample = sin(2 * PI * freq * t) * 0.7 +
                    sin(2 * PI * freq * 4.0 * t) * 0.25
                out[offset + i] = (sample * envelope * amp)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
        }
        return applyEdgeFade(out, 80)
    }

    /**
     * 3000ms — slowly swelling A major triad (A4 + C#5 + E5). Triangular envelope
     * so the sound feels like a gentle wave — no sharp transients, no clipping.
     */
    private fun morning(): ShortArray {
        val durMs = 3000
        val samples = SAMPLE_RATE * durMs / 1000
        val out = ShortArray(samples)
        val freqs = doubleArrayOf(440.0, 554.37, 659.25) // A4, C#5, E5
        val mid = samples / 2.0
        val amp = Short.MAX_VALUE * 0.5
        for (i in 0 until samples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = if (i < mid) i / mid else (samples - i) / mid
            var sum = 0.0
            for (f in freqs) sum += sin(2 * PI * f * t)
            sum /= freqs.size
            out[i] = (sum * envelope * amp)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return applyEdgeFade(out, 200)
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
