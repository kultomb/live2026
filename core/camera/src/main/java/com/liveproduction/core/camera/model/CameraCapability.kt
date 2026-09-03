package com.liveproduction.core.camera.model

import com.liveproduction.core.media.model.VideoSourceType

data class CameraCapability(
    val sourceType: VideoSourceType,
    val cameraId: String,
    val lensFacing: Int,
    val focalLength: Float,
    val status: CapabilityStatus,
    val sensorOrientation: Int = 90,
    val isLogicalMultiCamera: Boolean = false,
    val physicalCameraIds: List<String> = emptyList()
)
