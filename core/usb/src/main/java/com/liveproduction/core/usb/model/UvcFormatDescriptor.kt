package com.liveproduction.core.usb.model

enum class UvcFrameFormat {
    MJPEG,
    YUY2,
    NV21,
    UNKNOWN
}

data class UvcFormatDescriptor(
    val format: UvcFrameFormat,
    val width: Int,
    val height: Int,
    val fps: Int
)
