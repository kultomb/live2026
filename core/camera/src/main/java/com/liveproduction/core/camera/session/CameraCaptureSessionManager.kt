package com.liveproduction.core.camera.session

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.liveproduction.core.camera.model.CameraControls

class CameraCaptureSessionManager(private val context: Context) {

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var activeCameraId: String? = null
    private var activeDisplaySurface: Surface? = null
    private var activeEncoderSurface: Surface? = null
    private var currentControls = CameraControls()

    fun startBackgroundThread() {
        if (backgroundThread == null) {
            val thread = HandlerThread("Camera2BackgroundThread").apply { start() }
            backgroundThread = thread
            backgroundHandler = Handler(thread.looper)
            Log.i(TAG, "Camera2 Background HandlerThread started")
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera(
        cameraId: String,
        targetSurface: Surface,
        encoderSurface: Surface? = null,
        zoomRatio: Float = 1.0f
    ) {
        startBackgroundThread()
        currentControls = currentControls.copy(zoomRatio = zoomRatio)

        activeCameraId = cameraId
        activeDisplaySurface = targetSurface
        activeEncoderSurface = encoderSurface

        Log.i(TAG, "Opening Camera2 ID: $cameraId (Display + ${if (encoderSurface != null) "Encoder" else "No Encoder"}) with zoom ${zoomRatio}x...")
        closeCameraInternal()

        val systemCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        try {
            systemCameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.i(TAG, "CameraDevice $cameraId opened successfully")
                    cameraDevice = camera
                    createCaptureSession(camera, targetSurface, encoderSurface, zoomRatio)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "CameraDevice $cameraId disconnected")
                    camera.close()
                    if (cameraDevice == camera) cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "CameraDevice $cameraId error code: $error")
                    camera.close()
                    if (cameraDevice == camera) cameraDevice = null
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Camera2 ID: $cameraId", e)
        }
    }

    private fun createCaptureSession(
        camera: CameraDevice,
        displaySurface: Surface,
        encoderSurface: Surface?,
        zoomRatio: Float
    ) {
        try {
            val targets = mutableListOf<Surface>(displaySurface)
            if (encoderSurface != null && encoderSurface.isValid) {
                targets.add(encoderSurface)
                Log.i(TAG, "Adding Hardware MediaCodec Encoder Surface to Camera2 CaptureSession targets")
            }

            val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(displaySurface)
                if (encoderSurface != null && encoderSurface.isValid) {
                    addTarget(encoderSurface)
                }
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    try {
                        set(CaptureRequest.CONTROL_ZOOM_RATIO, zoomRatio)
                    } catch (e: Exception) {
                        Log.w(TAG, "CONTROL_ZOOM_RATIO not supported on this device camera lens")
                    }
                }
            }

            camera.createCaptureSession(
                targets,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.i(TAG, "CameraCaptureSession configured successfully with ${targets.size} targets for camera ${camera.id}")
                        captureSession = session
                        session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "CameraCaptureSession configuration failed for camera ${camera.id}")
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating CameraCaptureSession", e)
        }
    }

    fun updateControls(controls: CameraControls) {
        currentControls = controls
        val session = captureSession ?: return
        val camera = cameraDevice ?: return
        val displaySurface = activeDisplaySurface ?: return
        val encoderSurface = activeEncoderSurface

        try {
            val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(displaySurface)
                if (encoderSurface != null && encoderSurface.isValid) {
                    addTarget(encoderSurface)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    try {
                        set(CaptureRequest.CONTROL_ZOOM_RATIO, controls.zoomRatio)
                    } catch (e: Exception) {}
                }
                set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, controls.exposureCompensation)
                if (controls.isTorchEnabled) {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                } else {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                }
            }
            session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
            Log.i(TAG, "Updated Camera2 manual controls: $controls")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Camera2 controls", e)
        }
    }

    fun closeCamera() {
        closeCameraInternal()
        stopBackgroundThread()
    }

    private fun closeCameraInternal() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            activeCameraId = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera internal handles", e)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    companion object {
        private const val TAG = "CameraCaptureSessionManager"
    }
}
