package com.liveproduction.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.liveproduction.core.media.overlay.OverlayManager
import com.liveproduction.core.streaming.SocialPlatformManager
import com.liveproduction.core.streaming.model.SocialPlatformType
import com.liveproduction.core.streaming.model.StreamProfile
import com.liveproduction.core.streaming.model.StreamingDestination
import kotlinx.coroutines.flow.StateFlow

class StreamSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val socialPlatformManager = SocialPlatformManager.getInstance()
    private val overlayManager = OverlayManager.getInstance()

    val activeDestination: StateFlow<StreamingDestination> = socialPlatformManager.activeDestination
    val overlayConfig = overlayManager.overlayConfig

    init {
        socialPlatformManager.initialize(application)
    }

    fun saveDestinationConfig(
        platformType: SocialPlatformType,
        customUrl: String,
        streamKey: String,
        profile: StreamProfile
    ) {
        socialPlatformManager.configureActiveDestination(platformType, customUrl, streamKey, profile)
    }

    fun getSavedStreamKey(platformType: SocialPlatformType): String {
        return socialPlatformManager.getSavedStreamKey(platformType)
    }

    fun updateLowerThird(enabled: Boolean, title: String, subtitle: String) {
        overlayManager.updateLowerThird(enabled, title, subtitle)
    }

    fun toggleClock(enabled: Boolean) {
        overlayManager.toggleClock(enabled)
    }
}
