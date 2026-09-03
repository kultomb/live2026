package com.liveproduction.feature.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.liveproduction.core.audio.AudioManager
import com.liveproduction.core.audio.MicAudioRecorder
import com.liveproduction.core.camera.CameraManager
import com.liveproduction.core.diagnostics.DiagnosticsManager
import com.liveproduction.core.media.VideoPipelineManager
import com.liveproduction.core.media.encoder.EncoderManager
import com.liveproduction.core.media.model.VideoSourceType
import com.liveproduction.core.media.recording.RecordingManager
import com.liveproduction.core.media.recording.RecordingState
import com.liveproduction.core.network.NetworkManager
import com.liveproduction.core.streaming.LiveSessionManager
import com.liveproduction.core.streaming.model.LiveSessionState
import com.liveproduction.core.usb.UsbCaptureManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class LiveStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val liveSessionManager = LiveSessionManager.getInstance()
    private val videoPipelineManager = VideoPipelineManager.getInstance()
    private val cameraManager = CameraManager.getInstance()
    private val usbCaptureManager = UsbCaptureManager.getInstance()
    private val audioManager = AudioManager.getInstance()
    private val recordingManager = RecordingManager.getInstance()
    private val encoderManager = EncoderManager.getInstance()
    private val networkManager = NetworkManager.getInstance()
    private val diagnosticsManager = DiagnosticsManager.getInstance()

    val sessionState: StateFlow<LiveSessionState> = liveSessionManager.sessionState
    val activeSource: StateFlow<VideoSourceType> = videoPipelineManager.activeSource
    val currentFps: StateFlow<Float> = videoPipelineManager.currentFps
    val cameraCapabilities = cameraManager.cameraCapabilities
    val usbStatus = usbCaptureManager.usbStatus
    val audioMeterState = audioManager.audioMeterState
    val recordingState: StateFlow<RecordingState> = recordingManager.recordingState
    val recordedDurationMs: StateFlow<Long> = recordingManager.recordedDurationMs
    val networkStatus = networkManager.networkStatus
    val healthMetrics = diagnosticsManager.healthMetrics

    init {
        // Bind VideoPipelineManager surface events to CameraManager
        videoPipelineManager.setOnSourceSurfaceListener { sourceType, surface ->
            when (sourceType) {
                VideoSourceType.SOURCE_FRONT_CAMERA,
                VideoSourceType.SOURCE_REAR_MAIN,
                VideoSourceType.SOURCE_REAR_ULTRAWIDE,
                VideoSourceType.SOURCE_REAR_TELEPHOTO,
                VideoSourceType.SOURCE_REAR_MACRO -> {
                    cameraManager.switchCameraSource(sourceType, surface)
                }
                else -> {
                    // HDMI UVC or other sources
                }
            }
        }

        // Bind EncoderManager surface events to CameraManager for MediaCodec recording
        encoderManager.setOnEncoderSurfaceListener { surface ->
            cameraManager.setEncoderSurface(surface)
        }

        // Bind MicAudioRecorder audio frames directly to EncoderManager AAC
        MicAudioRecorder.getInstance().setAudioFrameListener(object : MicAudioRecorder.AudioFrameListener {
            override fun onPcmFrameAvailable(pcmData: ShortArray, length: Int) {
                encoderManager.encodeAudioFrame(pcmData, length)
            }
        })

        // Run initial hardware capability discovery & mic recording on background Dispatchers.IO to prevent Main UI Thread ANR
        viewModelScope.launch(Dispatchers.IO) {
            cameraManager.detectCapabilities(application)
            usbCaptureManager.inspectAttachedDevices(application)
            networkManager.updateNetworkState(application)
            diagnosticsManager.updateMetrics(application)
            audioManager.startMicAudioRecording(application)
        }
    }

    fun onSourceSelected(sourceType: VideoSourceType) {
        videoPipelineManager.setActiveSource(sourceType)
    }

    fun onGoLiveToggled() {
        if (sessionState.value == LiveSessionState.LIVE || sessionState.value == LiveSessionState.STARTING) {
            liveSessionManager.stopLiveSession()
        } else {
            encoderManager.startEncoders()
            liveSessionManager.startLiveSession()
        }
    }

    fun onRecordToggled() {
        if (recordingState.value == RecordingState.RECORDING) {
            recordingManager.stopRecording(getApplication())
            if (sessionState.value != LiveSessionState.LIVE) {
                encoderManager.stopEncoders()
            }
        } else {
            encoderManager.startEncoders()
            recordingManager.startRecording(getApplication())
        }
    }

    fun getRecordedFiles(): List<File> {
        return recordingManager.getRecordedFiles()
    }

    fun onAudioMuteToggled() {
        val currentMute = audioMeterState.value.isMuted
        audioManager.setMute(!currentMute)
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.stopMicAudioRecording()
        encoderManager.stopEncoders()
    }
}
