package com.liveproduction.core.media.router

import android.util.Log
import com.liveproduction.core.media.model.VideoFrame
import com.liveproduction.core.media.model.VideoSourceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FrameRouter {

    private val _currentRoutedSource = MutableStateFlow(VideoSourceType.SOURCE_EXTERNAL_HDMI)
    val currentRoutedSource: StateFlow<VideoSourceType> = _currentRoutedSource.asStateFlow()

    private var activeFrameListener: ((VideoFrame) -> Unit)? = null

    fun routeSource(sourceType: VideoSourceType) {
        Log.i(TAG, "FrameRouter switching active source: ${_currentRoutedSource.value} -> $sourceType")
        _currentRoutedSource.value = sourceType
    }

    fun setFrameListener(listener: (VideoFrame) -> Unit) {
        this.activeFrameListener = listener
    }

    fun dispatchFrame(frame: VideoFrame) {
        // Only dispatch frame if source matches current routed source
        if (frame.sourceType == _currentRoutedSource.value) {
            activeFrameListener?.invoke(frame)
        }
    }

    companion object {
        private const val TAG = "FrameRouter"
    }
}
