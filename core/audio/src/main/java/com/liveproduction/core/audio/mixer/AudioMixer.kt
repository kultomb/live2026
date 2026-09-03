package com.liveproduction.core.audio.mixer

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max

class AudioMixer {

    data class AudioMixResult(
        val mixedPcm: ShortArray,
        val leftDb: Float,
        val rightDb: Float,
        val isClipping: Boolean
    )

    fun mixAndScale(
        inputPcm: ShortArray,
        gainFactor: Float,
        isMuted: Boolean
    ): AudioMixResult {
        if (isMuted) {
            val silentPcm = ShortArray(inputPcm.size)
            return AudioMixResult(silentPcm, -60.0f, -60.0f, false)
        }

        val outputPcm = ShortArray(inputPcm.size)
        var maxPeakL = 0
        var maxPeakR = 0
        var clippingDetected = false

        for (i in inputPcm.indices step 2) {
            val rawL = (inputPcm[i] * gainFactor).toInt()
            val rawR = (inputPcm[if (i + 1 < inputPcm.size) i + 1 else i] * gainFactor).toInt()

            val scaledL = softLimit(rawL)
            val scaledR = softLimit(rawR)

            if (abs(rawL) > 32767 || abs(rawR) > 32767) {
                clippingDetected = true
            }

            outputPcm[i] = scaledL.toShort()
            if (i + 1 < outputPcm.size) {
                outputPcm[i + 1] = scaledR.toShort()
            }

            maxPeakL = max(maxPeakL, abs(scaledL.toInt()))
            maxPeakR = max(maxPeakR, abs(scaledR.toInt()))
        }

        val leftDb = calculateDb(maxPeakL)
        val rightDb = calculateDb(maxPeakR)

        return AudioMixResult(outputPcm, leftDb, rightDb, clippingDetected)
    }

    private fun softLimit(sample: Int): Int {
        val maxVal = 32767
        val minVal = -32768
        return when {
            sample > maxVal -> maxVal - (maxVal - sample) / 4
            sample < minVal -> minVal + (minVal - sample) / 4
            else -> sample
        }
    }

    private fun calculateDb(peakSample: Int): Float {
        if (peakSample <= 0) return -60.0f
        val ratio = peakSample.toDouble() / 32768.0
        val db = 20.0 * log10(ratio)
        return db.toFloat().coerceIn(-60.0f, 0.0f)
    }
}
