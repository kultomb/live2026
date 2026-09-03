package com.liveproduction.core.diagnostics.model

data class SystemHealthMetrics(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val thermalStatus: String = "NORMAL",
    val availableMemoryMb: Long = 0L,
    val totalMemoryMb: Long = 0L,
    val activeFps: Float = 0f,
    val droppedFrameCount: Long = 0L,
    val activeBitrateKbps: Long = 0L
)
