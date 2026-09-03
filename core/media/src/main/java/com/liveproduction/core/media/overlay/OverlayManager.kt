package com.liveproduction.core.media.overlay

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OverlayConfig(
    val isWatermarkEnabled: Boolean = false,
    val watermarkBitmap: Bitmap? = null,
    val isLowerThirdEnabled: Boolean = false,
    val lowerThirdTitle: String = "",
    val lowerThirdSubtitle: String = "",
    val isClockEnabled: Boolean = true
)

class OverlayManager private constructor() {

    private val _overlayConfig = MutableStateFlow(OverlayConfig())
    val overlayConfig: StateFlow<OverlayConfig> = _overlayConfig.asStateFlow()

    fun updateWatermark(enabled: Boolean, bitmap: Bitmap? = null) {
        _overlayConfig.value = _overlayConfig.value.copy(
            isWatermarkEnabled = enabled,
            watermarkBitmap = bitmap
        )
        Log.i(TAG, "Updated Watermark overlay: Enabled=$enabled")
    }

    fun updateLowerThird(enabled: Boolean, title: String = "", subtitle: String = "") {
        _overlayConfig.value = _overlayConfig.value.copy(
            isLowerThirdEnabled = enabled,
            lowerThirdTitle = title,
            lowerThirdSubtitle = subtitle
        )
        Log.i(TAG, "Updated LowerThird overlay: Enabled=$enabled, Title=$title")
    }

    fun toggleClock(enabled: Boolean) {
        _overlayConfig.value = _overlayConfig.value.copy(isClockEnabled = enabled)
        Log.i(TAG, "Toggled Realtime Clock overlay: Enabled=$enabled")
    }

    companion object {
        private const val TAG = "OverlayManager"

        @Volatile
        private var INSTANCE: OverlayManager? = null

        fun getInstance(): OverlayManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OverlayManager().also { INSTANCE = it }
            }
        }
    }
}
