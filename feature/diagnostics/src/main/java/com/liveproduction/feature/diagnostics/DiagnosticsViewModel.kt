package com.liveproduction.feature.diagnostics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.liveproduction.core.camera.CameraManager
import com.liveproduction.core.camera.model.CameraCapability
import com.liveproduction.core.diagnostics.DiagnosticsManager
import com.liveproduction.core.diagnostics.model.DeviceCapabilityReport
import com.liveproduction.core.media.model.VideoSourceType
import com.liveproduction.core.usb.UsbCaptureManager
import com.liveproduction.core.usb.model.UsbDeviceStatus
import kotlinx.coroutines.flow.StateFlow

class DiagnosticsViewModel(application: Application) : AndroidViewModel(application) {

    private val diagnosticsManager = DiagnosticsManager.getInstance()
    private val cameraManager = CameraManager.getInstance()
    private val usbCaptureManager = UsbCaptureManager.getInstance()

    val capabilityReport: StateFlow<DeviceCapabilityReport?> = diagnosticsManager.capabilityReport
    val cameraCapabilities: StateFlow<Map<VideoSourceType, CameraCapability>> = cameraManager.cameraCapabilities
    val usbStatus: StateFlow<UsbDeviceStatus> = usbCaptureManager.usbStatus

    init {
        refreshDiagnostics()
    }

    fun refreshDiagnostics() {
        cameraManager.detectCapabilities(getApplication())
        usbCaptureManager.inspectAttachedDevices(getApplication())
        diagnosticsManager.updateMetrics(getApplication())
        diagnosticsManager.generateFullReport(getApplication())
    }
}
