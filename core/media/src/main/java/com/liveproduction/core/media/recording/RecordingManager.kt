package com.liveproduction.core.media.recording

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED,
    ERROR,
    LOW_STORAGE_STOPPED
}

class RecordingManager private constructor() {

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _recordedDurationMs = MutableStateFlow(0L)
    val recordedDurationMs: StateFlow<Long> = _recordedDurationMs.asStateFlow()

    private var mediaMuxer: MediaMuxer? = null
    private var videoTrackIndex: Int = -1
    private var audioTrackIndex: Int = -1
    private var isMuxerStarted = false
    private var pendingVideoFormat: MediaFormat? = null
    private var pendingAudioFormat: MediaFormat? = null
    private var currentRecordingFile: File? = null
    private var timerJob: Job? = null
    private var fallbackTimeoutJob: Job? = null

    private var baseVideoPtsUs = -1L
    private var baseAudioPtsUs = -1L

    fun startRecording(context: Context): Boolean {
        if (_recordingState.value == RecordingState.RECORDING) {
            Log.w(TAG, "Recording already active")
            return false
        }

        // Save to public DCIM / LiveStreamRecordings directory so it appears in Phone Gallery & Files app
        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "LiveStreamRecordings"
        )

        // Check storage safety threshold (> 500MB required)
        val availableMb = checkAvailableStorageMb(outputDir)
        if (availableMb < 500) {
            Log.e(TAG, "Storage too low for recording ($availableMb MB available). Minimum 500MB required.")
            _recordingState.value = RecordingState.LOW_STORAGE_STOPPED
            return false
        }

