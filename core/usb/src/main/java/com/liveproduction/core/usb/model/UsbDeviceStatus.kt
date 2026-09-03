package com.liveproduction.core.usb.model

enum class UsbDeviceStatus {
    NO_DEVICE_ATTACHED,
    DEVICE_ATTACHED_PERMISSION_REQUIRED,
    CONNECTING,
    STREAMING_ACTIVE,
    DISCONNECTED,
    ERROR
}
