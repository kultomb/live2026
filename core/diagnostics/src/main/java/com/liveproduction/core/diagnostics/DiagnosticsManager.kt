package com.liveproduction.core.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.liveproduction.core.diagnostics.model.CodecCapability
import com.liveproduction.core.diagnostics.model.DeviceCapabilityReport
import com.liveproduction.core.diagnostics.model.SystemHealthMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DiagnosticsManager private constructor() {

    private val _healthMetrics = MutableStateFlow(SystemHealthMetrics())
    val healthMetrics: StateFlow<SystemHealthMetrics> = _healthMetrics.asStateFlow()

    private val _capabilityReport = MutableStateFlow<DeviceCapabilityReport?>(null)
    val capabilityReport: StateFlow<DeviceCapabilityReport?> = _capabilityReport.asStateFlow()

    fun updateMetrics(context: Context) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        val isCharging = batteryManager?.isCharging ?: false

        val thermalStatusStr = getThermalStatusString(context)

        _healthMetrics.value = _healthMetrics.value.copy(
            batteryPercent = batteryLevel,
            isCharging = isCharging,
            thermalStatus = thermalStatusStr
        )
    }

    fun generateFullReport(context: Context): DeviceCapabilityReport {
        val hasUsbHost = context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val h264Capability = queryCodecCapability(MediaFormat.MIMETYPE_VIDEO_AVC, isEncoder = true)
        val aacCapability = queryCodecCapability(MediaFormat.MIMETYPE_AUDIO_AAC, isEncoder = true)

        val report = DeviceCapabilityReport(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            socName = Build.HARDWARE,
            hasUsbHostFeature = hasUsbHost,
            totalRamMb = memoryInfo.totalMem / (1024 * 1024),
            availableRamMb = memoryInfo.availMem / (1024 * 1024),
            h264Codec = h264Capability,
            aacCodec = aacCapability,
            thermalStatus = getThermalStatusString(context),
            batteryPercent = _healthMetrics.value.batteryPercent,
            isCharging = _healthMetrics.value.isCharging
        )

        _capabilityReport.value = report
        Log.i(TAG, "Generated hardware capability report: $report")
        return report
    }

    private fun queryCodecCapability(mimeType: String, isEncoder: Boolean): CodecCapability? {
        try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (info in codecList.codecInfos) {
                if (info.isEncoder != isEncoder) continue
                for (type in info.supportedTypes) {
                    if (type.equals(mimeType, ignoreCase = true)) {
                        val caps = info.getCapabilitiesForType(type)
                        val videoCaps = caps.videoCapabilities
                        val encoderCaps = caps.encoderCapabilities

                        val isHardware = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            info.isHardwareAccelerated
                        } else {
                            !info.name.startsWith("OMX.google.") && !info.name.startsWith("c2.android.")
                        }

                        return CodecCapability(
                            mimeType = mimeType,
                            isHardwareAccelerated = isHardware,
                            maxSupportedWidth = videoCaps?.supportedWidths?.upper ?: 1920,
                            maxSupportedHeight = videoCaps?.supportedHeights?.upper ?: 1080,
                            maxSupportedFrameRate = videoCaps?.supportedFrameRates?.upper?.toInt() ?: 60,
                            maxBitrateBps = videoCaps?.bitrateRange?.upper ?: 12_000_000,
                            supportedProfiles = caps.profileLevels.map { "Profile ${it.profile}" }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying codec capability for $mimeType", e)
        }
        return null
    }

    private fun getThermalStatusString(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            when (powerManager?.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "NORMAL"
                PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT_PRESSURE"
                PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE_PRESSURE"
                PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE_THROTTLE"
                PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL_THROTTLE"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY_SHUTDOWN"
                else -> "NORMAL"
            }
        } else {
            "NORMAL"
        }
    }

    companion object {
        private const val TAG = "DiagnosticsManager"

        @Volatile
        private var INSTANCE: DiagnosticsManager? = null

        fun getInstance(): DiagnosticsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DiagnosticsManager().also { INSTANCE = it }
            }
        }
    }
}
