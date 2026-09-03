package com.liveproduction.core.audio.resampler

import kotlin.math.roundToInt

class AudioResampler(
    private val targetSampleRate: Int = 44100,
    private val targetChannels: Int = 2
) {

    fun resamplePcm16(
        inputPcm: ShortArray,
        inputSampleRate: Int,
        inputChannels: Int
    ): ShortArray {
        if (inputSampleRate == targetSampleRate && inputChannels == targetChannels) {
            return inputPcm
        }

        val ratio = targetSampleRate.toDouble() / inputSampleRate.toDouble()
        val inputFrameCount = inputPcm.size / inputChannels
        val outputFrameCount = (inputFrameCount * ratio).roundToInt()
        val outputPcm = ShortArray(outputFrameCount * targetChannels)

        for (i in 0 until outputFrameCount) {
            val inputIndex = (i / ratio).toInt().coerceIn(0, inputFrameCount - 1)
            for (c in 0 until targetChannels) {
                val sample = if (inputChannels == 1) {
                    inputPcm[inputIndex]
                } else {
                    inputPcm[inputIndex * inputChannels + (c % inputChannels)]
                }
                outputPcm[i * targetChannels + c] = sample
            }
        }

        return outputPcm
    }
}
