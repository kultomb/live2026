package com.liveproduction.core.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

class MicAudioRecorder private constructor() {

    interface AudioFrameListener {
        fun onPcmFrameAvailable(pcmData: ShortArray, length: Int)
    }

    private var audioRecord: AudioRecord? = null
    @Volatile private var isRecording = false
    private var recordThread: Thread? = null
    private var frameListener: AudioFrameListener? = null

    fun setAudioFrameListener(listener: AudioFrameListener?) {
        this.frameListener = listener
    }

    @SuppressLint("MissingPermission")
    fun startRecording(context: Context) {
        if (isRecording) return

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val sourcesToTry = intArrayOf(
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        )

        var record: AudioRecord? = null
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, 8192)

        for (src in sourcesToTry) {
            try {
                val r = AudioRecord(src, sampleRate, channelConfig, audioFormat, bufferSize)
                if (r.state == AudioRecord.STATE_INITIALIZED) {
                    record = r
                    Log.i(TAG, "AudioRecord initialized successfully with source: $src at $sampleRate Hz")
                    break
                } else {
                    r.release()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to initialize AudioRecord with source $src", e)
            }
        }

        if (record == null) {
            Log.e(TAG, "Could not initialize AudioRecord with any audio source!")
            return
        }

        try {
            audioRecord = record
            record.startRecording()
            isRecording = true

            recordThread = Thread {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                val buffer = ShortArray(2048)

                while (isRecording) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        // 1. Process Audio Buffer for Metering & Resampling
                        val processed = AudioManager.getInstance().processAudioBuffer(buffer, sampleRate, 2)

                        // 2. Calculate Logarithmic dBFS Level for High Sensitivity VU Meter
                        val level = calculatePcmLevel(buffer, read)
                        AudioManager.getInstance().updateAudioLevel(level)

                        // 3. Forward processed PCM buffer with exact length to encoder listener
                        frameListener?.onPcmFrameAvailable(processed, processed.size)
                    }
                }
            }.apply {
                name = "LiveMicAudioRecordThread"
                start()
            }

            Log.i(TAG, "MicAudioRecorder started recording loop successfully")
        } catch (e: Throwable) {
            Log.e(TAG, "Error starting AudioRecord loop", e)
        }
    }

    /**
     * Professional Logarithmic dBFS Level Calculation with Dynamic Speech Gain Boost.
     * Maps PCM Short values to 0% - 100% responsiveness corresponding to -50dBFS to 0dBFS.
     */
    private fun calculatePcmLevel(pcmData: ShortArray, size: Int): Int {
        var sum = 0.0
        for (i in 0 until size) {
            val sample = pcmData[i].toDouble()
            sum += sample * sample
        }
        if (size == 0) return 0

        val rms = sqrt(sum / size)
        if (rms <= 1.0) return 0

        // Logarithmic dBFS Calculation (0 dB max, -50 dB floor)
        val db = 20.0 * log10(rms / 32767.0)

        // Map -50 dB ... 0 dB to 0.0 ... 1.0
        val normalized = ((db + 50.0) / 50.0).coerceIn(0.0, 1.0)

        // Apply dynamic gain boost curve (pow 0.65) for responsive speech metering
        val boosted = normalized.pow(0.65)

        return (boosted * 100.0).toInt().coerceIn(0, 100)
    }

    fun stopRecording() {
        isRecording = false
        try {
            recordThread?.join(500)
            recordThread = null
        } catch (e: Throwable) {}

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Throwable) {}

        Log.i(TAG, "MicAudioRecorder stopped")
    }

    companion object {
        private const val TAG = "MicAudioRecorder"

        @Volatile
        private var INSTANCE: MicAudioRecorder? = null

        fun getInstance(): MicAudioRecorder {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MicAudioRecorder().also { INSTANCE = it }
            }
        }
    }
}
