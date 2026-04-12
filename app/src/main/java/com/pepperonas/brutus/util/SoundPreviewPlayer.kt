package com.pepperonas.brutus.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager

class SoundPreviewPlayer(private val context: Context) {

    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null

    fun play(sound: AlarmSound) {
        stop()
        if (sound == AlarmSound.SILENT) return
        if (sound == AlarmSound.SYSTEM) {
            playSystem()
            return
        }
        val pcm = AlarmSoundGenerator.generatePcm(sound)
        if (pcm.isEmpty()) return

        val bufferBytes = pcm.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(AlarmSoundGenerator.SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        track.setLoopPoints(0, pcm.size, -1)
        track.play()
        audioTrack = track
    }

    private fun playSystem() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: return
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(context, uri)
            isLooping = true
            prepare()
            start()
        }
    }

    fun stop() {
        audioTrack?.let {
            try { it.stop() } catch (_: IllegalStateException) { }
            it.release()
        }
        audioTrack = null
        mediaPlayer?.let {
            try { if (it.isPlaying) it.stop() } catch (_: IllegalStateException) { }
            it.release()
        }
        mediaPlayer = null
    }
}
