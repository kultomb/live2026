package com.liveproduction.core.audio.sync

import android.util.Log
import kotlin.math.abs

class TimestampManager {

    private var sessionStartNs: Long = 0L
    private var totalAudioSamplesProcessed: Long = 0L

    fun startSession() {
        sessionStartNs = System.nanoTime()
        totalAudioSamplesProcessed = 0L
        Log.i(TAG, "TimestampManager session started at nano: $sessionStartNs")
    }

    fun calculateVideoPtsUs(frameNanoTime: Long): Long {
        if (sessionStartNs == 0L) startSession()
        return (frameNanoTime - sessionStartNs) / 1000L
    }

    fun calculateAudioPtsUs(sampleCount: Long, sampleRate: Int = 48000): Long {
        totalAudioSamplesProcessed += sampleCount
        return (totalAudioSamplesProcessed * 1_000_000L) / sampleRate
    }

    fun checkDrift(videoPtsUs: Long, audioPtsUs: Long): Long {
        val driftUs = videoPtsUs - audioPtsUs
        if (abs(driftUs) > 40_000) { // > 40ms drift threshold
            Log.w(TAG, "A/V Synchronization Drift detected: ${driftUs / 1000} ms (Video: $videoPtsUs us, Audio: $audioPtsUs us)")
        }
        return driftUs
    }

    companion object {
        private const val TAG = "TimestampManager"
    }
}
