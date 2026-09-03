package com.liveproduction.core.diagnostics.model

data class CodecCapability(
    val mimeType: String,
    val isHardwareAccelerated: Boolean,
    val maxSupportedWidth: Int,
    val maxSupportedHeight: Int,
    val maxSupportedFrameRate: Int,
    val maxBitrateBps: Int,
    val supportedProfiles: List<String>
)