        try {
            if (!outputDir.exists()) outputDir.mkdirs()
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(outputDir, "LIVE_REC_$timeStamp.mp4")
            currentRecordingFile = file

            val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            mediaMuxer = muxer
            isMuxerStarted = false
            videoTrackIndex = -1
            audioTrackIndex = -1
            baseVideoPtsUs = -1L
            baseAudioPtsUs = -1L

            _recordingState.value = RecordingState.RECORDING
            startTimer()

            // Check if formats were already cached by encoders
            checkStartMuxerLocked()

            // Set fallback timeout: If AudioFormat doesn't arrive within 1500ms, start Muxer with Video track only!
            fallbackTimeoutJob?.cancel()
            fallbackTimeoutJob = CoroutineScope(Dispatchers.Default).launch {
                delay(1500)
                synchronized(this@RecordingManager) {
                    if (!isMuxerStarted && pendingVideoFormat != null) {
                        Log.w(TAG, "AudioFormat timeout: Starting MediaMuxer with Video track only")
                        forceStartMuxerLocked()
                    }
                }
            }

            Log.i(TAG, "MediaMuxer recording initialized to public file: ${file.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaMuxer", e)
            _recordingState.value = RecordingState.ERROR
            return false
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        val startTime = System.currentTimeMillis()
        timerJob = CoroutineScope(Dispatchers.Default).launch {
            while (_recordingState.value == RecordingState.RECORDING) {
                _recordedDurationMs.value = System.currentTimeMillis() - startTime
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _recordedDurationMs.value = 0L
    }

    @Synchronized
    fun addVideoFormat(format: MediaFormat) {
        if (!isMuxerStarted && pendingVideoFormat == null) {
            pendingVideoFormat = format
            Log.i(TAG, "Cached Hardware Video Format with SPS/PPS CSD for MediaMuxer: $format")
            checkStartMuxerLocked()
        }
    }

    @Synchronized
    fun addAudioFormat(format: MediaFormat) {
        if (!isMuxerStarted && pendingAudioFormat == null) {
            pendingAudioFormat = format
            Log.i(TAG, "Cached Hardware Audio Format with AAC CSD-0 for MediaMuxer: $format")
            checkStartMuxerLocked()
        }
    }

    private fun checkStartMuxerLocked() {
        val muxer = mediaMuxer ?: return
        if (isMuxerStarted) return

        val vFmt = pendingVideoFormat
        val aFmt = pendingAudioFormat

        // GitHub Android MediaCodec Standard:
        // Wait until BOTH Video (SPS/PPS CSD) AND Audio (AAC CSD-0) formats are available before calling muxer.start()!
        if (vFmt != null && aFmt != null) {
            forceStartMuxerLocked()
        }
    }

    private fun forceStartMuxerLocked() {
        val muxer = mediaMuxer ?: return
        if (isMuxerStarted) return

        try {
            val vFmt = pendingVideoFormat
            val aFmt = pendingAudioFormat

            if (vFmt != null) {
                videoTrackIndex = muxer.addTrack(vFmt)
                Log.i(TAG, "Added Hardware H.264 Video Track to MediaMuxer: index $videoTrackIndex")
            }

            if (aFmt != null) {
                audioTrackIndex = muxer.addTrack(aFmt)
                Log.i(TAG, "Added Hardware AAC Audio Track with CSD-0 to MediaMuxer: index $audioTrackIndex")
            }

            muxer.start()
            isMuxerStarted = true
            fallbackTimeoutJob?.cancel()
            fallbackTimeoutJob = null
            Log.i(TAG, "MediaMuxer started successfully with Video ($videoTrackIndex) & Audio ($audioTrackIndex)!")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting dual-track MediaMuxer", e)
        }
    }

    @Synchronized
    fun writeVideoSample(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (!isMuxerStarted || videoTrackIndex == -1) return
        try {
            if (baseVideoPtsUs == -1L) {
                baseVideoPtsUs = bufferInfo.presentationTimeUs
            }
            val ptsUs = bufferInfo.presentationTimeUs - baseVideoPtsUs
            bufferInfo.presentationTimeUs = if (ptsUs >= 0) ptsUs else 0L

            mediaMuxer?.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Error writing video sample to MediaMuxer", e)
        }
    }

    @Synchronized
    fun writeAudioSample(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (!isMuxerStarted || audioTrackIndex == -1) return
        try {
            if (baseAudioPtsUs == -1L) {
                baseAudioPtsUs = bufferInfo.presentationTimeUs
            }
            val ptsUs = bufferInfo.presentationTimeUs - baseAudioPtsUs
            bufferInfo.presentationTimeUs = if (ptsUs >= 0) ptsUs else 0L

            mediaMuxer?.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Error writing audio sample to MediaMuxer", e)
        }
    }

    fun stopRecording(context: Context): String? {
        if (_recordingState.value != RecordingState.RECORDING) return null

        Log.i(TAG, "Stopping MediaMuxer local recording...")
        stopTimer()
        fallbackTimeoutJob?.cancel()
        fallbackTimeoutJob = null

        try {
            if (isMuxerStarted) {
                mediaMuxer?.stop()
                mediaMuxer?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaMuxer", e)
        } finally {
            mediaMuxer = null
            isMuxerStarted = false
            pendingVideoFormat = null
            pendingAudioFormat = null
            baseVideoPtsUs = -1L
            baseAudioPtsUs = -1L
            _recordingState.value = RecordingState.IDLE
        }

        val filePath = currentRecordingFile?.absolutePath
        if (filePath != null) {
            // Trigger MediaScannerConnection so the MP4 immediately appears in Phone Gallery & Photos App!
            MediaScannerConnection.scanFile(
                context,
                arrayOf(filePath),
                arrayOf("video/mp4")
            ) { path, uri ->
                Log.i(TAG, "Successfully scanned recorded MP4 into System MediaStore Gallery: $path -> $uri")
            }
        }
        return filePath
    }

    fun getRecordedFiles(): List<File> {
        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "LiveStreamRecordings"
        )
        if (!outputDir.exists()) return emptyList()
        return outputDir.listFiles { file -> file.extension == "mp4" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    private fun checkAvailableStorageMb(dir: File): Long {
        return try {
            val stat = StatFs(dir.absolutePath)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes / (1024 * 1024)
        } catch (e: Exception) {
            1000L
        }
    }

    companion object {
        private const val TAG = "RecordingManager"

        @Volatile
        private var INSTANCE: RecordingManager? = null

        fun getInstance(): RecordingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RecordingManager().also { INSTANCE = it }
            }
        }
    }
}
