package com.liveproduction.core.media

import android.util.Log
import android.view.Surface
import com.liveproduction.core.media.model.VideoFrame
import com.liveproduction.core.media.model.VideoSourceType
import com.liveproduction.core.media.router.FrameRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VideoPipelineManager private constructor() {

    val frameRouter = FrameRouter()

    private val _activeSource = MutableStateFlow(VideoSourceType.SOURCE_FRONT_CAMERA)
    val activeSource: StateFlow<VideoSourceType> = _activeSource.asStateFlow()

    private val _currentFps = MutableStateFlow(0f)
    val currentFps: StateFlow<Float> = _currentFps.asStateFlow()

    private var activePreviewSurface: Surface? = null
    private var onSourceSurfaceListener: ((VideoSourceType, Surface) -> Unit)? = null
    private var frameCount = 0
    private var lastFpsTimestamp = System.currentTimeMillis()

    init {
        frameRouter.setFrameListener { frame ->
            onFrameArrived(frame)
        }
    }

    fun setOnSourceSurfaceListener(listener: (VideoSourceType, Surface) -> Unit) {
        this.onSourceSurfaceListener = listener
        activePreviewSurface?.let { surface ->
            listener.invoke(_activeSource.value, surface)
        }
    }

    fun onPreviewSurfaceAvailable(surface: Surface) {
        Log.i(TAG, "Preview Surface attached to VideoPipelineManager")
        activePreviewSurface = surface
        onSourceSurfaceListener?.invoke(_activeSource.value, surface)
    }

    fun onPreviewSurfaceDestroyed() {
        Log.i(TAG, "Preview Surface detached from VideoPipelineManager")
        activePreviewSurface = null
    }

    fun setActiveSource(sourceType: VideoSourceType) {
        Log.i(TAG, "Switching active video source from ${_activeSource.value} to $sourceType")
        _activeSource.value = sourceType
        frameRouter.routeSource(sourceType)

        activePreviewSurface?.let { surface ->
            onSourceSurfaceListener?.invoke(sourceType, surface)
        }
    }

    fun onFrameArrived(frame: VideoFrame) {
        frameCount++
        val now = System.currentTimeMillis()
        if (now - lastFpsTimestamp >= 1000) {
            _currentFps.value = frameCount * 1000f / (now - lastFpsTimestamp)
            frameCount = 0
            lastFpsTimestamp = now
        }
    }

    companion object {
        private const val TAG = "VideoPipelineManager"

        @Volatile
        private var INSTANCE: VideoPipelineManager? = null

        fun getInstance(): VideoPipelineManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VideoPipelineManager().also { INSTANCE = it }
            }
        }
    }
}
