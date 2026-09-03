package com.liveproduction.core.camera.model

data class CameraControls(
    val zoomRatio: Float = 1.0f,
    val exposureCompensation: Int = 0,
    val isTorchEnabled: Boolean = false,
    val isAutoFocusEnabled: Boolean = true
)
