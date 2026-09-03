package com.liveproduction.core.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as AndroidCameraManager
import android.os.Build
import android.util.Log
import android.view.Surface
import com.liveproduction.core.camera.model.CameraCapability
import com.liveproduction.core.camera.model.CameraControls
import com.liveproduction.core.camera.model.CapabilityStatus
import com.liveproduction.core.camera.session.CameraCaptureSessionManager
import com.liveproduction.core.media.model.VideoSourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CameraManager private constructor() {

    private val _cameraCapabilities = MutableStateFlow<Map<VideoSourceType, CameraCapability>>(emptyMap())
    val cameraCapabilities: StateFlow<Map<VideoSourceType, CameraCapability>> = _cameraCapabilities.asStateFlow()

    private val _currentControls = MutableStateFlow(CameraControls())
    val currentControls: StateFlow<CameraControls> = _currentControls.asStateFlow()

    private var sessionManager: CameraCaptureSessionManager? = null
    private var activeTargetSurface: Surface? = null
    private var activeEncoderSurface: Surface? = null
    private var currentActiveSource: VideoSourceType = VideoSourceType.SOURCE_REAR_MAIN

    fun detectCapabilities(context: Context) {
        val systemCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? AndroidCameraManager
        if (systemCameraManager == null) {
            Log.e(TAG, "CameraService unavailable on device")
            return
        }

        if (sessionManager == null) {
            sessionManager = CameraCaptureSessionManager(context.applicationContext)
        }

        val caps = mutableMapOf<VideoSourceType, CameraCapability>()

        try {
            val cameraIds = systemCameraManager.cameraIdList
            Log.i(TAG, "Discovered ${cameraIds.size} total camera IDs: ${cameraIds.joinToString()}")

            val frontCameras = mutableListOf<Pair<String, CameraCharacteristics>>()
            val rearCameras = mutableListOf<Pair<String, CameraCharacteristics>>()

            for (id in cameraIds) {
                val characteristics = systemCameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                when (facing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> frontCameras.add(id to characteristics)
                    CameraCharacteristics.LENS_FACING_BACK -> rearCameras.add(id to characteristics)
                }
            }

            // Map Front Camera
            frontCameras.firstOrNull()?.let { (id, characteristics) ->
                val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val primaryFocal = focalLengths?.firstOrNull() ?: 3.0f
                val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 270
                caps[VideoSourceType.SOURCE_FRONT_CAMERA] = CameraCapability(
                    sourceType = VideoSourceType.SOURCE_FRONT_CAMERA,
                    cameraId = id,
                    lensFacing = CameraCharacteristics.LENS_FACING_FRONT,
                    focalLength = primaryFocal,
                    status = CapabilityStatus.SUPPORTED,
                    sensorOrientation = sensorOrientation
                )
            }

            // Map Rear Cameras
            if (rearCameras.isNotEmpty()) {
                val primaryRear = rearCameras.first()
                val primaryOrientation = primaryRear.second.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90

                // Primary Main Rear Lens (1.0x)
                caps[VideoSourceType.SOURCE_REAR_MAIN] = CameraCapability(
                    sourceType = VideoSourceType.SOURCE_REAR_MAIN,
                    cameraId = primaryRear.first,
                    lensFacing = CameraCharacteristics.LENS_FACING_BACK,
                    focalLength = primaryRear.second.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: 4.5f,
                    status = CapabilityStatus.SUPPORTED,
                    sensorOrientation = primaryOrientation
                )

                // Ultra Wide Lens (0.5x)
                val ultraWideCandidate = rearCameras.find { (_, char) ->
                    val focal = char.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: 4.5f
                    focal < 3.5f
                } ?: rearCameras.getOrNull(1) ?: primaryRear

                val uwOrientation = ultraWideCandidate.second.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
                caps[VideoSourceType.SOURCE_REAR_ULTRAWIDE] = CameraCapability(
                    sourceType = VideoSourceType.SOURCE_REAR_ULTRAWIDE,
                    cameraId = ultraWideCandidate.first,
                    lensFacing = CameraCharacteristics.LENS_FACING_BACK,
                    focalLength = ultraWideCandidate.second.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: 2.0f,
                    status = CapabilityStatus.SUPPORTED,
                    sensorOrientation = uwOrientation
                )

                // Telephoto Lens (3.0x)
                val telephotoCandidate = rearCameras.find { (_, char) ->
                    val focal = char.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: 4.5f
                    focal > 5.5f
                } ?: rearCameras.getOrNull(2) ?: primaryRear

                val teleOrientation = telephotoCandidate.second.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
                caps[VideoSourceType.SOURCE_REAR_TELEPHOTO] = CameraCapability(
                    sourceType = VideoSourceType.SOURCE_REAR_TELEPHOTO,
                    cameraId = telephotoCandidate.first,
                    lensFacing = CameraCharacteristics.LENS_FACING_BACK,
                    focalLength = telephotoCandidate.second.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: 7.5f,
                    status = CapabilityStatus.SUPPORTED,
                    sensorOrientation = teleOrientation
                )
            }

            _cameraCapabilities.value = caps
            Log.i(TAG, "Camera capabilities mapped successfully: ${caps.keys.map { it.name }}")
        } catch (e: Exception) {
            Log.e(TAG, "Error enumerating Camera2 characteristics", e)
        }
    }

    fun setEncoderSurface(encoderSurface: Surface?) {
        this.activeEncoderSurface = encoderSurface
        activeTargetSurface?.let { targetSurface ->
            switchCameraSource(currentActiveSource, targetSurface)
        }
    }

    fun switchCameraSource(sourceType: VideoSourceType, targetSurface: Surface) {
        activeTargetSurface = targetSurface
        currentActiveSource = sourceType
        val cap = _cameraCapabilities.value[sourceType]
            ?: _cameraCapabilities.value[VideoSourceType.SOURCE_REAR_MAIN]
            ?: _cameraCapabilities.value[VideoSourceType.SOURCE_FRONT_CAMERA]

        if (cap == null) {
            Log.w(TAG, "Cannot switch to camera $sourceType: No valid camera found on device")
            return
        }

        val targetZoom = when (sourceType) {
            VideoSourceType.SOURCE_REAR_ULTRAWIDE -> 0.5f
            VideoSourceType.SOURCE_REAR_TELEPHOTO -> 3.0f
            else -> 1.0f
        }

        Log.i(TAG, "Opening Camera2 ID: ${cap.cameraId} for source: ${sourceType.name} with target zoom: ${targetZoom}x, orientation: ${cap.sensorOrientation}° (Encoder: ${activeEncoderSurface != null})")
        sessionManager?.openCamera(cap.cameraId, targetSurface, activeEncoderSurface, targetZoom, cap.sensorOrientation)
    }

    fun updateZoom(zoomRatio: Float) {
        val updated = _currentControls.value.copy(zoomRatio = zoomRatio)
        _currentControls.value = updated
        sessionManager?.updateControls(updated)
    }

    fun toggleTorch(enabled: Boolean) {
        val updated = _currentControls.value.copy(isTorchEnabled = enabled)
        _currentControls.value = updated
        sessionManager?.updateControls(updated)
    }

    fun release() {
        sessionManager?.closeCamera()
        sessionManager = null
        activeTargetSurface = null
        activeEncoderSurface = null
    }

    companion object {
        private const val TAG = "CameraManager"

        @Volatile
        private var INSTANCE: CameraManager? = null

        fun getInstance(): CameraManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CameraManager().also { INSTANCE = it }
            }
        }
    }
}
