package com.liveproduction.core.diagnostics.model

data class DeviceCapabilityReport(
    val deviceModel: String,
    val androidVersion: String,
    val socName: String,
    val hasUsbHostFeature: Boolean,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val h264Codec: CodecCapability?,
    val aacCodec: CodecCapability?,
    val thermalStatus: String,
    val batteryPercent: Int,
    val isCharging: Boolean
)
