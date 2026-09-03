package com.liveproduction.core.diagnostics.verifier

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.liveproduction.core.camera.model.CapabilityStatus
import com.liveproduction.core.diagnostics.DiagnosticsManager

data class CompatibilityCheckResult(
    val featureName: String,
    val status: CapabilityStatus,
    val details: String
)

class CompatibilityMatrixVerifier {

    fun verifyDeviceCompatibility(context: Context): List<CompatibilityCheckResult> {
        val results = mutableListOf<CompatibilityCheckResult>()

        // 1. Check USB Host Support
        val hasUsbHost = context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        results.add(
            CompatibilityCheckResult(
                featureName = "USB Host UVC Dongle Support",
                status = if (hasUsbHost) CapabilityStatus.SUPPORTED else CapabilityStatus.NOT_SUPPORTED,
                details = if (hasUsbHost) "Device kernel exposes USB Host Mode" else "Device lacks FEATURE_USB_HOST"
            )
        )

        // 2. Check Hardware H.264 Encoder
        val report = DiagnosticsManager.getInstance().generateFullReport(context)
        val h264 = report.h264Codec
        val h264Status = when {
            h264 == null -> CapabilityStatus.NOT_SUPPORTED
            h264.isHardwareAccelerated -> CapabilityStatus.SUPPORTED
            else -> CapabilityStatus.PARTIALLY_SUPPORTED
        }

        results.add(
            CompatibilityCheckResult(
                featureName = "H.264 Hardware Encoder (MediaCodec)",
                status = h264Status,
                details = h264?.let { "${it.maxSupportedWidth}x${it.maxSupportedHeight} @ ${it.maxSupportedFrameRate}fps" } ?: "No AVC codec found"
            )
        )

        // 3. Check AAC Audio Encoder
        val aac = report.aacCodec
        results.add(
            CompatibilityCheckResult(
                featureName = "AAC Audio Encoder",
                status = if (aac != null) CapabilityStatus.SUPPORTED else CapabilityStatus.NOT_SUPPORTED,
                details = aac?.let { "48kHz AAC-LC supported" } ?: "No AAC encoder found"
            )
        )

        Log.i(TAG, "Completed Device Compatibility Matrix Verification: ${results.size} checks run.")
        return results
    }

    companion object {
        private const val TAG = "CompatibilityMatrixVerifier"
    }
}
