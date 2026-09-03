package com.liveproduction.core.audio.model

data class AudioMeterState(
    val leftChannelDb: Float = -60.0f,
    val rightChannelDb: Float = -60.0f,
    val levelPercent: Int = 0,
    val isMuted: Boolean = false,
    val isClipping: Boolean = false,
    val masterGainFactor: Float = 1.0f
)
